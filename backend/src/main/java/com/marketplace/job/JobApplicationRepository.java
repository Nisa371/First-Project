package com.marketplace.job;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface JobApplicationRepository extends JpaRepository<JobApplication,Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from JobApplication a where a.id=:id")
    Optional<JobApplication> findForEvaluation(Long id);
    @Query("select a.id from JobApplication a where a.candidate.id=:candidateId and a.status <> com.marketplace.job.ApplicationStatus.WITHDRAWN and ((:cv=true and a.cvScore is null) or (:portfolio=true and a.portfolioScore is null))")
    List<Long> outstandingEvaluations(Long candidateId, boolean cv, boolean portfolio);
    interface ApplicationState { Long getJobId(); Long getId(); ApplicationStatus getStatus(); }
    @Query("select a.job.id as jobId, a.id as id, a.status as status from JobApplication a where a.candidate.id=:candidateId and a.job.id in :jobIds")
    List<ApplicationState> states(Long candidateId, List<Long> jobIds);
    boolean existsByJobIdAndCandidateId(Long jobId,Long candidateId);
    boolean existsByCandidateIdAndJobEmployerUserId(Long candidateId,Long employerUserId);
    boolean existsByJobIdAndCandidateIdAndStatus(Long jobId,Long candidateId,ApplicationStatus status);
    Optional<JobApplication> findByJobIdAndCandidateId(Long jobId,Long candidateId);
    Optional<JobApplication> findByIdAndJobId(Long id,Long jobId);
    List<JobApplication> findByCandidateUserIdOrderByCreatedAtDescIdDesc(Long userId);
    List<JobApplication> findByJobIdOrderByCreatedAtDescIdDesc(Long jobId);
    interface ApplicantSummary {
        Long getId();
        java.time.Instant getAppliedAt();
        int getExperienceMonths();
        com.marketplace.candidate.Availability getAvailability();
        java.math.BigDecimal getCvScore();
        java.math.BigDecimal getPortfolioScore();
        java.math.BigDecimal getAssessmentScore();
        com.marketplace.interview.AssessmentSession.Status getAssessmentStatus();
    }
    @Query("""
        select c.availability as availability, a.id as id, a.createdAt as appliedAt, c.totalExperienceMonths as experienceMonths,
            a.cvScore as cvScore, a.portfolioScore as portfolioScore, a.assessmentScore as assessmentScore,
            s.status as assessmentStatus
        from JobApplication a join a.candidate c
        left join AssessmentSession s on s.jobApplication.id = a.id
        where a.job.id = :jobId
            and (:status is null or a.status = :status)
            and (:assessment is null or s.status = :assessment
                or (s.id is null and :notStarted = true))
            and (:minExperience is null or c.totalExperienceMonths >= :minExperience)
            and (:search is null or lower(c.fullName) like :search escape '!')
        """)
    List<ApplicantSummary> applicantSummaries(Long jobId, ApplicationStatus status,
        com.marketplace.interview.AssessmentSession.Status assessment, boolean notStarted, Integer minExperience, String search);
    @EntityGraph(attributePaths={"candidate", "job"})
    List<JobApplication> findByJobIdAndIdIn(Long jobId, List<Long> ids);
    long countByJobId(Long jobId);
    long countByJobIdAndStatus(Long jobId,ApplicationStatus status);
    @Query("select a.job.id from JobApplication a where a.id=:id and a.candidate.user.id=:userId")
    Optional<Long> ownedJobId(Long id,Long userId);
    @Query("select a.job.id from JobApplication a where a.id=:id and a.job.employer.user.id=:userId")
    Optional<Long> employerJobId(Long id,Long userId);
}
