package com.marketplace.candidate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<CandidateProfile> {
    Optional<CandidateProfile> findByUserId(Long userId);

    @Query("select c.id from CandidateProfile c where c.candidateType = :type order by c.id")
    java.util.List<Long> findIdsByCandidateType(@Param("type") CandidateType type);

    // Lock the candidate before multi-skill reservation or placement changes.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CandidateProfile c where c.id = :id")
    Optional<CandidateProfile> findByIdForUpdate(@Param("id") Long id);
}

