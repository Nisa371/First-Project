package com.marketplace.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {
    @org.springframework.data.jpa.repository.Query("select s.jobApplication.id from AssessmentSession s where s.id=:id")
    Optional<Long> applicationId(Long id);
    @org.springframework.data.jpa.repository.Query("select s.jobApplication.job.id from AssessmentSession s where s.jobApplication.id=:applicationId")
    Optional<Long> jobIdForApplication(Long applicationId);
    @org.springframework.data.jpa.repository.Query("select s.jobApplication.id from AssessmentSession s where s.status=:status and s.startedAt<=:cutoff")
    java.util.List<Long> expiredApplications(AssessmentSession.Status status, java.time.Instant cutoff);
    Optional<AssessmentSession> findByJobApplicationId(Long applicationId);
}
