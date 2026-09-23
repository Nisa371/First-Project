package com.marketplace.job;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface JobApplicationRepository extends JpaRepository<JobApplication,Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from JobApplication a where a.id=:id")
    Optional<JobApplication> findForEvaluation(Long id);
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
    long countByJobId(Long jobId);
    long countByJobIdAndStatus(Long jobId,ApplicationStatus status);
    @Query("select a.job.id from JobApplication a where a.id=:id and a.candidate.user.id=:userId")
    Optional<Long> ownedJobId(Long id,Long userId);
    @Query("select a.job.id from JobApplication a where a.id=:id and a.job.employer.user.id=:userId")
    Optional<Long> employerJobId(Long id,Long userId);
}
