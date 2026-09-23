package com.marketplace.ai;

import com.marketplace.candidate.StructuredCvDtos;
import java.util.List;

public final class AiEvaluationDtos {
    private AiEvaluationDtos() {}
    public record JobContext(String title, String description, String publicExpectations,
        String privateExpectations, int expectedExperienceMonths, String requiredSkill, String track, String companyCategory) {}
    public record CvContext(StructuredCvDtos.Content content, List<String> skills, int experienceMonths, String portfolioUrl) {}
    public record PortfolioContext(List<StructuredCvDtos.Project> projects, List<StructuredCvDtos.Achievement> achievements,
        List<String> skills, String githubUrl, String portfolioUrl) {}
    public record CvEvaluationRequest(String instruction, String version, JobContext job, CvContext candidate) {}
    public record PortfolioEvaluationRequest(String instruction, String version, JobContext job, PortfolioContext candidate) {}
    public record CvEvaluationResult(Double score) {}
    public record PortfolioEvaluationResult(Double score) {}
    public record AttemptView(String status, String failureCode, java.time.Instant attemptedAt, java.time.Instant evaluatedAt) {}
}
