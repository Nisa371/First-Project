package com.marketplace.ai;

public class UnconfiguredAiEvaluationProvider implements AiEvaluationProvider {
    public AiEvaluationDtos.CvEvaluationResult evaluateCv(AiEvaluationDtos.CvEvaluationRequest request) {
        throw new AiEvaluationUnavailableException();
    }
    public AiEvaluationDtos.PortfolioEvaluationResult evaluatePortfolio(AiEvaluationDtos.PortfolioEvaluationRequest request) {
        throw new AiEvaluationUnavailableException();
    }
}
