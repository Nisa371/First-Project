package com.marketplace.assessment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {
    Optional<Evaluation> findByAttemptId(Long attemptId);
    List<Evaluation> findByAttemptCandidateIdAndReleasedTrue(Long candidateId);
}

