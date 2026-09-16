package com.marketplace.training;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReferralRepository extends JpaRepository<Referral, Long> {
    List<Referral> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
}

