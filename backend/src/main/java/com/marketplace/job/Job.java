package com.marketplace.job;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.TimestampedEntity;
import com.marketplace.employer.EmployerProfile;
import com.marketplace.skill.Skill;
import com.marketplace.candidate.CandidateType;

@Getter
@Setter
@Entity
@Table(name = "jobs")
public class Job extends TimestampedEntity {
    @org.hibernate.annotations.ColumnDefault("0.25")
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("1")
    @Column(nullable=false, precision=20, scale=16, check=@jakarta.persistence.CheckConstraint(constraint="cv_weight between 0 and 1"))
    private java.math.BigDecimal cvWeight = new java.math.BigDecimal("0.25");

    @org.hibernate.annotations.ColumnDefault("0.25")
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("1")
    @Column(nullable=false, precision=20, scale=16, check=@jakarta.persistence.CheckConstraint(constraint="portfolio_weight between 0 and 1"))
    private java.math.BigDecimal portfolioWeight = new java.math.BigDecimal("0.25");

    @org.hibernate.annotations.ColumnDefault("0.25")
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("1")
    @Column(nullable=false, precision=20, scale=16, check=@jakarta.persistence.CheckConstraint(constraint="experience_weight between 0 and 1"))
    private java.math.BigDecimal experienceWeight = new java.math.BigDecimal("0.25");

    @org.hibernate.annotations.ColumnDefault("0.25")
    @jakarta.validation.constraints.NotNull
    @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("1")
    @Column(nullable=false, precision=20, scale=16, check=@jakarta.persistence.CheckConstraint(constraint="assessment_weight between 0 and 1"))
    private java.math.BigDecimal assessmentWeight = new java.math.BigDecimal("0.25");


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employer_id", nullable = false)
    private EmployerProfile employer;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 5000)
    private String description;

    @Column(length=5000) private String publicExpectations;
    @Column(length=5000) private String privateExpectations;
    @org.hibernate.annotations.ColumnDefault("0")
    @Column(nullable=false) private int expectedExperienceMonths;

    @Column(nullable = false, length = 255)
    private String location;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "required_skill_id", nullable = true)
    private Skill requiredSkill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CandidateType candidateType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType; // Legacy jobs remain explicitly unspecified until edited.
    private java.time.Instant activatedAt;
    private java.time.Instant portalClosesAt;
    private java.time.LocalDate employerRequestedEndDate;
    private java.time.Instant replacementWindowStartedAt;
    private java.time.Instant replacementWindowExpiresAt;
    private Integer replacementGuaranteeDaysSnapshot;
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="original_job_id")
    private Job originalJob;

    public java.time.Instant closingTime() {
        if (portalClosesAt != null) return portalClosesAt;
        if (originalJob != null) return originalJob.getReplacementWindowExpiresAt();
        var start = activatedAt != null ? activatedAt : getCreatedAt();
        return start == null ? null : start.atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant();
    }
    public boolean portalOpen() {
        var deadline=closingTime();
        return status==JobStatus.ACTIVE && deadline!=null && java.time.Instant.now().isBefore(deadline);
    }
    public JobStatus effectiveStatus() { return status==JobStatus.ACTIVE && !portalOpen()?JobStatus.CLOSED:status; }
    public static java.time.Instant closingDate(java.time.LocalDate requested, java.time.Instant now) {
        var maximum=now.atZone(java.time.ZoneOffset.UTC).plusMonths(1).toInstant();
        var end=requested==null?maximum:requested.atStartOfDay(java.time.ZoneId.of("Asia/Dhaka")).toInstant();
        if(!end.isAfter(now) || end.isAfter(maximum))
            throw new com.marketplace.common.api.ApiException(400,"INVALID_END_DATE","Job post ending date must be after publication and no later than one month after publication (Bangladesh time).");
        return end;
    }

}

