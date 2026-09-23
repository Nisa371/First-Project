package com.marketplace.job;

import com.marketplace.candidate.CandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Internal write boundary for future evaluators. Deliberately has no HTTP controller. */
@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationEvaluationService {
    private final JobApplicationRepository applications;

    public void updateCvScore(Long applicationId, BigDecimal score) {
        MeritScoring.unitValue(score);
        application(applicationId).setCvScore(score);
    }

    public void updatePortfolioScore(Long applicationId, BigDecimal score) {
        MeritScoring.unitValue(score);
        application(applicationId).setPortfolioScore(score);
    }

    public void updateAssessmentScore(Long applicationId, BigDecimal score) {
        MeritScoring.unitValue(score);
        application(applicationId).setAssessmentScore(score);
    }

    private JobApplication application(Long id) {
        return applications.findForEvaluation(id).orElseThrow(CandidateService::missing);
    }
}
