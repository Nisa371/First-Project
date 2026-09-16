package com.marketplace.assessment;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import jakarta.validation.constraints.*;
import com.marketplace.candidate.CandidateType;
public final class AssessmentDtos {
    private AssessmentDtos() {}
    public record QuestionView(Long id, String prompt, List<String> options) {}
    public record AssessmentView(Long id, String title, CandidateType candidateType, BigDecimal passingScore,
        List<QuestionView> questions) {}
    public record AnswersRequest(@NotNull @Size(max=100) Map<@Positive Long, @NotNull AnswerOption> answers) {}
    public record ResultView(BigDecimal score, Recommendation recommendation, String feedback) {}
    public record AttemptView(Long id, AssessmentView assessment, AttemptStatus status, Map<Long,AnswerOption> answers,
        Instant startedAt, Instant submittedAt, BigDecimal autoScore, ResultView result) {}
    public record EvaluationRequest(@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal score,
        @NotNull Recommendation recommendation, @NotBlank @Size(max=3000) String feedback,
        @Size(max=3000) String internalNotes) {}
    public record ReviewView(AttemptView attempt, String candidateName, BigDecimal autoScore, ResultView draft,
        String internalNotes, boolean released, boolean editable) {}
}
