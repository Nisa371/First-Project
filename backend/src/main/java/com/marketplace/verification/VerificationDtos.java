package com.marketplace.verification;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class VerificationDtos {
    private VerificationDtos() {}
    public record Submission(@NotBlank @Size(max=255) String identityReference) {}
    public record Decision(@NotNull VerificationStatus status, @NotBlank @Size(max=3000) String notes) {}
    // Identity evidence and internal notes are deliberately absent from candidate history.
    public record StatusView(Long id, VerificationStatus status, Instant submittedAt, Instant reviewedAt) {}
    public record ReviewView(Long id, String candidateName, String candidateType, String trade,
        String location, String identityReference, VerificationStatus status, String notes,
        Instant submittedAt, Instant reviewedAt) {}
}
