package com.marketplace.interview;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class AssessmentDtos {
    private AssessmentDtos() {}
    public static final int MAX_ANSWER_LENGTH = 4000;
    public record Answer(@NotBlank @Size(max=MAX_ANSWER_LENGTH) String response) {}
    public record Message(String senderRole, String content, int sequenceNumber, Instant createdAt) {}
    public record SessionView(Long id, Long applicationId, String jobTitle, String status, int currentTurn, int maxTurns,
        Instant startedAt, Instant completedAt, String failureCode, boolean canStart, boolean canAnswer, List<Message> messages) {}
    public record EmployerView(SessionView session, java.math.BigDecimal assessmentScore, String summary) {}
}
