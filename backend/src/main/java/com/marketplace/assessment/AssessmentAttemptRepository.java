package com.marketplace.assessment;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from AssessmentAttempt a where a.id = :id")
    java.util.Optional<AssessmentAttempt> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    List<AssessmentAttempt> findByCandidateIdOrderByStartedAtDesc(Long candidateId);
    List<AssessmentAttempt> findByStatusOrderBySubmittedAtAsc(AttemptStatus status);
}

