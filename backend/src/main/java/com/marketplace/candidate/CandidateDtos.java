package com.marketplace.candidate;

import java.util.List;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public final class CandidateDtos {
    private CandidateDtos() {}
    public record ProfileRequest(@NotBlank @Size(max=160) String fullName,
        @Size(max=32) @Pattern(regexp="[+0-9 ()-]*") String phone, @Size(max=255) String location,
        @Size(max=2000) String bio, @Size(max=2000) String educationSummary,
        @Size(max=2000) String experienceSummary, @NotNull Availability availability,
        @Size(max=120) String primaryTradeCategory,
        @Size(max=2048) @Pattern(regexp="^(https?://[^\\s]+)?$", message="must be an http or https URL") String portfolioUrl) {}
    public record SkillRequest(@NotNull Long skillId, @Size(max=80) String proficiencyLevel) {}
    public record SkillView(Long id, String name, String category, String proficiencyLevel) {}
    public record ResultView(BigDecimal score, String recommendation) {}
    public record ProfileView(Long id, CandidateType candidateType, String fullName, String phone,
        String location, String bio, String educationSummary, String experienceSummary,
        Availability availability, String primaryTradeCategory, String portfolioUrl, String cvOriginalName,
        List<SkillView> skills, String verificationStatus, List<ResultView> releasedResults) {}
    public record CandidateCard(Long id, CandidateType candidateType, String fullName, String location,
        String bio, String experienceSummary, Availability availability, String primaryTradeCategory,
        String portfolioUrl, boolean hasCv, List<SkillView> skills, String verificationStatus,
        List<ResultView> releasedResults) {}
    public record SearchPage(List<CandidateCard> content, long totalElements, int page, int totalPages) {}
}
