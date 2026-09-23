package com.marketplace.job;
import com.marketplace.candidate.CandidateType;
import jakarta.validation.constraints.*;
import java.time.Instant;
public final class JobDtos {
    private JobDtos() {}
    public record JobRequest(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=5000) String description,
        @NotBlank @Size(max=255) String location, @NotNull CandidateType candidateType, Long requiredSkillId,
        @Size(max=5000) String publicExpectations, @Size(max=5000) String privateExpectations, @Min(0) Integer expectedExperienceMonths) {}
    public record JobView(Long id, String title, String description, String location, CandidateType candidateType,
        Long requiredSkillId, String requiredSkillName, JobStatus status, long shortlistCount, long applicationCount, Instant createdAt, String publicExpectations, String privateExpectations, int expectedExperienceMonths) {}
    public record PublicJob(Long id, String title, String description, String companyName, String location,
        CandidateType candidateType, String requiredSkillName, JobStatus status, String publicExpectations,
        int expectedExperienceMonths, Instant createdAt, Long applicationId, ApplicationStatus applicationStatus, boolean hasApplied,
        Long companyTypeId, String companyTypeName) {}
    public record JobSearch(@Size(max=200) String search, @Size(max=255) String location,
        @Positive Long companyTypeId, @Min(0) Integer minExperience, @Min(0) Integer maxExperience,
        CandidateType candidateTrack, @Min(0) Integer page, @Min(1) @Max(50) Integer size, String sort) {
        public JobSearch {
            page = page == null ? 0 : page;
            size = size == null ? 12 : size;
            sort = sort == null ? "newest" : sort;
        }
    }
    public record JobPage(java.util.List<PublicJob> content, long totalElements, int page, int size, int totalPages) {}
    public record ApplicationView(Long id, PublicJob job, ApplicationStatus status, Instant appliedAt, Instant updatedAt, String assessmentStatus) {}
    public record Applicant(Long id, Long jobId, ApplicationStatus status, Instant appliedAt, Instant updatedAt,
        com.marketplace.candidate.CandidateDtos.CandidateCard candidate,
        java.math.BigDecimal cvScore, java.math.BigDecimal portfolioScore, java.math.BigDecimal experienceScore,
        java.math.BigDecimal assessmentScore, java.math.BigDecimal finalScore, String evaluationStatus,
        com.marketplace.ai.AiEvaluationDtos.AttemptView cvEvaluation,
        com.marketplace.ai.AiEvaluationDtos.AttemptView portfolioEvaluation, String assessmentStatus) {}
    public record EvaluationWeights(
        @NotNull @DecimalMin("0") @DecimalMax("1") java.math.BigDecimal cvWeight,
        @NotNull @DecimalMin("0") @DecimalMax("1") java.math.BigDecimal portfolioWeight,
        @NotNull @DecimalMin("0") @DecimalMax("1") java.math.BigDecimal experienceWeight,
        @NotNull @DecimalMin("0") @DecimalMax("1") java.math.BigDecimal assessmentWeight) {}
    public record StatusRequest(@NotNull ApplicationStatus status) {}
}
