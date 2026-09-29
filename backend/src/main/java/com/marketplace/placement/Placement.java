package com.marketplace.placement;

import java.time.Instant;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.CreatedEntity;
import com.marketplace.candidate.CandidateProfile;
import com.marketplace.employer.EmployerProfile;
import com.marketplace.job.Job;
import com.marketplace.skill.Skill;

@Getter
@Setter
@Entity
@Table(name = "placements")
public class Placement extends CreatedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private EmployerProfile employer;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "job_id", nullable = true)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlacementStatus status = PlacementStatus.PENDING;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = true)
    private Instant endedAt;

    private Instant candidateLeftAt;

    public boolean isReplacementSourceEligible() {
        return status == PlacementStatus.ACTIVE || (status == PlacementStatus.COMPLETED && candidateLeftAt != null);
    }

    @Column(nullable = false)
    private boolean guaranteeEligible = false;

    @Column(nullable = true)
    private Instant guaranteeExpiresAt;

    @Column(precision=14, scale=2, updatable=false)
    private java.math.BigDecimal agreedFirstMonthSalary;
    @Column(precision=3, scale=2, updatable=false)
    private java.math.BigDecimal placementFeeRate;
    @Column(precision=14, scale=2, updatable=false)
    private java.math.BigDecimal placementFeeAmount;
    @Column(updatable=false)
    private Instant feeAgreementAcceptedAt;
    @Column(updatable=false)
    private Long acceptedByEmployerUserId;
    @Column(length=40, updatable=false)
    private String termsVersion;

    @Version
    @Setter(lombok.AccessLevel.NONE)
    private long version;

}

