package com.marketplace.placement;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PlacementRepository extends JpaRepository<Placement, Long> {
    Optional<Placement> findByIdAndEmployerUserId(Long id, Long userId);
    List<Placement> findByEmployerIdOrderByCreatedAtDesc(Long employerId);
    List<Placement> findByCandidateIdOrderByCreatedAtDesc(Long candidateId);
    boolean existsByCandidateIdAndStatusIn(Long candidateId, Collection<PlacementStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Placement p where p.id = :id")
    Optional<Placement> findByIdForUpdate(@Param("id") Long id);
}

