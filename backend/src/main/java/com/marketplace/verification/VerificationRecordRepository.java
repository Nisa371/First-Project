package com.marketplace.verification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationRecordRepository extends JpaRepository<VerificationRecord, Long> {
    @org.springframework.data.jpa.repository.Query("select v from VerificationRecord v left join v.candidate c where v.owner.id = :userId or c.user.id = :userId order by v.submittedAt desc, v.id desc")
    List<VerificationRecord> forUser(Long userId);
    @org.springframework.data.jpa.repository.Query("select coalesce(v.owner.id, c.user.id) from VerificationRecord v left join v.candidate c where v.id = :id")
    Optional<Long> ownerId(Long id);
    @org.springframework.data.jpa.repository.Query("""
        select v from VerificationRecord v left join v.candidate c left join c.user cu left join v.owner o
        where (:status = '' or (:status = 'PENDING' and v.status in (com.marketplace.verification.VerificationStatus.PENDING, com.marketplace.verification.VerificationStatus.IN_REVIEW))
          or (:status = 'FAILED' and v.status in (com.marketplace.verification.VerificationStatus.FAILED, com.marketplace.verification.VerificationStatus.FLAGGED))
          or cast(v.status as string) = :status)
        and (:role = '' or cast(coalesce(o.role, cu.role) as string) = :role)
        and (:companyId is null or exists (select e.id from EmployerProfile e where e.user.id = coalesce(o.id,cu.id) and e.companyType.id = :companyId))
        """)
    org.springframework.data.domain.Page<VerificationRecord> reviewPage(String status, String role, Long companyId, org.springframework.data.domain.Pageable pageable);
    List<VerificationRecord> findByRequirementIsNull();
    List<VerificationRecord> findAllByOrderBySubmittedAtDescIdDesc();
    List<VerificationRecord> findByCandidateIdOrderBySubmittedAtDescIdDesc(Long candidateId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from VerificationRecord v where v.id = :id")
    Optional<VerificationRecord> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    List<VerificationRecord> findByStatusOrderBySubmittedAtAsc(VerificationStatus status);
    Optional<VerificationRecord> findFirstByCandidateIdOrderBySubmittedAtDescIdDesc(Long candidateId);
}

