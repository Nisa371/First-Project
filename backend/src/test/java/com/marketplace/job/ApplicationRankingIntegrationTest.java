package com.marketplace.job;

import com.marketplace.auth.JwtService;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.user.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class ApplicationRankingIntegrationTest {
    @Autowired MockMvc mvc; @Autowired JwtService jwt;
    @Autowired UserRepository users; @Autowired EmployerProfileRepository employers;
    @Autowired CandidateProfileRepository candidates; @Autowired JobRepository jobs;
    @Autowired JobApplicationRepository applications; @Autowired ApplicationEvaluationService evaluation;

    User user(Role role) {
        var u=new User();u.setRole(role);u.setEmail(UUID.randomUUID()+"@example.test");u.setPasswordHash("test-only");
        return users.saveAndFlush(u);
    }
    User employer() {
        var u=user(Role.EMPLOYER);var e=new EmployerProfile();e.setUser(u);e.setCompanyName("Ranking company");
        employers.saveAndFlush(e);return u;
    }
    CandidateProfile candidate(int months) {
        var c=new CandidateProfile();c.setUser(user(Role.CANDIDATE));c.setFullName("Applicant");
        c.setCandidateType(CandidateType.TECH);c.setAvailability(Availability.AVAILABLE);c.setTotalExperienceMonths(months);
        return candidates.saveAndFlush(c);
    }
    Job job(User employer) {
        var j=new Job();j.setEmployer(employers.findByUserId(employer.getId()).orElseThrow());j.setTitle("Role");
        j.setDescription("Description");j.setLocation("Dhaka");j.setCandidateType(CandidateType.TECH);
        j.setExpectedExperienceMonths(12);j.setPrivateExpectations("PRIVATE_EXPECTATION");j.setStatus(JobStatus.ACTIVE);
        return jobs.saveAndFlush(j);
    }
    JobApplication apply(Job j,CandidateProfile c) {
        var a=new JobApplication();a.setJob(j);a.setCandidate(c);return applications.saveAndFlush(a);
    }
    String auth(User u) { return "Bearer "+jwt.issue(u.getId()); }
    String path(Job j) { return "/api/jobs/"+j.getId(); }
    ResultActions weights(User u,Job j,String cv,String portfolio,String experience,String assessment) throws Exception {
        return mvc.perform(put(path(j)+"/evaluation-weights").header("Authorization",auth(u)).contentType("application/json")
            .content("{\"cvWeight\":"+cv+",\"portfolioWeight\":"+portfolio+",\"experienceWeight\":"+experience+",\"assessmentWeight\":"+assessment+"}"));
    }
    ResultActions rank(User u,Job j) throws Exception {
        return mvc.perform(get(path(j)+"/applications").header("Authorization",auth(u)));
    }
    @Test void weightsPersistIndependentlyAndValidateEveryComponent() throws Exception {
        var e=employer();var j=job(e);var other=job(e);
        mvc.perform(get(path(j)+"/evaluation-weights").header("Authorization",auth(e)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.cvWeight").value(0.25)).andExpect(jsonPath("$.assessmentWeight").value(0.25));
        weights(e,j,"0","0","0","0").andExpect(status().isOk());
        weights(e,j,"1","1","1","1").andExpect(status().isOk());
        weights(e,j,"0.5","0.8","0.4","1").andExpect(status().isOk());
        mvc.perform(get(path(j)+"/evaluation-weights").header("Authorization",auth(e)))
            .andExpect(jsonPath("$.cvWeight").value(0.5)).andExpect(jsonPath("$.portfolioWeight").value(0.8))
            .andExpect(jsonPath("$.experienceWeight").value(0.4)).andExpect(jsonPath("$.assessmentWeight").value(1));
        assertThat(jobs.findById(other.getId()).orElseThrow().getCvWeight()).isEqualByComparingTo("0.25");
        for(String invalid:List.of("-0.1","1.1","null","\"NaN\"","\"Infinity\"","NaN","Infinity","\"bad\"","0.12345678901234567")) {
            weights(e,j,invalid,"0","0","0").andExpect(status().isBadRequest());
            weights(e,j,"0",invalid,"0","0").andExpect(status().isBadRequest());
            weights(e,j,"0","0",invalid,"0").andExpect(status().isBadRequest());
            weights(e,j,"0","0","0",invalid).andExpect(status().isBadRequest());
        }
        assertThat(jobs.findById(j.getId()).orElseThrow().getCvWeight()).isEqualByComparingTo("0.5");
    }
    @Test void rankingAndWeightsRequireOwnerAndEmployerRole() throws Exception {
        var e=employer();var outsider=employer();var c=candidate(12);var j=job(e);apply(j,c);
        weights(outsider,j,"1","1","1","1").andExpect(status().isNotFound());
        rank(outsider,j).andExpect(status().isNotFound());
        for(var u:List.of(c.getUser(),user(Role.EVALUATOR),user(Role.ADMIN))) {
            weights(u,j,"1","1","1","1").andExpect(status().isForbidden());
            rank(u,j).andExpect(status().isForbidden());
            mvc.perform(get(path(j)+"/evaluation-weights").header("Authorization",auth(u))).andExpect(status().isForbidden());
        }
        mvc.perform(get(path(j)+"/evaluation-weights").header("Authorization",auth(outsider))).andExpect(status().isNotFound());
        mvc.perform(get(path(j)+"/applications")).andExpect(status().isUnauthorized());
    }
    @Test void rankingReordersWithWeightsAndUsesCurrentExperienceAndJobRequirement() throws Exception {
        var e=employer();var j=job(e);var c=candidate(30);var a=apply(j,c);var b=apply(j,candidate(6));
        apply(job(e),candidate(100)); // Another job's applicant must never enter this ranking.
        evaluation.updateCvScore(a.getId(),new BigDecimal("0.1"));evaluation.updateCvScore(b.getId(),new BigDecimal("0.9"));
        weights(e,j,"0","0","1","0").andExpect(status().isOk());
        rank(e,j).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].id").value(a.getId()))
            .andExpect(jsonPath("$[0].experienceScore").value(2)).andExpect(jsonPath("$[0].finalScore").value(2));
        weights(e,j,"1","0","0","0").andExpect(status().isOk());
        rank(e,j).andExpect(jsonPath("$[0].id").value(b.getId())).andExpect(jsonPath("$[0].finalScore").value(0.9));
        c.setTotalExperienceMonths(47);candidates.saveAndFlush(c);
        weights(e,j,"0","0","1","0").andExpect(status().isOk());
        rank(e,j).andExpect(jsonPath("$[0].experienceScore").value(3));
        j=jobs.findById(j.getId()).orElseThrow();j.setExpectedExperienceMonths(24);jobs.saveAndFlush(j);
        rank(e,j).andExpect(jsonPath("$[0].experienceScore").value(1));
        weights(e,j,"0","0","0","0").andExpect(status().isOk());
        rank(e,j).andExpect(jsonPath("$[0].id").value(b.getId())).andExpect(jsonPath("$[0].finalScore").value(0));
        rank(e,j).andExpect(jsonPath("$[0].id").value(b.getId()));
    }
    @Test void nullableScoresAreApplicationSpecificAndInternalWritesAreValidated() throws Exception {
        var e=employer();var c=candidate(12);var j=job(e);var a=apply(j,c);var other=apply(job(e),c);
        weights(e,j,"1","1","1","1").andExpect(status().isOk());
        rank(e,j).andExpect(jsonPath("$[0].cvScore").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$[0].portfolioScore").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$[0].assessmentScore").value(org.hamcrest.Matchers.nullValue()))
            .andExpect(jsonPath("$[0].finalScore").value(1)).andExpect(jsonPath("$[0].evaluationStatus").value("NOT_EVALUATED"));
        evaluation.updateCvScore(a.getId(),new BigDecimal("0.7"));
        rank(e,j).andExpect(jsonPath("$[0].evaluationStatus").value("PARTIALLY_EVALUATED"));
        evaluation.updatePortfolioScore(a.getId(),new BigDecimal("0.6"));evaluation.updateAssessmentScore(a.getId(),new BigDecimal("0.8"));
        weights(e,j,"0.2","0.3","0.5","1").andExpect(status().isOk());
        rank(e,j).andExpect(jsonPath("$[0].finalScore").value(1.62)).andExpect(jsonPath("$[0].evaluationStatus").value("EVALUATED"));
        var untouched=applications.findById(other.getId()).orElseThrow();
        assertThat(untouched.getCvScore()).isNull();assertThat(untouched.getPortfolioScore()).isNull();assertThat(untouched.getAssessmentScore()).isNull();
        for(var invalid:Arrays.asList(new BigDecimal("-0.01"),new BigDecimal("1.01"),null)) {
            assertThatThrownBy(()->evaluation.updateCvScore(a.getId(),invalid)).isInstanceOf(com.marketplace.common.api.ApiException.class);
            assertThatThrownBy(()->evaluation.updatePortfolioScore(a.getId(),invalid)).isInstanceOf(com.marketplace.common.api.ApiException.class);
            assertThatThrownBy(()->evaluation.updateAssessmentScore(a.getId(),invalid)).isInstanceOf(com.marketplace.common.api.ApiException.class);
        }
        assertThat(applications.findById(a.getId()).orElseThrow().getCvScore()).isEqualByComparingTo("0.7");
    }
    @Test void rankingPreservesStatusesAndCandidatePrivacyAndIgnoresForgedScores() throws Exception {
        var e=employer();var c=candidate(0);var j=job(e);
        mvc.perform(post(path(j)+"/apply").header("Authorization",auth(c.getUser())).contentType("application/json")
            .content("{\"cvScore\":1,\"experienceScore\":99,\"finalScore\":999}"))
            .andExpect(status().isCreated());
        var a=applications.findByJobIdAndCandidateId(j.getId(),c.getId()).orElseThrow();
        for(String path:List.of("/api/candidates/me/jobs/"+j.getId(),"/api/candidates/me/applications")) {
            String body=mvc.perform(get(path).header("Authorization",auth(c.getUser()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(body).doesNotContain("PRIVATE_EXPECTATION","privateExpectations","cvWeight","finalScore","cvScore");
        }
        for(String status:List.of("UNDER_REVIEW","SHORTLISTED","REJECTED")) {
            mvc.perform(patch(path(j)+"/applications/"+a.getId()+"/status").header("Authorization",auth(e)).contentType("application/json")
                .content("{\"status\":\""+status+"\"}")).andExpect(status().isOk());
            rank(e,j).andExpect(jsonPath("$[0].status").value(status)).andExpect(jsonPath("$[0].finalScore").value(0));
        }
        mvc.perform(patch("/api/applications/"+a.getId()+"/withdraw").header("Authorization",auth(c.getUser()))).andExpect(status().isOk());
        rank(e,j).andExpect(jsonPath("$[0].status").value("WITHDRAWN"));
        assertThat(applications.findById(a.getId()).orElseThrow().getCvScore()).isNull();
    }
}
