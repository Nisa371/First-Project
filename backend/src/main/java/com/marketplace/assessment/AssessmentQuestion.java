package com.marketplace.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "assessment_questions", check = @CheckConstraint(constraint = "points > 0"))
public class AssessmentQuestion extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false, length = 3000)
    private String prompt;

    @Column(nullable = false, length = 1000)
    private String optionA;

    @Column(nullable = false, length = 1000)
    private String optionB;

    @Column(nullable = false, length = 1000)
    private String optionC;

    @Column(nullable = false, length = 1000)
    private String optionD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerOption correctOption;

    @Column(nullable = false)
    private int points = 1;

}

