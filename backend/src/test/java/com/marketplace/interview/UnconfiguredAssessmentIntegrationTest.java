package com.marketplace.interview;
import com.marketplace.ai.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class UnconfiguredAssessmentIntegrationTest extends AssessmentTestSupport {
    @Autowired AiAssessmentProvider provider;
    @Test void realDefaultBootsAndStartAndProgressReturnControlledUnavailableState() throws Exception {
        assertThat(provider).isInstanceOf(UnconfiguredAiAssessmentProvider.class);
        var c=candidate(12); var a=apply(job(employer()),c);
        start(c.getUser(),a).andExpect(status().isOk()).andExpect(jsonPath("$.failureCode").value("AI_PROVIDER_NOT_CONFIGURED"))
            .andExpect(jsonPath("$.status").value("NOT_STARTED")).andExpect(jsonPath("$.messages").isEmpty());
        long id=sessionId(a); start(c.getUser(),a).andExpect(jsonPath("$.id").value(id));
        // Simulate a persisted session from a previously available adapter.
        var s=sessions.findById(id).orElseThrow(); s.setStatus(AssessmentSession.Status.IN_PROGRESS); s.setStartedAt(java.time.Instant.now()); sessions.saveAndFlush(s);
        answer(c.getUser(),id,0,"Typed answer").andExpect(status().isOk()).andExpect(jsonPath("$.failureCode").value("AI_PROVIDER_NOT_CONFIGURED"))
            .andExpect(jsonPath("$.messages").isEmpty()).andExpect(jsonPath("$.currentTurn").value(0));
        assertThat(applications.findById(a.getId()).orElseThrow().getAssessmentScore()).isNull();
        assertThat(messages.findByAssessmentSessionIdOrderBySequenceNumberAsc(id)).isEmpty();
    }
}
