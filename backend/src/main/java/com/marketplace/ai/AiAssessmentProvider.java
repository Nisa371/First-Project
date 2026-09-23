package com.marketplace.ai;

import static com.marketplace.ai.AssessmentProviderDtos.*;

/** Vendor-neutral interview adapter. Production defaults to unavailable; no remote calls. */
public interface AiAssessmentProvider {
    AssessmentTurnResult generateNextTurn(AssessmentTurnRequest request);
    AssessmentEvaluationResult evaluateAssessment(AssessmentEvaluationRequest request);
}
