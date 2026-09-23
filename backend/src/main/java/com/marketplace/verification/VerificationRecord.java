package com.marketplace.verification;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;
import com.marketplace.common.persistence.BaseEntity;
import com.marketplace.candidate.CandidateProfile;
import com.marketplace.user.User;

@Getter
@Setter
@Entity
@Table(name = "verification_records")
public class VerificationRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private CandidateProfile candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id")
    private VerificationRequirement requirement;

    @Column(length=80) private String storedName;
    @Column(length=180) private String originalName;
    @Column(length=80) private String contentType;
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus status = VerificationStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "reviewer_user_id", nullable = true)
    private User reviewerUser;

    @Column(nullable = true, length = 255)
    private String identityReference;

    @Column(nullable = true, length = 3000)
    private String reviewerNotes;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(nullable = true)
    private Instant reviewedAt;

    @PrePersist
    private void initializeSubmittedAt() {
        if (submittedAt == null) submittedAt = Instant.now();
    }
}

