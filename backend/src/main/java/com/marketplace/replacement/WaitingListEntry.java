package com.marketplace.replacement;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.BaseEntity;
import com.marketplace.candidate.CandidateProfile;
import com.marketplace.skill.Skill;

@Getter
@Setter
@Entity
@Table(name = "waiting_list_entries", uniqueConstraints = {
        @UniqueConstraint(name = "uk_queue_active_membership", columnNames = {"candidate_id", "skill_id", "active_membership"}),
        @UniqueConstraint(name = "uk_queue_candidate_reservation", columnNames = {"candidate_id", "reservation_marker"})
}, indexes = @Index(name = "idx_queue_fifo", columnList = "skill_id,status,joined_at,id"), check = @CheckConstraint(constraint = """
        ((status = 'EXITED' and active_membership is null)
          or (status <> 'EXITED' and active_membership is not null and active_membership = true))
        and ((status = 'RESERVED' and reservation_marker is not null and reservation_marker = true)
          or (status <> 'RESERVED' and reservation_marker is null))
        """))
public class WaitingListEntry extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueStatus status = QueueStatus.QUEUED;

    @Column(nullable = false, updatable = false)
    private Instant joinedAt;

    @Column(nullable = true)
    private Instant reservedAt;

    @Column(nullable = true, length = 500)
    private String exitReason;

    @Setter(lombok.AccessLevel.NONE)
    @Column(nullable = true)
    private Boolean activeMembership;

    @Setter(lombok.AccessLevel.NONE)
    @Column(nullable = true)
    private Boolean reservationMarker;

    @Version
    @Setter(lombok.AccessLevel.NONE)
    private long version;

    // NULL markers allow multiple historical rows in H2 and MySQL unique constraints.
    @PrePersist
    @PreUpdate
    private void synchronizeMembership() {
        if (joinedAt == null) joinedAt = Instant.now();
        activeMembership = status == QueueStatus.EXITED ? null : Boolean.TRUE;
        reservationMarker = status == QueueStatus.RESERVED ? Boolean.TRUE : null;
    }
}

