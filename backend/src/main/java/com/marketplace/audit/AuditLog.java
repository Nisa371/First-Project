package com.marketplace.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.CreatedEntity;
import com.marketplace.user.User;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog extends CreatedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "actor_user_id", nullable = true)
    private User actorUser;

    @Column(nullable = false, length = 120)
    private String action;

    @Column(nullable = false, length = 120)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = true, length = 3000)
    private String details;

}

