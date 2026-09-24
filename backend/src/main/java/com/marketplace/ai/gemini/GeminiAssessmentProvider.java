package com.marketplace.ai.gemini;

import com.marketplace.ai.*;
import java.util.Map;
import static com.marketplace.ai.AssessmentProviderDtos.*;

public class GeminiAssessmentProvider implements AiAssessmentProvider {
    private final GeminiClientGateway gateway;
    public GeminiAssessmentProvider(GeminiClientGateway gateway) { this.gateway=gateway; }
    public AssessmentTurnResult generateNextTurn(AssessmentTurnRequest request) {
        var result = gateway.parse(gateway.generate("ASSESSMENT_TURN", request.instruction(),
            Map.of("job", request.job(), "currentTurn", request.currentTurn(), "maxTurns", request.maxTurns()),
            Map.of("candidate", request.candidate(), "transcript", request.transcript()), GeminiClientGateway.Output.TURN));
        var complete = result.get("complete");
        if (complete == null || !complete.isBoolean()) throw new AiProviderInvalidResponseException();
        return new AssessmentTurnResult(gateway.text(result, "message", 2000), complete.booleanValue());
    }
    public AssessmentEvaluationResult evaluateAssessment(AssessmentEvaluationRequest request) {
        var result = gateway.parse(gateway.generate("ASSESSMENT_EVALUATION", request.instruction() + " Include a concise summary.", request.job(),
            Map.of("candidate", request.candidate(), "transcript", request.transcript()), GeminiClientGateway.Output.SCORE));
        return new AssessmentEvaluationResult(gateway.score(result), gateway.text(result, "summary", 1000));
    }
}
