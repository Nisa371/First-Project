package com.marketplace.placement;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "replacement_guarantee_policy")
@Getter
@Setter
public class ReplacementGuaranteePolicy {
    @Id
    private Long id = 1L;
    @Column(nullable = false)
    private int days = 30;
}
