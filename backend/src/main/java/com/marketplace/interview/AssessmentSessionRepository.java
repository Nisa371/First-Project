package com.marketplace.interview;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AssessmentSessionRepository extends JpaRepository<AssessmentSession, Long> {
    @org.springframework.data.jpa.repository.Query("select s.jobApplication.id from AssessmentSession s where s.id=:id")
    Optional<Long> applicationId(Long id);
    Optional<AssessmentSession> findByJobApplicationId(Long applicationId);
}
