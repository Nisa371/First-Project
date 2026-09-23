package com.marketplace.payment;

import com.marketplace.common.persistence.CreatedEntity;
import com.marketplace.user.User;
import com.marketplace.job.Job;
import com.marketplace.booking.Booking;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @Entity
@Table(name="payment_transactions", check=@CheckConstraint(name="payment_resource_check", constraint="(purpose = 'JOB_POSTING' and job_id is not null and booking_id is null) or (purpose = 'SESSION_BOOKING' and booking_id is not null and job_id is null)"))
public class PaymentTransaction extends CreatedEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(nullable=false, updatable=false)
    private User payer;
    @Enumerated(EnumType.STRING) @Column(nullable=false, updatable=false) private PaymentPurpose purpose;
    @Column(nullable=false, precision=12, scale=2, updatable=false) private BigDecimal amount;
    @Column(nullable=false, length=3, updatable=false) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private PaymentStatus status = PaymentStatus.PENDING;
    @Column(nullable=false, unique=true, length=50, updatable=false) private String reference;
    private Instant completedAt;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="job_id", unique=true, updatable=false) private Job job;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="booking_id", unique=true, updatable=false) private Booking booking;
    @PrePersist @PreUpdate void validateResource() {
        if (purpose == null || (purpose == PaymentPurpose.JOB_POSTING ? job == null || booking != null : booking == null || job != null))
            throw new IllegalStateException("Payment purpose must match exactly one resource");
    }
}
