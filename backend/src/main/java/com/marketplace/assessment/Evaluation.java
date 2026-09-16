package com.marketplace.assessment;

import java.math.BigDecimal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.CreatedEntity;
import com.marketplace.user.User;

@Getter
@Setter
@Entity
@Table(name = "evaluations", check = @CheckConstraint(constraint = "score >= 0"))
public class Evaluation extends CreatedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false, unique = true)
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "evaluator_user_id", nullable = true)
    private User evaluatorUser;

    @Column(nullable = false)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Recommendation recommendation;

    @Column(nullable = true, length = 3000)
    private String candidateFeedback;

    @Column(nullable = true, length = 3000)
    private String internalNotes;

    @Column(nullable = false)
    private boolean released = false;

}

