package com.marketplace.assessment;
import com.marketplace.candidate.CandidateType;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class AssessmentStrategyTest {
    final TechAssessmentStrategy tech=new TechAssessmentStrategy();
    final VoiceAssessmentStrategy voice=new VoiceAssessmentStrategy();
    final AssessmentStrategyFactory factory=new AssessmentStrategyFactory(tech,voice);
    AssessmentQuestion question(long id,int points,AnswerOption answer) {
        var q=mock(AssessmentQuestion.class); when(q.getId()).thenReturn(id); when(q.getPoints()).thenReturn(points); when(q.getCorrectOption()).thenReturn(answer); return q;
    }
    @Test void factorySelectsRealTrackBehavior() {
        assertThat(factory.forType(CandidateType.TECH)).isSameAs(tech);
        assertThat(factory.forType(CandidateType.TRADE)).isSameAs(voice);
        assertThat(factory.forType(CandidateType.TRADE).score(List.of(question(1,1,AnswerOption.A)),Map.of(1L,AnswerOption.A))).isNull();
    }
    @Test void weightedScoreIsDeterministicRoundedAndBounded() {
        var qs=List.of(question(1,1,AnswerOption.A),question(2,2,AnswerOption.B));
        assertThat(tech.score(qs,Map.of(2L,AnswerOption.B))).isEqualByComparingTo("66.67");
        assertThat(tech.score(qs,Map.of())).isEqualByComparingTo("0");
        assertThat(tech.score(qs,Map.of(1L,AnswerOption.A,2L,AnswerOption.B,3L,AnswerOption.D))).isEqualByComparingTo("100");
        assertThatThrownBy(() -> tech.score(List.of(),Map.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
