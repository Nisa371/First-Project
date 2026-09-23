package com.marketplace.admin;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.job.*;
import com.marketplace.booking.*;
import com.marketplace.payment.*;
import com.marketplace.interview.AssessmentSession;
import com.marketplace.assessment.AssessmentAttempt;
import com.marketplace.skill.Skill;
import com.marketplace.companytype.CompanyType;
import com.marketplace.verification.VerificationRequirement;
import com.marketplace.user.*;
import com.marketplace.common.api.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;

@Service @RequiredArgsConstructor @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
public class AdminRecordsService {
    private final EntityManager em;
    private final CurrentAccount current;
    private final JobApplicationService applicationViews;
    // Only these explicitly selected scalar fields cross the admin API boundary.
    public record Row(Long id, String title, String subtitle, String status, Map<String,Object> fields) {}
    public record Page(List<Row> content, long totalElements, int page, int totalPages) {}
    private record Catalog(Class<?> type, String[] search, String status) {}
    private Catalog catalog(String section) {
        return switch(section) {
            case "users" -> new Catalog(User.class,new String[]{"email","role"},"accountStatus");
            case "candidates" -> new Catalog(CandidateProfile.class,new String[]{"fullName","user.email","candidateType"},"user.accountStatus");
            case "employers" -> new Catalog(EmployerProfile.class,new String[]{"companyName","user.email"},"user.accountStatus");
            case "jobs" -> new Catalog(Job.class,new String[]{"title","employer.companyName"},"status");
            case "applications" -> new Catalog(JobApplication.class,new String[]{"candidate.fullName","job.title"},"status");
            case "bookings" -> new Catalog(Booking.class,new String[]{"candidate.fullName","purpose"},"status");
            case "payments" -> new Catalog(PaymentTransaction.class,new String[]{"reference","payer.email","purpose"},"status");
            case "assessments" -> new Catalog(AssessmentSession.class,new String[]{"jobApplication.candidate.fullName","jobApplication.job.title"},"status");
            case "attempts" -> new Catalog(AssessmentAttempt.class,new String[]{"candidate.fullName","assessment.title"},"status");
            case "company-types" -> new Catalog(CompanyType.class,new String[]{"name"},"active");
            case "verification-requirements" -> new Catalog(VerificationRequirement.class,new String[]{"name","targetType"},"active");
            case "skills" -> new Catalog(Skill.class,new String[]{"name","category"},"active");
            default -> throw CandidateService.missing();
        };
    }
    public Page list(String section,String search,String status,int page,int size) {
        current.requireActive();
        if(page<0 || size<1 || size>50 || (long)page*size>Integer.MAX_VALUE || search.length()>200 || status.length()>40)
            throw new ApiException(400,"INVALID_FILTER","Use a page size of 1–50 and a short search.");
        var c=catalog(section); var cb=em.getCriteriaBuilder();
        var query=cb.createQuery(c.type()); var root=query.from(c.type());
        query.where(filter(cb,root,c,search,status)); query.orderBy(cb.desc(root.get("id")));
        var rows=em.createQuery(query).setFirstResult(page*size).setMaxResults(size).getResultList().stream().map(this::row).toList();
        var count=cb.createQuery(Long.class); var cr=count.from(c.type()); count.select(cb.count(cr)).where(filter(cb,cr,c,search,status));
        long total=em.createQuery(count).getSingleResult(); return new Page(rows,total,page,(int)((total+size-1)/size));
    }
    private Predicate filter(CriteriaBuilder cb,Root<?> root,Catalog c,String search,String status) {
        var predicates=new ArrayList<Predicate>();
        if(!search.isBlank()) {
            String pattern="%"+search.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
            predicates.add(cb.or(Arrays.stream(c.search()).map(p->cb.like(cb.lower(path(root,p).as(String.class)),pattern,'!')).toArray(Predicate[]::new)));
        }
        if(!status.isBlank()) {
            if(c.status().equals("active")) {
                if(!status.equals("true") && !status.equals("false")) throw new ApiException(400,"INVALID_FILTER","Select active or inactive.");
                predicates.add(cb.equal(path(root,c.status()),Boolean.valueOf(status)));
            } else predicates.add(cb.equal(cb.lower(path(root,c.status()).as(String.class)),status.toLowerCase(Locale.ROOT)));
        }
        return cb.and(predicates.toArray(Predicate[]::new));
    }
    private Path<?> path(Path<?> root,String name) { for(var part:name.split("\\.")) root=root.get(part); return root; }
    public Row get(String section,Long id) { current.requireActive(); var entity=em.find(catalog(section).type(),id); if(entity==null) throw CandidateService.missing(); return row(entity); }
    private Map<String,Object> fields(Object... pairs) { var map=new LinkedHashMap<String,Object>(); for(int i=0;i<pairs.length;i+=2) map.put((String)pairs[i],pairs[i+1]); return map; }
    private Row row(Object entity) {
        if(entity instanceof User u) return new Row(u.getId(),u.getEmail(),u.getRole().name(),u.getAccountStatus().name(),fields("role",u.getRole(),"createdAt",u.getCreatedAt(),"updatedAt",u.getUpdatedAt()));
        if(entity instanceof CandidateProfile c) return new Row(c.getId(),c.getFullName(),c.getUser().getEmail(),c.getUser().getAccountStatus().name(),fields("userId",c.getUser().getId(),"candidateType",c.getCandidateType(),"fullName",c.getFullName(),"phone",c.getPhone(),"location",c.getLocation(),"bio",c.getBio(),"educationSummary",c.getEducationSummary(),"experienceSummary",c.getExperienceSummary(),"totalExperienceMonths",c.getTotalExperienceMonths(),"availability",c.getAvailability(),"primaryTradeCategory",c.getPrimaryTradeCategory(),"portfolioUrl",c.getPortfolioUrl(),"updatedAt",c.getUpdatedAt()));
        if(entity instanceof EmployerProfile e) return new Row(e.getId(),e.getCompanyName(),e.getUser().getEmail(),e.getUser().getAccountStatus().name(),fields("userId",e.getUser().getId(),"companyName",e.getCompanyName(),"companyTypeId",e.getCompanyType()==null?null:e.getCompanyType().getId(),"companyTypeName",e.getCompanyType()==null?null:e.getCompanyType().getName(),"companyTypeOther",e.getCompanyType()!=null&&e.getCompanyType().isOther(),"companyTypeActive",e.getCompanyType()!=null&&e.getCompanyType().isActive(),"customCompanyType",e.getCustomCompanyType(),"contactPhone",e.getContactPhone(),"address",e.getAddress(),"description",e.getDescription(),"updatedAt",e.getUpdatedAt()));
        if(entity instanceof Job j) return new Row(j.getId(),j.getTitle(),j.getEmployer().getCompanyName(),j.getStatus().name(),fields("employerId",j.getEmployer().getId(),"title",j.getTitle(),"description",j.getDescription(),"location",j.getLocation(),"candidateType",j.getCandidateType(),"requiredSkillId",j.getRequiredSkill()==null?null:j.getRequiredSkill().getId(),"publicExpectations",j.getPublicExpectations(),"privateExpectations",j.getPrivateExpectations(),"expectedExperienceMonths",j.getExpectedExperienceMonths(),"createdAt",j.getCreatedAt(),"updatedAt",j.getUpdatedAt()));
        if(entity instanceof JobApplication a) { var result=applicationViews.adminView(a.getId()); return new Row(a.getId(),a.getCandidate().getFullName(),a.getJob().getTitle(),a.getStatus().name(),fields("candidateId",a.getCandidate().getId(),"jobId",a.getJob().getId(),"cvScore",a.getCvScore(),"portfolioScore",a.getPortfolioScore(),"assessmentScore",a.getAssessmentScore(),"experienceScore",result.experienceScore(),"finalScore",result.finalScore(),"evaluationStatus",result.evaluationStatus(),"assessmentStatus",result.assessmentStatus(),"createdAt",a.getCreatedAt(),"updatedAt",a.getUpdatedAt())); }
        if(entity instanceof PaymentTransaction p) return new Row(p.getId(),p.getReference(),p.getPayer().getEmail(),p.getStatus().name(),fields("purpose",p.getPurpose(),"amount",p.getAmount(),"currency",p.getCurrency(),"jobId",p.getJob()==null?null:p.getJob().getId(),"bookingId",p.getBooking()==null?null:p.getBooking().getId(),"createdAt",p.getCreatedAt(),"completedAt",p.getCompletedAt()));
        if(entity instanceof Booking b) return new Row(b.getId(),b.getCandidate().getFullName(),b.getPurpose().name(),b.getStatus().name(),fields("candidateId",b.getCandidate().getId(),"slotId",b.getSlot().getId(),"evaluator",b.getSlot().getEvaluatorUser()==null?null:b.getSlot().getEvaluatorUser().getEmail(),"scheduledAt",b.getSlot().getStartTime(),"endTime",b.getSlot().getEndTime(),"notes",b.getNotes(),"createdAt",b.getCreatedAt()));
        if(entity instanceof AssessmentSession a) return new Row(a.getId(),a.getJobApplication().getCandidate().getFullName(),a.getJobApplication().getJob().getTitle(),a.getStatus().name(),fields("applicationId",a.getJobApplication().getId(),"jobId",a.getJobApplication().getJob().getId(),"candidateId",a.getJobApplication().getCandidate().getId(),"startedAt",a.getStartedAt(),"completedAt",a.getCompletedAt(),"failureCode",a.getFailureCode(),"assessmentScore",a.getJobApplication().getAssessmentScore()));
        if(entity instanceof AssessmentAttempt a) return new Row(a.getId(),a.getCandidate().getFullName(),a.getAssessment().getTitle(),a.getStatus().name(),fields("candidateId",a.getCandidate().getId(),"assessmentId",a.getAssessment().getId(),"autoScore",a.getAutoScore(),"startedAt",a.getStartedAt(),"submittedAt",a.getSubmittedAt(),"evaluatedAt",a.getEvaluatedAt()));
        if(entity instanceof CompanyType t) return new Row(t.getId(),t.getName(),"Company type",t.isActive()?"ACTIVE":"INACTIVE",fields("name",t.getName(),"active",t.isActive(),"other",t.isOther()));
        if(entity instanceof VerificationRequirement r) return new Row(r.getId(),r.getName(),r.getTargetType().name(),r.isActive()?"ACTIVE":"INACTIVE",fields("name",r.getName(),"description",r.getDescription(),"targetType",r.getTargetType(),"companyTypeId",r.getCompanyType()==null?null:r.getCompanyType().getId(),"companyTypeName",r.getCompanyType()==null?null:r.getCompanyType().getName(),"required",r.isRequired(),"active",r.isActive(),"baseline",r.getCode()!=null));
        if(entity instanceof Skill s) return new Row(s.getId(),s.getName(),s.getCategory(),s.isActive()?"ACTIVE":"INACTIVE",fields("name",s.getName(),"category",s.getCategory(),"active",s.isActive()));
        throw CandidateService.missing();
    }
}
