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
    // Mirrors checklist applicability/latest semantics without loading submission history.
    @org.springframework.data.jpa.repository.Query("""
        select count(v) from VerificationRecord v
        left join v.candidate c left join c.user cu left join v.owner o left join v.requirement vr
        where coalesce(o.role, cu.role) = :role
          and v.status in (com.marketplace.verification.VerificationStatus.PENDING, com.marketplace.verification.VerificationStatus.IN_REVIEW)
          and exists (
            select r.id from VerificationRequirement r
            where r.active = true
              and (r.id = vr.id or (vr.id is null and c.id is not null and r.code = 'CANDIDATE_NID'))
              and ((coalesce(o.role, cu.role) = com.marketplace.user.Role.CANDIDATE
                    and r.targetType = com.marketplace.verification.VerificationTarget.CANDIDATE and r.companyType is null)
                or (coalesce(o.role, cu.role) = com.marketplace.user.Role.EMPLOYER and exists (
                    select e.id from EmployerProfile e left join e.companyType t
                    where e.user.id = coalesce(o.id, cu.id)
                      and (r.companyType is null or r.companyType.id = t.id)
                      and (r.targetType = com.marketplace.verification.VerificationTarget.EMPLOYER
                        or (r.targetType = com.marketplace.verification.VerificationTarget.HOUSEHOLD_EMPLOYER and t.code = 'HOUSEHOLD')
                        or (r.targetType = com.marketplace.verification.VerificationTarget.COMPANY_EMPLOYER and (t.code is null or t.code <> 'HOUSEHOLD')))))))
          and not exists (
            select newer.id from VerificationRecord newer
            left join newer.candidate nc left join nc.user nu left join newer.requirement nr
            where (newer.owner.id = coalesce(o.id, cu.id) or nu.id = coalesce(o.id, cu.id))
              and (nr.id = vr.id
                or (vr.id is null and (nr.id is null or nr.code = 'CANDIDATE_NID'))
                or (vr.code = 'CANDIDATE_NID' and nr.id is null and nc.id is not null))
              and (newer.submittedAt > v.submittedAt or (newer.submittedAt = v.submittedAt and newer.id > v.id)))
        """)
    long countAwaitingReview(com.marketplace.user.Role role);
    List<VerificationRecord> findByRequirementIsNull();
    List<VerificationRecord> findAllByOrderBySubmittedAtDescIdDesc();
    List<VerificationRecord> findByCandidateIdOrderBySubmittedAtDescIdDesc(Long candidateId);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from VerificationRecord v where v.id = :id")
    Optional<VerificationRecord> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    List<VerificationRecord> findByStatusOrderBySubmittedAtAsc(VerificationStatus status);
    Optional<VerificationRecord> findFirstByCandidateIdOrderBySubmittedAtDescIdDesc(Long candidateId);
}

