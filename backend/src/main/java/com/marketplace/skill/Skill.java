package com.marketplace.skill;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "skills", uniqueConstraints = @UniqueConstraint(name = "uk_skill_name", columnNames = {"name"}))
public class Skill extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 120)
    private String category;

    @Column(nullable = false)
    private boolean active = true;

}

