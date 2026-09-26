package com.marketplace.ai;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.CandidateService;
import com.marketplace.common.api.ApiException;
import com.marketplace.job.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.BigDecimal;

@Service @lombok.RequiredArgsConstructor @Transactional
public class AiCandidateEvaluationService {
    public enum Component { CV, PORTFOLIO, BOTH }
    private final JobRepository jobs;
    private final JobApplicationRepository applications;
    private final CurrentAccount current;
    private final EvaluationRequestBuilder inputs;
    private final AiEvaluationProvider provider;
    private final ApplicationEvaluationService scores;
    private final JobApplicationService views;

    // Internal after-commit entry point. No HTTP endpoint or request security context.
    public void evaluateSubmitted(Long applicationId) {
        var a = applications.findForEvaluation(applicationId).orElse(null);
        if (a == null || a.getStatus() == ApplicationStatus.WITHDRAWN) return;
        // The application lock serializes duplicate deliveries and manual evaluations.
        if (a.getCvScore() == null) attempt(a, true);
        if (a.getPortfolioScore() == null) attempt(a, false);
    }

    @PreAuthorize("hasRole('EMPLOYER')")
    public JobDtos.Applicant evaluate(Long jobId, Long applicationId, Component component) {
        jobs.findOwnedForUpdate(jobId, current.requireActive().getId()).orElseThrow(CandidateService::missing);
        var a = applications.findForEvaluation(applicationId).filter(v -> v.getJob().getId().equals(jobId))
            .orElseThrow(CandidateService::missing);
        if (a.getStatus() == ApplicationStatus.WITHDRAWN)
            throw new ApiException(409, "APPLICATION_WITHDRAWN", "This application has been withdrawn.");
        if (component != Component.PORTFOLIO) attempt(a, true);
        if (component != Component.CV) attempt(a, false);
        applications.flush();
        return views.applicant(jobId, applicationId);
    }

    private void attempt(JobApplication a, boolean cv) {
        var attempt = cv ? a.getCvAttempt() : a.getPortfolioAttempt();
        if (attempt == null) {
            attempt = new EvaluationAttempt();
            if (cv) a.setCvAttempt(attempt); else a.setPortfolioAttempt(attempt);
        }
        BigDecimal score;
        try {
            Double raw;
            if (cv) {
                var result = provider.evaluateCv(inputs.cv(a));
                raw = result == null ? null : result.score();
            } else {
                var result = provider.evaluatePortfolio(inputs.portfolio(a));
                raw = result == null ? null : result.score();
            }
            score = validatedScore(raw);
        } catch (AiEvaluationUnavailableException e) {
            attempt.finish("UNAVAILABLE", "AI_PROVIDER_NOT_CONFIGURED"); return;
        } catch (EvaluationRequestBuilder.InsufficientInput e) {
            attempt.finish("UNAVAILABLE", cv ? "INSUFFICIENT_CV_DATA" : "INSUFFICIENT_PORTFOLIO_DATA"); return;
        } catch (InvalidResponse | AiProviderInvalidResponseException e) {
            attempt.finish("FAILED", "INVALID_AI_RESPONSE"); return;
        } catch (RuntimeException e) {
            // Never return provider exception text, prompts or configuration to the caller.
            attempt.finish("FAILED", "AI_EVALUATION_FAILED"); return;
        }
        // Keep database failures outside provider error handling so writes remain atomic.
        if (cv) scores.updateCvScore(a.getId(), score); else scores.updatePortfolioScore(a.getId(), score);
        attempt.finish("COMPLETED", null);
    }

    static BigDecimal validatedScore(Double value) {
        if (value == null || !Double.isFinite(value) || value < 0 || value > 1) throw new InvalidResponse();
        var score = BigDecimal.valueOf(value);
        if (score.stripTrailingZeros().scale() > 16) throw new InvalidResponse();
        return score;
    }
    private static class InvalidResponse extends RuntimeException {}
}
