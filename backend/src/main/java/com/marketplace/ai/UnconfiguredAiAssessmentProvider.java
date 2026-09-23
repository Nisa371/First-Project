package com.marketplace.ai;

import static com.marketplace.ai.AssessmentProviderDtos.*;

public class UnconfiguredAiAssessmentProvider implements AiAssessmentProvider {
    public AssessmentTurnResult generateNextTurn(AssessmentTurnRequest request) { throw new AiEvaluationUnavailableException(); }
    public AssessmentEvaluationResult evaluateAssessment(AssessmentEvaluationRequest request) { throw new AiEvaluationUnavailableException(); }
}
