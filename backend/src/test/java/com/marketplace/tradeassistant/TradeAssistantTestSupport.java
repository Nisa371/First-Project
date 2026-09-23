package com.marketplace.tradeassistant;
import com.marketplace.auth.JwtService;
import com.marketplace.candidate.*;
import com.marketplace.user.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
abstract class TradeAssistantTestSupport {
    @Autowired MockMvc mvc; @Autowired JwtService jwt; @Autowired ObjectMapper mapper;
    @Autowired UserRepository users; @Autowired CandidateProfileRepository candidates;
    @Autowired TradeAssistantRepository conversations;
    User user(Role role) {
        var u=new User(); u.setRole(role); u.setEmail(UUID.randomUUID()+"@example.test"); u.setPasswordHash("PRIVATE_PASSWORD_HASH");
        return users.saveAndFlush(u);
    }
    User candidate(CandidateType type) {
        var c=new CandidateProfile(); c.setUser(user(Role.CANDIDATE)); c.setFullName("PRIVATE_FULL_NAME");
        c.setBio("PRIVATE_BIO_WITH_NID_CONTENT"); c.setCandidateType(type); candidates.saveAndFlush(c); return c.getUser();
    }
    String auth(User u) { return "Bearer "+jwt.issue(u.getId()); }
    ResultActions getConversation(User u) throws Exception { return mvc.perform(get("/api/trade/assistant/conversation").header("Authorization",auth(u))); }
    ResultActions send(User u,String text) throws Exception {
        return mvc.perform(post("/api/trade/assistant/messages").header("Authorization",auth(u)).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("message",text))));
    }
}
