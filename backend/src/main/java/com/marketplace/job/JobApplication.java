package com.marketplace.job;
import com.marketplace.common.persistence.TimestampedEntity;
import com.marketplace.candidate.CandidateProfile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
@Entity @Getter @Setter
@org.hibernate.annotations.DynamicUpdate
@Table(name="job_applications", uniqueConstraints=@UniqueConstraint(name="uk_application_job_candidate",columnNames={"job_id","candidate_id"}))
public class JobApplication extends TimestampedEntity {
    @Embedded @AttributeOverrides({
        @AttributeOverride(name="status", column=@Column(name="cv_evaluation_status", length=24)),
        @AttributeOverride(name="failureCode", column=@Column(name="cv_failure_code", length=48)),
        @AttributeOverride(name="attemptedAt", column=@Column(name="cv_attempted_at")),
        @AttributeOverride(name="evaluatedAt", column=@Column(name="cv_evaluated_at")),
        @AttributeOverride(name="evaluationVersion", column=@Column(name="cv_evaluation_version", length=16))
    })
    private com.marketplace.ai.EvaluationAttempt cvAttempt = new com.marketplace.ai.EvaluationAttempt();
    @Embedded @AttributeOverrides({
        @AttributeOverride(name="status", column=@Column(name="portfolio_evaluation_status", length=24)),
        @AttributeOverride(name="failureCode", column=@Column(name="portfolio_failure_code", length=48)),
        @AttributeOverride(name="attemptedAt", column=@Column(name="portfolio_attempted_at")),
        @AttributeOverride(name="evaluatedAt", column=@Column(name="portfolio_evaluated_at")),
        @AttributeOverride(name="evaluationVersion", column=@Column(name="portfolio_evaluation_version", length=16))
    })
    private com.marketplace.ai.EvaluationAttempt portfolioAttempt = new com.marketplace.ai.EvaluationAttempt();

    @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("1")
    @Column(precision=20, scale=16, check=@CheckConstraint(constraint="cv_score between 0 and 1"))
    private java.math.BigDecimal cvScore;
    @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("1")
    @Column(precision=20, scale=16, check=@CheckConstraint(constraint="portfolio_score between 0 and 1"))
    private java.math.BigDecimal portfolioScore;
    @jakarta.validation.constraints.DecimalMin("0") @jakarta.validation.constraints.DecimalMax("1")
    @Column(precision=20, scale=16, check=@CheckConstraint(constraint="assessment_score between 0 and 1"))
    private java.math.BigDecimal assessmentScore;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="job_id",nullable=false) private Job job;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="candidate_id",nullable=false) private CandidateProfile candidate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private ApplicationStatus status=ApplicationStatus.APPLIED;
}
