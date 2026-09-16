package com.marketplace.assessment;

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
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.CreatedEntity;
import com.marketplace.candidate.CandidateType;
import com.marketplace.skill.Skill;

@Getter
@Setter
@Entity
@Table(name = "assessments", check = @CheckConstraint(constraint = "(duration_minutes is null or duration_minutes > 0) and (passing_score is null or passing_score >= 0)"))
public class Assessment extends CreatedEntity {
    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateType candidateType = CandidateType.TECH;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "skill_id", nullable = true)
    private Skill skill;

    @Column(nullable = true)
    private Integer durationMinutes;

    @Column(nullable = true)
    private BigDecimal passingScore;

    @Column(nullable = false)
    private boolean active = true;

}

