package com.marketplace.assessment;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;
@Component
public class TechAssessmentStrategy implements AssessmentStrategy {
    @Override public BigDecimal score(List<AssessmentQuestion> questions, Map<Long,AnswerOption> answers) {
        long total=questions.stream().mapToLong(AssessmentQuestion::getPoints).sum();
        if(total<=0) throw new IllegalArgumentException("Assessment must have positive question points");
        long earned=questions.stream().filter(q -> q.getCorrectOption()==answers.get(q.getId())).mapToLong(AssessmentQuestion::getPoints).sum();
        return BigDecimal.valueOf(earned).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP);
    }
}
