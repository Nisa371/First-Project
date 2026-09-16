package com.marketplace.job;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortlistEntryRepository extends JpaRepository<ShortlistEntry, Long> {
    List<ShortlistEntry> findByJobIdAndJobEmployerUserId(Long jobId, Long userId);
    long countByJobId(Long jobId);
    java.util.Optional<ShortlistEntry> findByJobIdAndCandidateId(Long jobId, Long candidateId);
    boolean existsByJobIdAndCandidateId(Long jobId, Long candidateId);
}

