package com.marketplace.training;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
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
@Table(name = "training_programs")
public class TrainingProgram extends BaseEntity {
    @Column(nullable = false, length = 200)
    private String providerName;

    @Column(nullable = false, length = 200)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "skill_id", nullable = true)
    private Skill skill;

    @Column(nullable = true, length = 3000)
    private String description;

    @Column(nullable = false)
    private boolean active = true;

}

