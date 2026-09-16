package com.marketplace.assessment;

import java.time.Instant;
import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import jakarta.persistence.Lob;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.BaseEntity;
import com.marketplace.candidate.CandidateProfile;

@Getter
@Setter
@Entity
@Table(name = "assessment_attempts", check = @CheckConstraint(constraint = "auto_score is null or auto_score >= 0"))
public class AssessmentAttempt extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @Lob
    @Column(nullable = false)
    private String answersJson = "{}";

    @Column(nullable = true)
    private BigDecimal autoScore;

    @Column(nullable = false)
    private Instant startedAt;

    @Column(nullable = true)
    private Instant submittedAt;

    @Column(nullable = true)
    private Instant evaluatedAt;

    @Version
    @Setter(lombok.AccessLevel.NONE)
    private long version;

    @PrePersist
    private void initializeStartedAt() {
        if (startedAt == null) startedAt = Instant.now();
    }
}

