package com.marketplace.marketplace;

import com.marketplace.auth.JwtService;
import com.marketplace.candidate.*;
import com.marketplace.user.*;
import com.marketplace.skill.*;
import com.marketplace.assessment.*;
import com.marketplace.verification.*;
import com.marketplace.replacement.*;
import com.marketplace.placement.*;
import com.marketplace.employer.*;
import java.util.*;
import java.util.concurrent.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class VerificationReadinessIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtService jwt;
    @Autowired UserRepository users;
    @Autowired CandidateProfileRepository candidates;
    @Autowired CandidateSkillRepository candidateSkills;
    @Autowired SkillRepository skills;
    @Autowired VerificationRecordRepository verifications;
    @Autowired AssessmentRepository assessments;
    @Autowired AssessmentAttemptRepository attempts;
    @Autowired EvaluationRepository evaluations;
    @Autowired QueueEligibilityService eligibility;
    @Autowired WaitingListEntryRepository queue;
    @Autowired PlacementRepository placements;
    @Autowired EmployerProfileRepository employers;

    User user(Role role) { var u=new User();u.setEmail(UUID.randomUUID()+"@example.com");u.setPasswordHash("test-only-unusable-hash");u.setRole(role);return users.saveAndFlush(u); }
    String auth(User u) { return "Bearer "+jwt.issue(u.getId()); }
    CandidateProfile candidate() { var c=new CandidateProfile(); c.setUser(user(Role.CANDIDATE));c.setCandidateType(CandidateType.TRADE);c.setFullName("রহিম উদ্দিন");c.setPrimaryTradeCategory("Driving");c.setAvailability(Availability.AVAILABLE);return candidates.saveAndFlush(c); }
    Skill skill(CandidateProfile c) { var s=new Skill();s.setName("Trade "+UUID.randomUUID());s.setCategory("TRADE");s=skills.saveAndFlush(s);var link=new CandidateSkill();link.setCandidate(c);link.setSkill(s);candidateSkills.saveAndFlush(link);return s; }
    Long submit(CandidateProfile c) throws Exception { var r=mvc.perform(post("/api/verifications/me").header("Authorization",auth(c.getUser())).contentType("application/json").content("{\"identityReference\":\"PRIVATE-DEMO-001\"}"))
        .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.identityReference").doesNotExist()).andReturn();return mapper.readTree(r.getResponse().getContentAsString()).get("id").asLong(); }
    void decision(Long id,User reviewer,String status) throws Exception { mvc.perform(post("/api/evaluator/verifications/"+id+"/review").header("Authorization",auth(reviewer)).contentType("application/json").content("{\"status\":\""+status+"\",\"notes\":\"PRIVATE-NOTES\"}"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(status)).andExpect(jsonPath("$.reviewedAt").isNotEmpty()); }
    Evaluation evaluation(CandidateProfile c,Skill skill,Recommendation result,boolean released) { var a=new Assessment();a.setTitle("Practical");a.setCandidateType(CandidateType.TRADE);a.setSkill(skill);assessments.saveAndFlush(a);var attempt=new AssessmentAttempt();attempt.setCandidate(c);attempt.setAssessment(a);attempt.setStatus(AttemptStatus.EVALUATED);attempts.saveAndFlush(attempt);var e=new Evaluation();e.setAttempt(attempt);e.setScore(BigDecimal.valueOf(90));e.setRecommendation(result);e.setReleased(released);return evaluations.saveAndFlush(e); }
    boolean passed(CandidateProfile c,String code) { return eligibility.check(c.getId(),null).checks().stream().filter(v->v.code().equals(code)).findFirst().orElseThrow().passed(); }

    @Test void verificationOwnershipPrivacyValidationAndFinality() throws Exception {
        var c=candidate();var other=candidate();var reviewer=user(Role.EVALUATOR);var employer=user(Role.EMPLOYER);
        mvc.perform(post("/api/verifications/me").header("Authorization",auth(c.getUser())).contentType("application/json").content("{\"identityReference\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/verifications/me").header("Authorization",auth(c.getUser())).contentType("application/json").content("{\"identityReference\":\"x\",\"status\":\"VERIFIED\"}")).andExpect(status().isBadRequest());
        var id=submit(c);
        mvc.perform(get("/api/verifications/me").header("Authorization",auth(other.getUser()))).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/evaluator/verifications/"+id).header("Authorization",auth(c.getUser()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/evaluator/verifications/"+id).header("Authorization",auth(employer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/verifications/me").header("Authorization",auth(employer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/verifications/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/evaluator/verifications/"+id+"/review").header("Authorization",auth(c.getUser())).contentType("application/json").content("{\"status\":\"VERIFIED\",\"notes\":\"fake\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/evaluator/verifications/"+id).header("Authorization",auth(reviewer))).andExpect(jsonPath("$.identityReference").value("PRIVATE-DEMO-001"));
        mvc.perform(post("/api/evaluator/verifications/"+id+"/review").header("Authorization",auth(reviewer)).contentType("application/json").content("{\"status\":\"PENDING\",\"notes\":\"note\"}")).andExpect(status().isBadRequest());
        decision(id,reviewer,"VERIFIED");
        var history=mvc.perform(get("/api/verifications/me").header("Authorization",auth(c.getUser()))).andExpect(jsonPath("$[0].status").value("VERIFIED")).andReturn().getResponse().getContentAsString();assertThat(history).doesNotContain("PRIVATE", "identityReference", "notes");
        mvc.perform(post("/api/verifications/me").header("Authorization",auth(c.getUser())).contentType("application/json").content("{\"identityReference\":\"another\"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/evaluator/verifications/"+id+"/review").header("Authorization",auth(reviewer)).contentType("application/json").content("{\"status\":\"FAILED\",\"notes\":\"change\"}")).andExpect(status().isConflict());
    }
    @Test void failedAndFlaggedResubmissionPreservesTimeline() throws Exception {
        var c=candidate();var reviewer=user(Role.EVALUATOR);var first=submit(c);decision(first,reviewer,"FAILED");var second=submit(c);decision(second,reviewer,"FLAGGED");var third=submit(c);
        mvc.perform(get("/api/verifications/me").header("Authorization",auth(c.getUser()))).andExpect(jsonPath("$.length()").value(3)).andExpect(jsonPath("$[0].id").value(third)).andExpect(jsonPath("$[1].status").value("FLAGGED")).andExpect(jsonPath("$[2].status").value("FAILED"));
        mvc.perform(get("/api/evaluator/verifications").header("Authorization",auth(reviewer))).andExpect(status().isOk());
    }
    @Test void concurrentSubmissionsAndReviewsHaveOneWinner() throws Exception {
        var c=candidate();var r1=user(Role.EVALUATOR);var r2=user(Role.EVALUATOR);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);var futures=new ArrayList<Future<Integer>>();
            for(int i=0;i<2;i++) futures.add(pool.submit(()->{gate.await();return mvc.perform(post("/api/verifications/me").header("Authorization",auth(c.getUser())).contentType("application/json").content("{\"identityReference\":\"DEMO\"}")).andReturn().getResponse().getStatus();}));
            gate.countDown();assertThat(List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
            var id=verifications.findFirstByCandidateIdOrderBySubmittedAtDescIdDesc(c.getId()).orElseThrow().getId();var reviewGate=new CountDownLatch(1);futures.clear();
            for(var reviewer:List.of(r1,r2)) futures.add(pool.submit(()->{reviewGate.await();return mvc.perform(post("/api/evaluator/verifications/"+id+"/review").header("Authorization",auth(reviewer)).contentType("application/json").content("{\"status\":\"VERIFIED\",\"notes\":\"Reviewed\"}")).andReturn().getResponse().getStatus();}));
            reviewGate.countDown();assertThat(List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
    }
    @Test void readinessRequiresLatestVerificationReleasedMatchingEvaluationAndAvailability() throws Exception {
        var c=candidate();var s=skill(c);assertThat(eligibility.check(c.getId(),s.getId()).eligible()).isFalse();
        decision(submit(c),user(Role.EVALUATOR),"VERIFIED");var e=evaluation(c,s,Recommendation.HIRE_READY,false);assertThat(passed(c,"HIRE_READY")).isFalse();e.setReleased(true);evaluations.saveAndFlush(e);assertThat(eligibility.canAdmit(c.getId(),s.getId())).isTrue();
        var other=skill(candidate());assertThat(eligibility.canAdmit(c.getId(),other.getId())).isFalse();
        c.setAvailability(Availability.UNAVAILABLE);c=candidates.saveAndFlush(c);assertThat(passed(c,"AVAILABLE")).isFalse();c.setAvailability(Availability.AVAILABLE);c=candidates.saveAndFlush(c);
        evaluation(c,s,Recommendation.NEEDS_TRAINING,true);assertThat(passed(c,"HIRE_READY")).isFalse();evaluation(c,null,Recommendation.HIRE_READY,true);assertThat(passed(c,"HIRE_READY")).isTrue();
        var v=new VerificationRecord();v.setCandidate(c);v.setStatus(VerificationStatus.FLAGGED);verifications.saveAndFlush(v);assertThat(passed(c,"VERIFIED")).isFalse();
        mvc.perform(get("/api/candidates/me/readiness").header("Authorization",auth(c.getUser()))).andExpect(status().isOk()).andExpect(jsonPath("$.eligible").value(false));
        var account=users.findById(c.getUser().getId()).orElseThrow();account.setAccountStatus(AccountStatus.SUSPENDED);users.saveAndFlush(account);assertThat(passed(c,"ACTIVE_ACCOUNT")).isFalse();
        mvc.perform(get("/api/candidates/me/readiness").header("Authorization",auth(c.getUser()))).andExpect(status().isForbidden());
        c.setCandidateType(CandidateType.TECH);c=candidates.saveAndFlush(c);assertThat(passed(c,"TRADE")).isFalse();
    }
    @Test void readinessRejectsDuplicateReservationPlacementAndInactiveSkill() throws Exception {
        var c=candidate();var s=skill(c);decision(submit(c),user(Role.EVALUATOR),"VERIFIED");evaluation(c,null,Recommendation.HIRE_READY,true);assertThat(eligibility.canAdmit(c.getId(),s.getId())).isTrue();
        var entry=new WaitingListEntry();entry.setCandidate(c);entry.setSkill(s);entry=queue.saveAndFlush(entry);assertThat(eligibility.canAdmit(c.getId(),s.getId())).isFalse();assertThat(eligibility.check(c.getId(),s.getId()).eligible()).isTrue();
        entry.setStatus(QueueStatus.RESERVED);entry=queue.saveAndFlush(entry);assertThat(passed(c,"NOT_RESERVED")).isFalse();entry.setStatus(QueueStatus.EXITED);queue.saveAndFlush(entry);
        var employer=new EmployerProfile();employer.setUser(user(Role.EMPLOYER));employer.setCompanyName("Demo");employers.saveAndFlush(employer);
        var p=new Placement();p.setCandidate(c);p.setEmployer(employer);p.setSkill(s);p.setStartDate(LocalDate.now());p=placements.saveAndFlush(p);assertThat(passed(c,"NO_ACTIVE_PLACEMENT")).isFalse();p.setStatus(PlacementStatus.ACTIVE);p=placements.saveAndFlush(p);assertThat(passed(c,"NO_ACTIVE_PLACEMENT")).isFalse();p.setStatus(PlacementStatus.COMPLETED);placements.saveAndFlush(p);assertThat(eligibility.canAdmit(c.getId(),s.getId())).isTrue();
        s.setActive(false);skills.saveAndFlush(s);assertThat(passed(c,"TRADE_SKILL")).isFalse();
    }
}
