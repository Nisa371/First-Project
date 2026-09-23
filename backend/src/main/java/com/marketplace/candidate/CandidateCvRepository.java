package com.marketplace.candidate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateCvRepository extends JpaRepository<CandidateCv, Long> {
    Optional<CandidateCv> findByCandidateId(Long candidateId);
}
