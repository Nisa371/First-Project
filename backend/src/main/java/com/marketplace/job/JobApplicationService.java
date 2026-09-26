package com.marketplace.job;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.user.*;
import com.marketplace.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import static com.marketplace.job.JobDtos.*;

@Service @RequiredArgsConstructor @Transactional
public class JobApplicationService {
    private final com.marketplace.interview.AssessmentSessionRepository assessmentSessions;
    private final JobRepository jobs;
    private final com.marketplace.companytype.CompanyTypeRepository companyTypes;
    private final JobApplicationRepository applications;
    private final CandidateProfileRepository candidates;
    private final CandidateSkillRepository skills;
    private final CandidateService candidateViews;
    private final ShortlistEntryRepository shortlists;
    private final CurrentAccount current;
    private final com.marketplace.notification.NotificationService notifications;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final com.marketplace.verification.VerificationChecklist verifications;
    @PreAuthorize("hasRole('CANDIDATE')")
    @Transactional(readOnly=true)
    public JobPage openings(JobSearch input) {
        var c=candidate();
        if((long)input.page()*input.size()>Integer.MAX_VALUE) throw invalid("The requested page is too large.");
        if(input.minExperience()!=null && input.maxExperience()!=null && input.minExperience()>input.maxExperience())
            throw invalid("Minimum experience cannot exceed maximum experience.");
        if(input.companyTypeId()!=null && !companyTypes.existsById(input.companyTypeId()))
            throw invalid("The selected company type does not exist.");
        var sort=switch(input.sort()) {
            case "newest" -> org.springframework.data.domain.Sort.by("createdAt").descending();
            case "oldest" -> org.springframework.data.domain.Sort.by("createdAt").ascending();
            case "experienceAsc" -> org.springframework.data.domain.Sort.by("expectedExperienceMonths").ascending();
            case "experienceDesc" -> org.springframework.data.domain.Sort.by("expectedExperienceMonths").descending();
            default -> throw invalid("Choose newest, oldest, experienceAsc or experienceDesc sorting.");
        };
        org.springframework.data.jpa.domain.Specification<Job> filter=(root,query,cb)-> {
            var employer=root.join("employer");
            var user=employer.join("user");
            var predicates=new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.get("status"),JobStatus.ACTIVE));
            predicates.add(cb.equal(user.get("role"),Role.EMPLOYER));
            predicates.add(cb.equal(user.get("accountStatus"),AccountStatus.ACTIVE));
            if(input.search()!=null && !input.search().isBlank()) {
                var skill=root.join("requiredSkill",jakarta.persistence.criteria.JoinType.LEFT);
                String pattern=pattern(input.search());
                predicates.add(cb.or(cb.like(cb.lower(root.get("title")),pattern,'!'),
                    cb.like(cb.lower(root.get("description")),pattern,'!'),
                    cb.like(cb.lower(root.get("publicExpectations")),pattern,'!'),
                    cb.like(cb.lower(employer.get("companyName")),pattern,'!'),
                    cb.like(cb.lower(skill.get("name")),pattern,'!')));
            }
            if(input.location()!=null && !input.location().isBlank())
                predicates.add(cb.like(cb.lower(root.get("location")),pattern(input.location()),'!'));
            if(input.companyTypeId()!=null) predicates.add(cb.equal(employer.get("companyType").get("id"),input.companyTypeId()));
            if(input.minExperience()!=null) predicates.add(cb.ge(root.get("expectedExperienceMonths"),input.minExperience()));
            if(input.maxExperience()!=null) predicates.add(cb.le(root.get("expectedExperienceMonths"),input.maxExperience()));
            if(input.candidateTrack()!=null) predicates.add(cb.equal(root.get("candidateType"),input.candidateTrack()));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        var page=jobs.findAll(filter,org.springframework.data.domain.PageRequest.of(input.page(),input.size(),
            sort.and(org.springframework.data.domain.Sort.by("id").descending())));
        var states=new java.util.HashMap<Long,JobApplicationRepository.ApplicationState>();
        if(!page.isEmpty()) applications.states(c.getId(),page.getContent().stream().map(Job::getId).toList())
            .forEach(a->states.put(a.getJobId(),a));
        var content=page.getContent().stream().map(j->{
            var a=states.get(j.getId());return publicView(j,a==null?null:a.getId(),a==null?null:a.getStatus());
        }).toList();
        return new JobPage(content,page.getTotalElements(),page.getNumber(),page.getSize(),page.getTotalPages());
    }
    private static String pattern(String value) {
        return "%"+value.strip().toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
    }
    private static ApiException invalid(String message) { return new ApiException(400,"VALIDATION_ERROR",message); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public PublicJob opening(Long id) {
        var c=candidate();var j=jobs.findById(id).orElseThrow(CandidateService::missing);
        var a=applications.findByJobIdAndCandidateId(id,c.getId()).orElse(null);
        if(!available(j) && a==null) throw CandidateService.missing();
        return publicView(j,a);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationView apply(Long jobId) {
        var c=candidate();
        if(!"VERIFIED".equals(verifications.forUser(c.getUser()).status()))
            throw new ApiException(403,"VERIFICATION_REQUIRED","Candidate verification is required before applying for jobs.");
        if(c.getAvailability()!=Availability.AVAILABLE)
            throw new ApiException(403,"CANDIDATE_UNAVAILABLE","You are currently marked unavailable. Change your availability before applying for jobs.");
        var j=jobs.findByIdForUpdate(jobId).orElseThrow(CandidateService::missing);
        if(!available(j)) throw conflict("JOB_CLOSED","This job is not accepting applications.");
        if(applications.existsByJobIdAndCandidateId(jobId,c.getId())) throw conflict("ALREADY_APPLIED","You already applied to this job. View My Applications.");
        var a=new JobApplication();a.setJob(j);a.setCandidate(c);
        applications.saveAndFlush(a);
        notifications.afterCommit(j.getEmployer().getUser().getId(),"New application received",
            "A new application #"+a.getId()+" was received for "+j.getTitle()+".");
        // A successful application immediately enables the existing application assessment.
        notifications.afterCommit(c.getUser().getId(),"Assessment available",
            "You can now start the assessment for your application #"+a.getId()+" to "+j.getTitle()+".");
        events.publishEvent(new JobApplicationSubmitted(a.getId()));
        return ownView(a);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<ApplicationView> mine() { return applications.findByCandidateUserIdOrderByCreatedAtDescIdDesc(current.requireActive().getId()).stream().map(this::ownView).toList(); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public ApplicationView withdraw(Long id) {
        Long jobId=applications.ownedJobId(id,current.requireActive().getId()).orElseThrow(CandidateService::missing);
        jobs.findByIdForUpdate(jobId).orElseThrow(CandidateService::missing);
        var a=applications.findById(id).orElseThrow(CandidateService::missing);
        a.setStatus(ApplicationStatus.WITHDRAWN);clearShortlist(a);applications.flush();return ownView(a);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public List<Applicant> applicants(Long jobId) {
        ownedJob(jobId);return applications.findByJobIdOrderByCreatedAtDescIdDesc(jobId).stream().map(this::applicantView)
            .sorted(java.util.Comparator.comparing(Applicant::finalScore).reversed()
                .thenComparing(Applicant::appliedAt, java.util.Comparator.reverseOrder())
                .thenComparing(Applicant::id, java.util.Comparator.reverseOrder())).toList();
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public Applicant applicant(Long jobId,Long id) { ownedJob(jobId);return applicantView(applications.findByIdAndJobId(id,jobId).orElseThrow(CandidateService::missing)); }
    @PreAuthorize("hasRole('EMPLOYER')")
    public Applicant status(Long jobId,Long id,ApplicationStatus status) {
        var job=ownedJob(jobId);var a=applications.findByIdAndJobId(id,jobId).orElseThrow(CandidateService::missing);
        change(job,a,status);applications.flush();return applicantView(a);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public void shortlist(Long jobId,Long candidateId) {
        var job=ownedJob(jobId);var a=applications.findByJobIdAndCandidateId(jobId,candidateId)
            .orElseThrow(()->conflict("APPLICATION_REQUIRED","Only candidates who applied to this job can be shortlisted."));
        if(a.getStatus()==ApplicationStatus.SHORTLISTED) throw conflict("DUPLICATE_SHORTLIST","This applicant is already shortlisted.");
        change(job,a,ApplicationStatus.SHORTLISTED);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public void removeShortlist(Long jobId,Long candidateId) {
        var job=ownedJob(jobId);
        applications.findByJobIdAndCandidateId(jobId,candidateId).filter(a->a.getStatus()==ApplicationStatus.SHORTLISTED).ifPresent(a->change(job,a,ApplicationStatus.UNDER_REVIEW));
        shortlists.findByJobIdAndCandidateId(jobId,candidateId).ifPresent(shortlists::delete);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public Applicant adminStatus(Long id, ApplicationStatus status) {
        current.requireActive(); var a=applications.findById(id).orElseThrow(CandidateService::missing);
        var job=jobs.findByIdForUpdate(a.getJob().getId()).orElseThrow(CandidateService::missing);
        // Refresh after the job lock, which serializes employer and candidate transitions.
        entityManager.refresh(a, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE); change(job,a,status); applications.flush(); return applicantView(a);
    }
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    @PreAuthorize("hasRole('ADMIN')")
    public Applicant adminView(Long id) { current.requireActive(); return applicantView(applications.findById(id).orElseThrow(CandidateService::missing)); }
    private void change(Job j,JobApplication a,ApplicationStatus status) {
        if(status==ApplicationStatus.WITHDRAWN) throw new ApiException(400,"INVALID_STATUS","Only the candidate can withdraw an application.");
        if(a.getStatus()==ApplicationStatus.WITHDRAWN) throw conflict("APPLICATION_WITHDRAWN","This application has been withdrawn.");
        if(status==ApplicationStatus.SHORTLISTED) {
            var c=a.getCandidate();
            if(j.getStatus()!=JobStatus.ACTIVE) throw conflict("JOB_CLOSED","This job is closed.");
            if(c.getUser().getAccountStatus()!=AccountStatus.ACTIVE || c.getUser().getRole()!=Role.CANDIDATE
                || c.getAvailability()!=Availability.AVAILABLE || c.getCandidateType()!=j.getCandidateType()
                || (j.getRequiredSkill()!=null && !skills.existsByCandidateIdAndSkillId(c.getId(),j.getRequiredSkill().getId())))
                throw conflict("CANDIDATE_MISMATCH","Shortlisting requires an available applicant with the matching track and skill.");
            if(!shortlists.existsByJobIdAndCandidateId(j.getId(),c.getId())) { var s=new ShortlistEntry();s.setJob(j);s.setCandidate(c);shortlists.save(s); }
        } else clearShortlist(a);
        if(a.getStatus()!=status) {
            a.setStatus(status);
            notifications.afterCommit(a.getCandidate().getUser().getId(),"Application status updated",
                "Your application #"+a.getId()+" for "+j.getTitle()+" is now "+status.name().toLowerCase(java.util.Locale.ROOT).replace('_',' ')+".");
        }
    }
    private void clearShortlist(JobApplication a) { shortlists.findByJobIdAndCandidateId(a.getJob().getId(),a.getCandidate().getId()).ifPresent(shortlists::delete); }
    private Job ownedJob(Long id) { return jobs.findOwnedForUpdate(id,current.requireActive().getId()).orElseThrow(CandidateService::missing); }
    private CandidateProfile candidate() { return candidates.findByUserId(current.requireActive().getId()).orElseThrow(CandidateService::missing); }
    private boolean available(Job j) { return j.getStatus()==JobStatus.ACTIVE && j.getEmployer().getUser().getRole()==Role.EMPLOYER && j.getEmployer().getUser().getAccountStatus()==AccountStatus.ACTIVE; }
    private PublicJob publicView(Job j,JobApplication a) { return publicView(j,a==null?null:a.getId(),a==null?null:a.getStatus()); }
    private PublicJob publicView(Job j,Long applicationId,ApplicationStatus status) {
        var employer=j.getEmployer();var type=employer.getCompanyType();
        String label=type==null?null:type.isOther() && employer.getCustomCompanyType()!=null
            && !employer.getCustomCompanyType().isBlank()?employer.getCustomCompanyType():type.getName();
        return new PublicJob(j.getId(),j.getTitle(),j.getDescription(),employer.getCompanyName(),j.getLocation(),
            j.getCandidateType(),j.getRequiredSkill()==null?null:j.getRequiredSkill().getName(),j.getStatus(),
            j.getPublicExpectations(),j.getExpectedExperienceMonths(),j.getCreatedAt(),applicationId,status,
            applicationId!=null,type==null?null:type.getId(),label);
    }
    private ApplicationView ownView(JobApplication a) { return new ApplicationView(a.getId(),publicView(a.getJob(),a),a.getStatus(),a.getCreatedAt(),a.getUpdatedAt(),assessmentStatus(a)); }
    @PreAuthorize("hasRole('EMPLOYER')")
    public EvaluationWeights weights(Long jobId) { return weightView(ownedJob(jobId)); }
    @PreAuthorize("hasRole('EMPLOYER')")
    public EvaluationWeights updateWeights(Long jobId, EvaluationWeights input) {
        var job=ownedJob(jobId);
        MeritScoring.unitValue(input.cvWeight()); MeritScoring.unitValue(input.portfolioWeight());
        MeritScoring.unitValue(input.experienceWeight()); MeritScoring.unitValue(input.assessmentWeight());
        job.setCvWeight(input.cvWeight()); job.setPortfolioWeight(input.portfolioWeight());
        job.setExperienceWeight(input.experienceWeight()); job.setAssessmentWeight(input.assessmentWeight());
        jobs.flush(); return weightView(job);
    }
    private EvaluationWeights weightView(Job job) {
        return new EvaluationWeights(job.getCvWeight(),job.getPortfolioWeight(),job.getExperienceWeight(),job.getAssessmentWeight());
    }
    private Applicant applicantView(JobApplication a) {
        var experience=MeritScoring.experience(a.getCandidate().getTotalExperienceMonths(),a.getJob().getExpectedExperienceMonths());
        return new Applicant(a.getId(),a.getJob().getId(),a.getStatus(),a.getCreatedAt(),a.getUpdatedAt(),candidateViews.card(a.getCandidate()),
            a.getCvScore(),a.getPortfolioScore(),experience,a.getAssessmentScore(),
            MeritScoring.finalScore(experience,a.getAssessmentScore(),a.getCvScore(),a.getPortfolioScore(),weightView(a.getJob())),
            MeritScoring.evaluationStatus(a.getCvScore(),a.getPortfolioScore(),a.getAssessmentScore()),
            com.marketplace.ai.EvaluationAttempt.view(a.getCvAttempt(), a.getCvScore()),
            com.marketplace.ai.EvaluationAttempt.view(a.getPortfolioAttempt(), a.getPortfolioScore()),assessmentStatus(a));
    }
    private String assessmentStatus(JobApplication a) {
        return assessmentSessions.findByJobApplicationId(a.getId()).map(s -> s.getStatus().name()).orElse("NOT_STARTED");
    }
    private static ApiException conflict(String code,String message) { return new ApiException(409,code,message); }
}
