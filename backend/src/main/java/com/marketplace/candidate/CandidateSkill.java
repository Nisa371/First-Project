package com.marketplace.candidate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.BaseEntity;
import com.marketplace.skill.Skill;

@Getter
@Setter
@Entity
@Table(name = "candidate_skills", uniqueConstraints = @UniqueConstraint(name = "uk_candidate_skill", columnNames = {"candidate_id", "skill_id"}))
public class CandidateSkill extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateProfile candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = true, length = 80)
    private String proficiencyLevel;

}

