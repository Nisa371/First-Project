package com.marketplace.common.persistence;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class CreatedEntity extends BaseEntity {
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void initializeCreatedAt() {
        createdAt = Instant.now();
    }
}
