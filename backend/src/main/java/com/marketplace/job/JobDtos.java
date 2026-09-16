package com.marketplace.job;
import com.marketplace.candidate.CandidateType;
import jakarta.validation.constraints.*;
import java.time.Instant;
public final class JobDtos {
    private JobDtos() {}
    public record JobRequest(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=5000) String description,
        @NotBlank @Size(max=255) String location, @NotNull CandidateType candidateType, Long requiredSkillId) {}
    public record JobView(Long id, String title, String description, String location, CandidateType candidateType,
        Long requiredSkillId, String requiredSkillName, JobStatus status, long shortlistCount, Instant createdAt) {}
}
