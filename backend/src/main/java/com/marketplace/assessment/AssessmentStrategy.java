package com.marketplace.assessment;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
public interface AssessmentStrategy {
    // Percentage for supported MCQs; null means a practical/manual review is required.
    BigDecimal score(List<AssessmentQuestion> questions, Map<Long,AnswerOption> answers);
}
