package com.marketplace.employer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.TimestampedEntity;
import com.marketplace.user.User;

@Getter
@Setter
@Entity
@Table(name = "employer_profiles")
public class EmployerProfile extends TimestampedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 200)
    private String companyName;

    @Column(nullable = true, length = 120)
    private String industry;

    @Column(nullable = true, length = 32)
    private String contactPhone;

    @Column(nullable = true, length = 500)
    private String address;

    @Column(nullable = true, length = 2000)
    private String description;

}

