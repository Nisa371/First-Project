package com.marketplace.candidate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public final class StructuredCvDtos {
    private StructuredCvDtos() {}
    public record Education(@NotBlank @Size(max = 160) String institution, @NotBlank @Size(max = 160) String qualification, @Size(max = 160) String fieldOfStudy, LocalDate startDate, LocalDate endDate, Boolean current, @Size(max = 80) String grade, @Size(max = 3000) String description) {
        public Education { current = Boolean.TRUE.equals(current); }
    }
    public record Experience(@NotBlank @Size(max = 160) String organization, @NotBlank @Size(max = 160) String title, LocalDate startDate, LocalDate endDate, Boolean current, @Size(max = 3000) String description) {
        public Experience { current = Boolean.TRUE.equals(current); }
    }
    public record Project(@NotBlank @Size(max = 160) String name, @Size(max = 3000) String description, @Size(max = 500) String technologies, @Size(max = 2048) String projectUrl, @Size(max = 2048) String repositoryUrl, LocalDate startDate, LocalDate endDate) {}
    public record Certification(@NotBlank @Size(max = 160) String name, @NotBlank @Size(max = 160) String organization, LocalDate issueDate, @Size(max = 2048) String credentialUrl) {}
    public record Language(@NotBlank @Size(max = 80) String name, @NotBlank @Size(max = 80) String proficiency) {}
    public record Achievement(@NotBlank @Size(max = 160) String title, @Size(max = 160) String issuer, LocalDate awardDate, @Size(max = 3000) String description) {}
    public record Content(@Size(max = 4000) String summary, @Size(max = 2048) String linkedinUrl, @Size(max = 2048) String githubUrl,
        @NotNull @Size(max = 30) List<@NotNull @Valid Education> education,
        @NotNull @Size(max = 30) List<@NotNull @Valid Experience> experience,
        @NotNull @Size(max = 30) List<@NotNull @Valid Project> projects,
        @NotNull @Size(max = 30) List<@NotNull @Valid Certification> certifications,
        @NotNull @Size(max = 30) List<@NotNull @Valid Language> languages,
        @NotNull @Size(max = 30) List<@NotNull @Valid Achievement> achievements) {}
    public record Header(String fullName, String email, String phone, String location,
        String profilePhotoUrl, String portfolioUrl, List<CandidateDtos.SkillView> skills) {}
    // A clean employment-data DTO, also suitable for future internal evaluation consumers.
    public record View(Header header, Content content, Instant updatedAt, boolean empty) {}
}
