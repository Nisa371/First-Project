package com.marketplace.ai.gemini;

import com.marketplace.ai.*;
import static com.marketplace.ai.AiEvaluationDtos.*;

public class GeminiEvaluationProvider implements AiEvaluationProvider {
    private final GeminiClientGateway gateway;
    public GeminiEvaluationProvider(GeminiClientGateway gateway) { this.gateway=gateway; }
    public CvEvaluationResult evaluateCv(CvEvaluationRequest request) {
        var result = gateway.parse(gateway.generate("CV_EVALUATION", request.instruction() + " Include a concise job-relevant summary.",
            request.job(), request.candidate(), GeminiClientGateway.Output.SCORE));
        return new CvEvaluationResult(gateway.score(result));
    }
    public PortfolioEvaluationResult evaluatePortfolio(PortfolioEvaluationRequest request) {
        var result = gateway.parse(gateway.generate("PORTFOLIO_EVALUATION", request.instruction() + " Include a concise job-relevant summary.",
            request.job(), request.candidate(), GeminiClientGateway.Output.SCORE));
        return new PortfolioEvaluationResult(gateway.score(result));
    }
}
