package com.marketplace.ai;

import java.util.List;

public final class AssessmentProviderDtos {
    private AssessmentProviderDtos() {}
    public record CandidateContext(String summary, int experienceMonths, List<String> skills, List<String> projects) {}
    public record TranscriptEntry(String senderRole, String content, int sequenceNumber) {}
    public record AssessmentTurnRequest(String instruction, AiEvaluationDtos.JobContext job, CandidateContext candidate,
        List<TranscriptEntry> transcript, int currentTurn, int maxTurns) {}
    public record AssessmentTurnResult(String message, boolean complete) {}
    public record AssessmentEvaluationRequest(String instruction, AiEvaluationDtos.JobContext job,
        CandidateContext candidate, List<TranscriptEntry> transcript) {}
    public record AssessmentEvaluationResult(Double score, String summary) {}
}
