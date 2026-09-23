package com.marketplace.candidate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.OneToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.TimestampedEntity;
import com.marketplace.user.User;

@Getter
@Setter
@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile extends TimestampedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidateType candidateType = CandidateType.TECH;

    @Column(nullable = false, length = 160)
    private String fullName;

    @Column(nullable = true, length = 32)
    private String phone;

    @Column(nullable = true, length = 255)
    private String location;

    @Column(nullable = true, length = 2000)
    private String bio;

    @Column(nullable = true, length = 2000)
    private String educationSummary;

    @Column(nullable = true, length = 2000)
    private String experienceSummary;

    @org.hibernate.annotations.ColumnDefault("0")
    @Column(nullable=false) private int totalExperienceMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability = Availability.UNAVAILABLE;

    @Column(nullable = true, length = 120)
    private String primaryTradeCategory;

    @Column(nullable = true, length = 2048)
    private String portfolioUrl;

    @Column(nullable = true, length = 255)
    private String cvStoredName;

    @Column(nullable = true, length = 255)
    private String cvOriginalName;

    @Column(nullable = true, length = 100)
    private String cvContentType;

    @Column(length = 80)
    private String photoStoredName;

    @Version
    @Setter(lombok.AccessLevel.NONE)
    private long version;

}

