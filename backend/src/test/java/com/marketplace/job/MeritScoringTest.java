package com.marketplace.job;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class MeritScoringTest {
    @ParameterizedTest
    @CsvSource({"0,0,1", "30,0,1", "0,12,0", "6,12,0.5", "12,12,1", "18,12,1", "24,12,2", "30,12,2", "47,12,3"})
    void exactExperienceFormula(int current, int expected, String score) {
        assertThat(MeritScoring.experience(current,expected)).isEqualByComparingTo(score);
    }
    @Test void decimalPrecisionAndNegativeSources() {
        assertThat(MeritScoring.experience(1,3).precision()).isEqualTo(34);
        assertThatThrownBy(()->MeritScoring.experience(-1,12)).isInstanceOf(com.marketplace.common.api.ApiException.class);
        assertThatThrownBy(()->MeritScoring.experience(12,-1)).isInstanceOf(com.marketplace.common.api.ApiException.class);
    }
    @Test void rawWeightsAreNeverNormalized() {
        var weights=new JobDtos.EvaluationWeights(new BigDecimal("0.2"),new BigDecimal("0.3"),new BigDecimal("0.5"),BigDecimal.ONE);
        assertThat(MeritScoring.finalScore(BigDecimal.ONE,new BigDecimal("0.8"),new BigDecimal("0.7"),new BigDecimal("0.6"),weights)).isEqualByComparingTo("1.62");
        var all=new JobDtos.EvaluationWeights(BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE);
        assertThat(MeritScoring.finalScore(BigDecimal.ONE,null,null,null,all)).isEqualByComparingTo("1");
        assertThat(MeritScoring.finalScore(new BigDecimal("2"),BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,all)).isEqualByComparingTo("5");
        var zero=new JobDtos.EvaluationWeights(BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO);
        assertThat(MeritScoring.finalScore(new BigDecimal("2"),BigDecimal.ONE,BigDecimal.ONE,BigDecimal.ONE,zero)).isEqualByComparingTo("0");
    }
    @Test void evaluationStateDependsOnActualScores() {
        assertThat(MeritScoring.evaluationStatus(null,null,null)).isEqualTo("NOT_EVALUATED");
        assertThat(MeritScoring.evaluationStatus(BigDecimal.ZERO,null,null)).isEqualTo("PARTIALLY_EVALUATED");
        assertThat(MeritScoring.evaluationStatus(BigDecimal.ZERO,BigDecimal.ONE,BigDecimal.ONE)).isEqualTo("EVALUATED");
    }
}
