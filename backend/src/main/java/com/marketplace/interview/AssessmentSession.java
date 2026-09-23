package com.marketplace.interview;

import com.marketplace.common.persistence.TimestampedEntity;
import com.marketplace.job.JobApplication;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="application_assessment_sessions") @lombok.Getter @lombok.Setter
public class AssessmentSession extends TimestampedEntity {
    public enum Status { NOT_STARTED, IN_PROGRESS, COMPLETED, FAILED }
    @OneToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="application_id", nullable=false, unique=true)
    private JobApplication jobApplication;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=24)
    private Status status = Status.NOT_STARTED;
    private Instant startedAt;
    // Set once the conversation is closed, including when final evaluation fails.
    private Instant completedAt;
    @Column(nullable=false) private int currentTurn;
    @Column(nullable=false) private int maxTurns;
    @Column(length=48) private String failureCode;
    @Column(length=2000) private String evaluationSummary;
}
