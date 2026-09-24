package com.marketplace.interview;

import com.google.genai.Models;
import com.marketplace.ai.gemini.GeminiTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import static com.marketplace.ai.gemini.GeminiTestConfiguration.respond;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(properties={"app.ai.provider=GEMINI", "app.ai.gemini.api-key=test-only-not-real", "app.assessment.max-turns=1"})
@AutoConfigureMockMvc @Import(GeminiTestConfiguration.class)
class GeminiAssessmentIntegrationTest extends AssessmentTestSupport {
    @Autowired Models models;
    @org.junit.jupiter.api.BeforeEach void resetProvider() { reset(models); }
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    com.marketplace.job.ApplicationEvaluationService scoreWrites;
    org.mockito.stubbing.OngoingStubbing<com.google.genai.types.GenerateContentResponse> providerCall() {
        return when(models.generateContent(anyString(), org.mockito.ArgumentMatchers.<java.util.List<com.google.genai.types.Content>>any(),
            any(com.google.genai.types.GenerateContentConfig.class)));
    }
    void providerCalls(int count) {
        verify(models,times(count)).generateContent(anyString(), org.mockito.ArgumentMatchers.<java.util.List<com.google.genai.types.Content>>any(),
            any(com.google.genai.types.GenerateContentConfig.class));
    }
    @Test void transcriptScoringPersistsAndRankingRemainsLocal() throws Exception {
        var e=employer(); var j=job(e); var c=candidate(0); var a=apply(j,c); var other=apply(job(e),c);
        respond(models,"{\"message\":\"How would you test an API?\",\"complete\":false}");
        start(c.getUser(),a).andExpect(status().isOk());
        respond(models,"{\"score\":0.81,\"summary\":\"Understands API testing\"}");
        answer(c.getUser(),sessionId(a),0,"Use integration tests").andExpect(jsonPath("$.status").value("COMPLETED"));
        review(e,a).andExpect(jsonPath("$.assessmentScore").value(0.81));
        assertThat(applications.findById(other.getId()).orElseThrow().getAssessmentScore()).isNull();
        clearInvocations(models);
        mvc.perform(put("/api/jobs/"+j.getId()+"/evaluation-weights").header("Authorization",auth(e)).contentType("application/json")
            .content("{\"cvWeight\":0,\"portfolioWeight\":0,\"experienceWeight\":0,\"assessmentWeight\":1}")).andExpect(status().isOk());
        mvc.perform(get("/api/jobs/"+j.getId()+"/applications").header("Authorization",auth(e)))
            .andExpect(jsonPath("$[0].finalScore").value(0.81));
        verifyNoInteractions(models);
    }
    @Test void invalidFinalScorePreservesPreviousScore() throws Exception {
        var c=candidate(12); var a=apply(job(employer()),c);
        scores.updateAssessmentScore(a.getId(),new java.math.BigDecimal("0.4"));
        respond(models,"{\"message\":\"How would you test an API?\",\"complete\":false}");
        start(c.getUser(),a);
        respond(models,"{\"score\":-0.1,\"summary\":\"Invalid\"}");
        answer(c.getUser(),sessionId(a),0,"Use tests").andExpect(jsonPath("$.failureCode").value("INVALID_AI_RESPONSE"));
        assertThat(applications.findById(a.getId()).orElseThrow().getAssessmentScore()).isEqualByComparingTo("0.4");
    }
    @Test void retriesPersistOneOpeningAndOneEvaluation() throws Exception {
        var c=candidate(0); var a=apply(job(employer()),c);
        providerCall().thenThrow(new com.google.genai.errors.ApiException(503,"UNAVAILABLE","PRIVATE"))
            .thenReturn(GeminiTestConfiguration.response("{\"message\":\"How do you test?\",\"complete\":false}"));
        start(c.getUser(),a).andExpect(jsonPath("$.failureCode").isEmpty()).andExpect(jsonPath("$.messages.length()").value(1));
        providerCalls(2);
        assertThat(messages.findByAssessmentSessionIdOrderBySequenceNumberAsc(sessionId(a))).hasSize(1);
        reset(models); clearInvocations(scoreWrites);
        providerCall().thenThrow(new com.google.genai.errors.ApiException(500,"UNAVAILABLE","PRIVATE"),
            new com.google.genai.errors.ApiException(503,"UNAVAILABLE","PRIVATE"))
            .thenReturn(GeminiTestConfiguration.response("{\"score\":0.81,\"summary\":\"Good\"}"));
        answer(c.getUser(),sessionId(a),0,"Use tests").andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.failureCode").isEmpty()).andExpect(jsonPath("$.messages.length()").value(2));
        providerCalls(3);
        verify(scoreWrites,times(1)).updateAssessmentScore(eq(a.getId()),any());
        assertThat(messages.findByAssessmentSessionIdOrderBySequenceNumberAsc(sessionId(a))).hasSize(2);
    }
    @Test void exhaustedTransientEvaluationPreservesPriorScore() throws Exception {
        var c=candidate(0); var a=apply(job(employer()),c);
        scores.updateAssessmentScore(a.getId(),new java.math.BigDecimal("0.4"));
        respond(models,"{\"message\":\"How do you test?\",\"complete\":false}");
        start(c.getUser(),a);
        reset(models); clearInvocations(scoreWrites);
        providerCall().thenThrow(new com.google.genai.errors.ApiException(503,"UNAVAILABLE","PRIVATE"));
        answer(c.getUser(),sessionId(a),0,"Use tests").andExpect(jsonPath("$.failureCode").value("AI_ASSESSMENT_FAILED"));
        providerCalls(3);
        verify(scoreWrites,never()).updateAssessmentScore(anyLong(),any());
        assertThat(applications.findById(a.getId()).orElseThrow().getAssessmentScore()).isEqualByComparingTo("0.4");
        assertThat(messages.findByAssessmentSessionIdOrderBySequenceNumberAsc(sessionId(a))).hasSize(2);
    }

}
