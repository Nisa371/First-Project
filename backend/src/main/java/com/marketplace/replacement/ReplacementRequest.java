package com.marketplace.replacement;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
import com.marketplace.placement.Placement;
import com.marketplace.employer.EmployerProfile;
import com.marketplace.candidate.CandidateProfile;

@Getter
@Setter
@Entity
@Table(name = "replacement_requests", uniqueConstraints = @UniqueConstraint(name = "uk_replacement_active_placement", columnNames = {"placement_id", "active_request"}), check = @CheckConstraint(constraint = """
        target_completion_at >= requested_at
        and ((status in ('COMPLETED', 'FAILED') and active_request is null)
          or (status not in ('COMPLETED', 'FAILED') and active_request is not null and active_request = true))
        """))
public class ReplacementRequest extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "placement_id", nullable = false)
    private Placement placement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private EmployerProfile employer;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReplacementStatus status = ReplacementStatus.REQUESTED;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "selected_candidate_id", nullable = true)
    private CandidateProfile selectedCandidate;

    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "replacement_placement_id", nullable = true, unique = true)
    private Placement replacementPlacement;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(nullable = false, updatable = false)
    private Instant targetCompletionAt;

    @Column(nullable = true)
    private Instant actualCompletionAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlaStatus slaStatus = SlaStatus.PENDING;

    @Column(nullable = true, length = 2000)
    private String failureReason;

    @Setter(lombok.AccessLevel.NONE)
    @Column(nullable = true)
    private Boolean activeRequest;

    @Version
    @Setter(lombok.AccessLevel.NONE)
    private long version;

    @PrePersist
    @PreUpdate
    private void synchronizeActiveRequest() {
        activeRequest = status == ReplacementStatus.COMPLETED || status == ReplacementStatus.FAILED
                ? null : Boolean.TRUE;
    }
}

