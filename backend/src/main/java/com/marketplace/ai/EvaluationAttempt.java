package com.marketplace.ai;

import jakarta.persistence.*;
import java.time.Instant;

@Embeddable @lombok.Getter
public class EvaluationAttempt {
    @Column(length=24) private String status;
    @Column(length=48) private String failureCode;
    private Instant attemptedAt;
    private Instant evaluatedAt;
    @Column(length=16) private String evaluationVersion;
    public void finish(String status, String code) {
        this.status = status; failureCode = code; attemptedAt = Instant.now();
        evaluationVersion = EvaluationRequestBuilder.VERSION;
        if ("COMPLETED".equals(status)) evaluatedAt = attemptedAt;
    }
    public static AiEvaluationDtos.AttemptView view(EvaluationAttempt attempt, java.math.BigDecimal score) {
        return new AiEvaluationDtos.AttemptView(attempt == null || attempt.status == null
            ? (score == null ? "NOT_EVALUATED" : "COMPLETED") : attempt.status,
            attempt == null ? null : attempt.failureCode, attempt == null ? null : attempt.attemptedAt,
            attempt == null ? null : attempt.evaluatedAt);
    }
}
