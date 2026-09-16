package com.marketplace.replacement;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface WaitingListEntryRepository extends JpaRepository<WaitingListEntry, Long> {
    List<WaitingListEntry> findByCandidateIdOrderByJoinedAtDescIdDesc(Long candidateId);
    List<WaitingListEntry> findAllByOrderByJoinedAtAscIdAsc();
    List<WaitingListEntry> findBySkillIdAndStatusOrderByJoinedAtAscIdAsc(Long skillId, QueueStatus status);
    List<WaitingListEntry> findByCandidateIdAndStatusIn(Long candidateId, Collection<QueueStatus> statuses);
    boolean existsByCandidateIdAndSkillIdAndActiveMembershipTrue(Long candidateId, Long skillId);
    boolean existsByCandidateIdAndStatus(Long candidateId, QueueStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WaitingListEntry w where w.id = :id")
    Optional<WaitingListEntry> findByIdForUpdate(@Param("id") Long id);
}

