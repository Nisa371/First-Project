package com.marketplace.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.TimestampedEntity;

@Getter
@Setter
@Entity
@Table(name = "users", uniqueConstraints = { @UniqueConstraint(name = "uk_user_email", columnNames = {"email"}),
        @UniqueConstraint(name = "uk_user_phone", columnNames = {"phone"}) })
public class User extends TimestampedEntity {
    @Column(length = 254)
    private String email;

    @Column(length = 14)
    private String phone;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CANDIDATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @PrePersist
    @PreUpdate
    private void normalizeEmail() {
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            throw new IllegalStateException("An email address or phone number is required.");
        }
        if (email != null) {
            email = email.strip().toLowerCase(java.util.Locale.ROOT);
        }
    }
}

