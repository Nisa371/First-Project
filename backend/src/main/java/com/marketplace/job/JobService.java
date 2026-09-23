package com.marketplace.job;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.employer.EmployerService;
import com.marketplace.candidate.*;
import com.marketplace.skill.SkillRepository;
import com.marketplace.common.api.ApiException;
import com.marketplace.user.AccountStatus;
import com.marketplace.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import static com.marketplace.job.JobDtos.*;

@Service
@RequiredArgsConstructor
@Transactional
@PreAuthorize("hasRole('EMPLOYER')")
public class JobService {
    private final JobRepository jobs;
    private final EmployerService employers;
    private final CurrentAccount current;
    private final com.marketplace.payment.PaymentRepository payments;
    private final SkillRepository skills;
    private final ShortlistEntryRepository shortlists;
    private final JobApplicationRepository applications;
    private final JobApplicationService applicationService;
    private final CandidateService candidateViews;
    public List<JobView> list() { return jobs.findByEmployerIdOrderByCreatedAtDesc(employers.own().getId()).stream().map(this::view).toList(); }
    public JobView get(Long id) { return view(owned(id)); }
    public JobView create(JobRequest r) {
        var j=new Job(); j.setEmployer(employers.own()); j.setStatus(JobStatus.DRAFT); apply(j,r);
        return view(jobs.saveAndFlush(j));
    }
    public JobView update(Long id, JobRequest r) {
        var j=owned(id); requireOpen(j); apply(j,r); return view(j);
    }
    public JobView close(Long id) { return closeJob(owned(id)); }
    private JobView closeJob(Job j) { var id=j.getId(); j.setStatus(JobStatus.CLOSED); payments.findByJobId(id).filter(p -> p.getStatus()==com.marketplace.payment.PaymentStatus.PENDING).ifPresent(p -> {
            p.setStatus(com.marketplace.payment.PaymentStatus.CANCELLED); p.setCompletedAt(java.time.Instant.now());
        }); return view(j); }
    @PreAuthorize("hasRole('ADMIN')")
    public JobView adminGet(Long id) { current.requireActive(); return view(jobs.findById(id).orElseThrow(CandidateService::missing)); }
    @PreAuthorize("hasRole('ADMIN')")
    public JobView adminCreate(Long employerId, JobRequest r) {
        current.requireActive();
        var e=employerProfiles.findById(employerId).filter(x -> x.getUser().getRole()==Role.EMPLOYER && x.getUser().getAccountStatus()==AccountStatus.ACTIVE).orElseThrow(CandidateService::missing);
        var j=new Job(); j.setEmployer(e); j.setStatus(JobStatus.DRAFT); apply(j,r); return view(jobs.saveAndFlush(j));
    }
    private final com.marketplace.employer.EmployerProfileRepository employerProfiles;
    @PreAuthorize("hasRole('ADMIN')")
    public JobView adminUpdate(Long id, JobRequest r) {
        current.requireActive(); var j=jobs.findByIdForUpdate(id).orElseThrow(CandidateService::missing); requireOpen(j);
        if(applications.countByJobId(id)>0 && (j.getCandidateType()!=r.candidateType() || !java.util.Objects.equals(j.getRequiredSkill()==null?null:j.getRequiredSkill().getId(),r.requiredSkillId())))
            throw new ApiException(409,"JOB_HAS_APPLICATIONS","Track and required skill cannot change after applications arrive.");
        apply(j,r); return view(j);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public JobView adminClose(Long id) { current.requireActive(); return closeJob(jobs.findByIdForUpdate(id).orElseThrow(CandidateService::missing)); }
    @PreAuthorize("hasRole('ADMIN')")
    public JobView adminActivate(Long id) {
        current.requireActive(); var j=jobs.findByIdForUpdate(id).orElseThrow(CandidateService::missing);
        if(j.getEmployer().getUser().getAccountStatus()!=AccountStatus.ACTIVE || payments.findByJobId(id).filter(p -> p.getStatus()==com.marketplace.payment.PaymentStatus.SUCCESS).isEmpty())
            throw new ApiException(409,"PAYMENT_REQUIRED","Activation requires an active employer and a successful demo posting payment.");
        j.setStatus(JobStatus.ACTIVE); return view(j);
    }
    private Job owned(Long id) {
        return jobs.findOwnedForUpdate(id,current.requireActive().getId()).orElseThrow(CandidateService::missing);
    }
    private void requireOpen(Job j) { if(j.getStatus()==JobStatus.CLOSED) throw new ApiException(409,"JOB_CLOSED","This job is closed. Create a new job to continue hiring."); }
    private void apply(Job j, JobRequest r) {
        j.setPublicExpectations(r.publicExpectations()); j.setPrivateExpectations(r.privateExpectations());
        if(r.expectedExperienceMonths()!=null) j.setExpectedExperienceMonths(r.expectedExperienceMonths());
        j.setTitle(r.title().trim()); j.setDescription(r.description().trim()); j.setLocation(r.location().trim()); j.setCandidateType(r.candidateType());
        j.setRequiredSkill(r.requiredSkillId()==null ? null : skills.findById(r.requiredSkillId()).filter(s->s.isActive()).orElseThrow(CandidateService::missing));
    }
    private JobView view(Job j) {
        return new JobView(j.getId(),j.getTitle(),j.getDescription(),j.getLocation(),j.getCandidateType(),
            j.getRequiredSkill()==null?null:j.getRequiredSkill().getId(),j.getRequiredSkill()==null?null:j.getRequiredSkill().getName(),
            j.getStatus(),applications.countByJobIdAndStatus(j.getId(),ApplicationStatus.SHORTLISTED),applications.countByJobId(j.getId()),j.getCreatedAt(),j.getPublicExpectations(),j.getPrivateExpectations(),j.getExpectedExperienceMonths());
    }
    public List<CandidateDtos.CandidateCard> shortlist(Long id) {
        owned(id); return shortlists.findByJobIdAndJobEmployerUserId(id,current.requireActive().getId()).stream()
            .filter(s->applications.existsByJobIdAndCandidateIdAndStatus(id,s.getCandidate().getId(),ApplicationStatus.SHORTLISTED))
            .filter(s->s.getCandidate().getUser().getAccountStatus()==AccountStatus.ACTIVE && s.getCandidate().getUser().getRole()==Role.CANDIDATE)
            .map(s->candidateViews.card(s.getCandidate())).toList();
    }
    public void add(Long id, Long candidateId) { applicationService.shortlist(id,candidateId); }
    public void remove(Long id, Long candidateId) { applicationService.removeShortlist(id,candidateId); }
}
