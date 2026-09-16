package com.marketplace.assessment;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
@Component
public class VoiceAssessmentStrategy implements AssessmentStrategy {
    @Override public BigDecimal score(List<AssessmentQuestion> questions, Map<Long,AnswerOption> answers) {
        // TRADE responses inform the evaluator; practical/voice skill cannot be certified by MCQ alone.
        return null;
    }
}
