package com.marketplace.replacement;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ReplacementRequestRepository extends JpaRepository<ReplacementRequest, Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<ReplacementRequest> {
    Optional<ReplacementRequest> findFirstByFreeReplacementJobIdOrderByRequestedAtAscIdAsc(Long jobId);
    default Optional<ReplacementRequest> findByFreeReplacementJobId(Long jobId) { return findFirstByFreeReplacementJobIdOrderByRequestedAtAscIdAsc(jobId); }
    List<ReplacementRequest> findByFreeReplacementJobIdOrderByRequestedAtAscIdAsc(Long jobId);
    List<ReplacementRequest> findByPlacementJobIdOrderByRequestedAtAscIdAsc(Long jobId);
    List<ReplacementRequest> findByPlacementIdOrderByRequestedAtDescIdDesc(Long placementId);
    Optional<ReplacementRequest> findByPlacementIdAndActiveRequestTrue(Long placementId);
    Optional<ReplacementRequest> findByIdAndEmployerUserId(Long id, Long userId);
    List<ReplacementRequest> findByEmployerIdOrderByRequestedAtDesc(Long employerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReplacementRequest r where r.id = :id")
    Optional<ReplacementRequest> findByIdForUpdate(@Param("id") Long id);
}

