package com.marketplace.verification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, Long> {
    List<VerificationRecord> findByCandidateIdOrderBySubmittedAtDescIdDesc(Long candidateId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from VerificationRecord v where v.id = :id")
    Optional<VerificationRecord> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    List<VerificationRecord> findByStatusOrderBySubmittedAtAsc(VerificationStatus status);
    Optional<VerificationRecord> findFirstByCandidateIdOrderBySubmittedAtDescIdDesc(Long candidateId);
}

