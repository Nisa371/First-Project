package com.marketplace.ai;

/** Adapters must map strict numeric responses to these types; never parse scores from prose. */
public interface AiEvaluationProvider {
    AiEvaluationDtos.CvEvaluationResult evaluateCv(AiEvaluationDtos.CvEvaluationRequest request);
    AiEvaluationDtos.PortfolioEvaluationResult evaluatePortfolio(AiEvaluationDtos.PortfolioEvaluationRequest request);
}
