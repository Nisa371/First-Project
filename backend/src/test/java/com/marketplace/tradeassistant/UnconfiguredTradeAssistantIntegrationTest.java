package com.marketplace.tradeassistant;
import com.marketplace.ai.*;
import com.marketplace.candidate.CandidateType;
import com.marketplace.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class UnconfiguredTradeAssistantIntegrationTest extends TradeAssistantTestSupport {
    @Autowired AiTradeAssistantProvider provider;
    @Test void defaultBootsAndReturnsControlledUnavailableWithoutSavingFakeMessages() throws Exception {
        assertThat(provider).isInstanceOf(UnconfiguredAiTradeAssistantProvider.class);
        var user=candidate(CandidateType.TRADE);
        send(user,"চাকরি কীভাবে খুঁজব?").andExpect(status().isOk())
            .andExpect(jsonPath("$.failureCode").value("AI_PROVIDER_NOT_CONFIGURED")).andExpect(jsonPath("$.messages").isEmpty());
        getConversation(user).andExpect(status().isOk()).andExpect(jsonPath("$.messages").isEmpty());
        assertThat(conversations.findById(candidates.findByUserId(user.getId()).orElseThrow().getId())).isEmpty();
        mvc.perform(get("/api/candidates/me").header("Authorization",auth(user))).andExpect(status().isOk());
    }
    @Test void onlyActiveTradeCandidatesHaveAccess() throws Exception {
        var tech=candidate(CandidateType.TECH);
        getConversation(tech).andExpect(status().isForbidden()); send(tech,"প্রশ্ন").andExpect(status().isForbidden());
        for(var role: new Role[]{Role.EMPLOYER,Role.ADMIN,Role.EVALUATOR}) {
            var account=user(role);getConversation(account).andExpect(status().isForbidden());send(account,"প্রশ্ন").andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/trade/assistant/conversation")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/trade/assistant/messages").contentType("application/json").content("{\"message\":\"প্রশ্ন\"}"))
            .andExpect(status().isUnauthorized());
        var suspended=candidate(CandidateType.TRADE); suspended.setAccountStatus(com.marketplace.user.AccountStatus.SUSPENDED); users.saveAndFlush(suspended);
        getConversation(suspended).andExpect(status().isForbidden());
    }
    @Test void validatesLengthAndBlankMessages() throws Exception {
        var user=candidate(CandidateType.TRADE);
        send(user," ").andExpect(status().isBadRequest()); send(user,"অ".repeat(2001)).andExpect(status().isBadRequest());
    }
}
