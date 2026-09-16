package com.marketplace.auth;

import com.marketplace.user.*;
import com.marketplace.audit.AuditLogRepository;
import com.marketplace.candidate.CandidateProfileRepository;
import com.marketplace.employer.EmployerProfileRepository;
import com.marketplace.common.api.ApiException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@org.springframework.context.annotation.Import(AuthIntegrationTest.RoleProbe.class)
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired CandidateProfileRepository candidates;
    @Autowired EmployerProfileRepository employers;
    @Autowired AuditLogRepository audits;
    @Autowired PasswordEncoder passwords;
    @Autowired CurrentAccount current;
    @Autowired JwtService jwt;

    // Test-only endpoints verify the security chain without implementing later modules.
    @org.springframework.web.bind.annotation.RestController
    static class RoleProbe {
        @org.springframework.web.bind.annotation.GetMapping("/api/admin/probe")
        Map<String, Boolean> admin() { return Map.of("allowed", true); }
        @org.springframework.web.bind.annotation.GetMapping("/api/evaluators/probe")
        Map<String, Boolean> evaluator() { return Map.of("allowed", true); }
    }

    @Test void serverManagedRolesCanLoginAndRoleChangesApplyToExistingTokens() throws Exception {
        for (Role role : new Role[]{Role.ADMIN, Role.EVALUATOR}) {
            var user = new User();
            user.setEmail(email()); user.setPasswordHash(passwords.encode("SafePass123!")); user.setRole(role);
            user = users.save(user);
            var result = mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(mapper.writeValueAsString(Map.of("email",user.getEmail(),"password","SafePass123!"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value(role.name())).andReturn();
            String token = mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
            String path = role == Role.ADMIN ? "/api/admin/probe" : "/api/evaluators/probe";
            mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value(role.name()));
            user.setRole(Role.EMPLOYER); users.save(user);
            mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isForbidden());
        }
    }

    String email() { return UUID.randomUUID() + "@example.com"; }
    String payload(String email, String type, String track) {
        return "{\"email\":\"" + email + "\",\"password\":\"SafePass123!\",\"accountType\":\"" + type
                + "\",\"fullName\":\"Demo Candidate\",\"companyName\":\"Demo Company\""
                + (track == null ? "" : ",\"candidateType\":\"" + track + "\"") + "}";
    }
    String register(String email, String type, String track) throws Exception {
        var result = mvc.perform(post("/api/auth/register").contentType("application/json").content(payload(email,type,track)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.tokenType").value("Bearer")).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }

    @Test void candidateTracksRegisterLoginAndMe() throws Exception {
        for (String track : new String[]{"TECH", "TRADE"}) {
            String email = email();
            String token = register(email.toUpperCase(), "CANDIDATE", track);
            var user = users.findByEmailIgnoreCase(email).orElseThrow();
            assertThat(user.getEmail()).isEqualTo(email);
            assertThat(passwords.matches("SafePass123!", user.getPasswordHash())).isTrue();
            assertThat(user.getPasswordHash()).doesNotContain("SafePass");
            assertThat(candidates.findByUserId(user.getId())).isPresent();
            assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc("USER", user.getId()))
                    .hasSize(1).allSatisfy(log -> assertThat(log.getDetails()).isNull());
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CANDIDATE"))
                    .andExpect(jsonPath("$.candidateType").value(track)).andExpect(jsonPath("$.passwordHash").doesNotExist());
            mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(mapper.writeValueAsString(Map.of("email", email, "password", "SafePass123!"))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        }
    }

    @Test void employerRegistrationAndDuplicateAreAtomic() throws Exception {
        String email = email();
        String token = register(email, "EMPLOYER", null);
        var user = users.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(employers.findByUserId(user.getId())).isPresent();
        assertThat(candidates.findByUserId(user.getId())).isEmpty();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.displayName").value("Demo Company"));
        mvc.perform(post("/api/auth/register").contentType("application/json").content(payload(email.toUpperCase(),"CANDIDATE","TECH")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("EMAIL_IN_USE"));
        assertThat(candidates.findByUserId(user.getId())).isEmpty();
    }

    @Test void rejectsPrivilegedSignupAndAuthoritativeFields() throws Exception {
        for (String body : new String[]{payload(email(),"ADMIN",null), payload(email(),"EVALUATOR",null),
                payload(email(),"CANDIDATE","TECH").replace("}", ",\"role\":\"ADMIN\"}"),
                payload(email(),"CANDIDATE","TECH").replace("}", ",\"accountStatus\":\"ACTIVE\"}")}) {
            mvc.perform(post("/api/auth/register").contentType("application/json").content(body))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        }
    }

    @Test void rejectsInvalidAndIncompleteRequestsSafely() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.timestamp").exists()).andExpect(jsonPath("$.path").value("/api/auth/register"));
        for (String body : new String[]{payload(email(),"CANDIDATE",null), payload(email(),"EMPLOYER","TRADE"),
                payload(email(),"CANDIDATE","TECH").replace("Demo Candidate", "   ")}) {
            mvc.perform(post("/api/auth/register").contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/auth/login").contentType("application/json").content("not json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("The request contains invalid or unsupported fields."));
    }

    @Test void invalidCredentialsTokensAndAnonymousAreRejected() throws Exception {
        String email = email();
        register(email, "CANDIDATE", "TECH");
        for (String address : new String[]{email, email()}) {
            mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(mapper.writeValueAsString(Map.of("email", address, "password", "wrong"))))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
        }
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
        for (String token : new String[]{"Bearer junk", "Basic abc", "Bearer " + jwt.issue(Long.MAX_VALUE)}) {
            mvc.perform(get("/api/auth/me").header("Authorization", token)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
        }
    }

    @Test void currentAccountStatusRevokesExistingTokenAndBlocksLogin() throws Exception {
        for (AccountStatus status : new AccountStatus[]{AccountStatus.SUSPENDED, AccountStatus.BLOCKED, AccountStatus.FLAGGED}) {
            String email = email();
            String token = register(email,"CANDIDATE","TECH");
            var user = users.findByEmailIgnoreCase(email).orElseThrow();
            user.setAccountStatus(status); users.save(user);
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("ACCOUNT_INACTIVE"));
            mvc.perform(post("/api/auth/login").contentType("application/json")
                    .content(mapper.writeValueAsString(Map.of("email",email,"password","SafePass123!"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test void roleGatesAndOwnershipRejectCrossAccountAccess() throws Exception {
        String email = email();
        String token = register(email, "CANDIDATE", "TECH");
        for (String path : new String[]{"/api/admin/users", "/api/evaluators/reviews", "/api/employers/me"}) {
            mvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
        }
        String employer = register(email(), "EMPLOYER", null);
        mvc.perform(get("/api/candidates/me").header("Authorization", "Bearer " + employer)).andExpect(status().isForbidden());
        Long id = users.findByEmailIgnoreCase(email).orElseThrow().getId();
        try {
            SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(id,null,java.util.List.of()));
            current.requireOwner(id);
            assertThatThrownBy(() -> current.requireOwner(Long.MAX_VALUE)).isInstanceOf(ApiException.class)
                    .extracting("status").isEqualTo(403);
        } finally { SecurityContextHolder.clearContext(); }
    }

    @Test void authPreflightAcceptsAuthorizationHeader() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin","http://localhost:5173")
                .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","Content-Type,Authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://localhost:5173"));
    }
}
