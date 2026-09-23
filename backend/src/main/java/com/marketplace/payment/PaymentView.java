package com.marketplace.payment;
import java.math.BigDecimal;
import java.time.Instant;
public record PaymentView(Long id, String reference, PaymentPurpose purpose, BigDecimal amount, String currency,
    PaymentStatus status, Instant createdAt, Instant completedAt, Long jobId, Long bookingId, String description) {}
