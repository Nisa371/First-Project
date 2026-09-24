package com.marketplace.ai;

import static com.marketplace.ai.AssessmentProviderDtos.*;

/** Vendor-neutral interview adapter. Provider selection stays outside application services. */
public interface AiAssessmentProvider {
    AssessmentTurnResult generateNextTurn(AssessmentTurnRequest request);
    AssessmentEvaluationResult evaluateAssessment(AssessmentEvaluationRequest request);
}
