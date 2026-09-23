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
import java.time.*;
import com.marketplace.job.*;
import com.marketplace.notification.*;
import com.marketplace.audit.*;
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
class ReplacementEngineIntegrationTest {
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


    @Autowired JobRepository jobs;
    @Autowired ShortlistEntryRepository shortlists;
    @Autowired ReplacementRequestRepository replacements;
    @Autowired NotificationRepository notifications;
    @Autowired AuditLogRepository audits;
    @Autowired org.springframework.context.ApplicationContext context;
    @Autowired org.springframework.transaction.support.TransactionTemplate tx;
    void link(CandidateProfile c, Skill s) { var l=new CandidateSkill();l.setCandidate(c);l.setSkill(s);candidateSkills.saveAndFlush(l); }
    CandidateProfile ready(Skill s) { var c=candidate();link(c,s);var v=new VerificationRecord();v.setCandidate(c);v.setStatus(VerificationStatus.VERIFIED);verifications.saveAndFlush(v);evaluation(c,null,Recommendation.HIRE_READY,true);return c; }
    EmployerProfile employer() { var e=new EmployerProfile();e.setUser(user(Role.EMPLOYER));e.setCompanyName("Managed Demo");return employers.saveAndFlush(e); }
    Placement original(EmployerProfile e,Skill s) { var p=new Placement();p.setCandidate(ready(s));p.setEmployer(e);p.setSkill(s);p.setStartDate(LocalDate.now());p.setStatus(PlacementStatus.ACTIVE);p.setGuaranteeEligible(true);p.setGuaranteeExpiresAt(Instant.now().plusSeconds(86400));return placements.saveAndFlush(p); }
    tools.jackson.databind.JsonNode postOk(String path,User u,String body) throws Exception { var result=mvc.perform(post(path).header("Authorization",auth(u)).contentType("application/json").content(body)).andExpect(status().isOk()).andReturn();var text=result.getResponse().getContentAsString();return text.isEmpty()?null:mapper.readTree(text); }
    Long join(CandidateProfile c,Skill s) throws Exception { return postOk("/api/waiting-list/me",c.getUser(),"{\"skillId\":"+s.getId()+"}").get("id").asLong(); }
    tools.jackson.databind.JsonNode request(Placement p,EmployerProfile e) throws Exception { return postOk("/api/replacements",e.getUser(),"{\"placementId\":"+p.getId()+",\"reason\":\"Worker unavailable\"}"); }
    int requestStatus(Placement p,EmployerProfile e) throws Exception { return mvc.perform(post("/api/replacements").header("Authorization",auth(e.getUser())).contentType("application/json").content("{\"placementId\":"+p.getId()+",\"reason\":\"Unavailable\"}")).andReturn().getResponse().getStatus(); }

    @Test void admissionOwnershipDuplicatePositionAndWithdrawal() throws Exception {
        var s=skill(candidate());var a=ready(s);var b=ready(s);var id=join(a,s);join(b,s);
        mvc.perform(get("/api/waiting-list/me").header("Authorization",auth(b.getUser()))).andExpect(jsonPath("$[0].position").value(2));
        mvc.perform(post("/api/waiting-list/me").header("Authorization",auth(a.getUser())).contentType("application/json").content("{\"skillId\":"+s.getId()+"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/waiting-list/"+id+"/leave").header("Authorization",auth(b.getUser()))).andExpect(status().isNotFound());
        postOk("/api/waiting-list/"+id+"/leave",a.getUser(),"{}");
        mvc.perform(get("/api/waiting-list/me").header("Authorization",auth(b.getUser()))).andExpect(jsonPath("$[0].position").value(1));
        var unready=candidate();mvc.perform(post("/api/waiting-list/me").header("Authorization",auth(unready.getUser())).contentType("application/json").content("{\"skillId\":"+s.getId()+"}")).andExpect(status().isConflict());
        mvc.perform(post("/api/waiting-list/me").header("Authorization",auth(a.getUser())).contentType("application/json").content("{\"skillId\":"+s.getId()+",\"position\":1}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/waiting-list").header("Authorization",auth(a.getUser()))).andExpect(status().isForbidden());
    }
    @Test void hiringLifecycleEnforcesShortlistOwnershipAvailabilityAndServerCoverage() throws Exception {
        var e=employer();var s=skill(candidate());var c=ready(s);var j=new Job();j.setEmployer(e);j.setTitle("Driver");j.setDescription("Demo");j.setLocation("Dhaka");j.setCandidateType(CandidateType.TRADE);j.setStatus(JobStatus.ACTIVE);j.setRequiredSkill(s);j=jobs.saveAndFlush(j);
        String body="{\"jobId\":"+j.getId()+",\"candidateId\":"+c.getId()+"}";
        mvc.perform(post("/api/placements").header("Authorization",auth(e.getUser())).contentType("application/json").content(body)).andExpect(status().isConflict());
        var l=new ShortlistEntry();l.setJob(j);l.setCandidate(c);shortlists.saveAndFlush(l);
        mvc.perform(post("/api/placements").header("Authorization",auth(e.getUser())).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(post("/api/jobs/"+j.getId()+"/apply").header("Authorization",auth(c.getUser()))).andExpect(status().isCreated());
        // A legacy shortlist alone cannot authorize a new placement.
        mvc.perform(post("/api/jobs/"+j.getId()+"/shortlist/"+c.getId()).header("Authorization",auth(e.getUser()))).andExpect(status().isCreated());
        mvc.perform(post("/api/placements").header("Authorization",auth(employer().getUser())).contentType("application/json").content(body)).andExpect(status().isNotFound());
        var p=postOk("/api/placements",e.getUser(),body);assertThat(p.get("status").asText()).isEqualTo("ACTIVE");assertThat(p.get("guaranteeEligible").asBoolean()).isTrue();assertThat(Instant.parse(p.get("guaranteeExpiresAt").asText())).isBetween(Instant.now().plus(Duration.ofDays(29)),Instant.now().plus(Duration.ofDays(31)));
        mvc.perform(post("/api/placements").header("Authorization",auth(e.getUser())).contentType("application/json").content(body)).andExpect(status().isConflict());
        var id=p.get("id").asLong();assertThat(postOk("/api/placements/"+id+"/complete",e.getUser(),"{}").get("status").asText()).isEqualTo("COMPLETED");
        mvc.perform(post("/api/placements/"+id+"/terminate").header("Authorization",auth(e.getUser()))).andExpect(status().isConflict());
        var second=postOk("/api/placements",e.getUser(),body);assertThat(postOk("/api/placements/"+second.get("id").asLong()+"/terminate",e.getUser(),"{}").get("status").asText()).isEqualTo("TERMINATED");
    }
    @Test void fifoSkipsIneligibleReservesActivatesAndObserversCommit() throws Exception {
        var e=employer();var s=skill(candidate());var p=original(e,s);var unavailable=ready(s);var first=ready(s);var second=ready(s);
        join(unavailable,s);var firstId=join(first,s);join(second,s);unavailable.setAvailability(Availability.UNAVAILABLE);candidates.saveAndFlush(unavailable);
        var r=request(p,e);var id=r.get("id").asLong();assertThat(r.get("selectedCandidate").get("id").asLong()).isEqualTo(first.getId());assertThat(queue.findById(firstId).orElseThrow().getStatus()).isEqualTo(QueueStatus.RESERVED);
        assertThat(Duration.between(Instant.parse(r.get("requestedAt").asText()),Instant.parse(r.get("targetCompletionAt").asText()))).isEqualTo(Duration.ofHours(24));
        assertThat(requestStatus(p,e)).isEqualTo(409);
        mvc.perform(post("/api/replacements/"+id+"/complete").header("Authorization",auth(e.getUser()))).andExpect(status().isConflict());
        postOk("/api/replacements/"+id+"/accept",e.getUser(),"{}");var complete=postOk("/api/replacements/"+id+"/complete",e.getUser(),"{}");assertThat(complete.get("slaStatus").asText()).isEqualTo("ON_TIME");assertThat(complete.get("replacementPlacementId").isNumber()).isTrue();
        assertThat(placements.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PlacementStatus.REPLACED);assertThat(queue.findById(firstId).orElseThrow().getStatus()).isEqualTo(QueueStatus.EXITED);
        assertThat(notifications.countByUserIdAndReadAtIsNull(first.getUser().getId())).isGreaterThanOrEqualTo(3);
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc("REPLACEMENT",id).stream().map(AuditLog::getAction)).contains("REPLACEMENT_REQUESTED","REPLACEMENT_SELECTED","REPLACEMENT_COMPLETED");
        assertThat(context.getBean(ReplacementQueueManager.class)).isSameAs(context.getBean(ReplacementQueueManager.class));
        mvc.perform(post("/api/replacements/"+id+"/complete").header("Authorization",auth(e.getUser()))).andExpect(status().isConflict());
    }
    @Test void coverageAuthorizationNotificationPrivacyAndRollback() throws Exception {
        var e=employer();var s=skill(candidate());var p=original(e,s);assertThat(requestStatus(p,employer())).isEqualTo(403);
        p.setGuaranteeExpiresAt(Instant.now().minusSeconds(1));p=placements.saveAndFlush(p);assertThat(requestStatus(p,e)).isEqualTo(409);
        p.setGuaranteeExpiresAt(Instant.now().plusSeconds(86400));p=placements.saveAndFlush(p);var a=ready(s);join(a,s);var r=request(p,e);var id=r.get("id").asLong();
        mvc.perform(get("/api/replacements/"+id).header("Authorization",auth(candidate().getUser()))).andExpect(status().isNotFound());
        mvc.perform(post("/api/replacements/"+id+"/accept").header("Authorization",auth(a.getUser()))).andExpect(status().isForbidden());
        var response=mvc.perform(get("/api/replacements/"+id).header("Authorization",auth(e.getUser()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertThat(response).doesNotContain("identityReference","passwordHash","internalNotes");
        var n=notifications.findByUserIdOrderByCreatedAtDescIdDesc(a.getUser().getId()).getFirst();
        mvc.perform(post("/api/notifications/"+n.getId()+"/read").header("Authorization",auth(e.getUser()))).andExpect(status().isNotFound());postOk("/api/notifications/read-all",a.getUser(),"{}");assertThat(notifications.countByUserIdAndReadAtIsNull(a.getUser().getId())).isZero();
        long before=notifications.count();tx.executeWithoutResult(status->{context.publishEvent(new MarketplaceEvent(e.getUser(),"REPLACEMENT_SELECTED","REPLACEMENT",id,List.of(a.getUser()),"Rollback check","Test"));status.setRollbackOnly();});assertThat(notifications.count()).isEqualTo(before);
    }
    @Test void emptyQueueRetryPreservesClockAndLateCompletionBreaches() throws Exception {
        var e=employer();var s=skill(candidate());var p=original(e,s);var r=request(p,e);var id=r.get("id").asLong();assertThat(r.get("status").asText()).isEqualTo("FAILED");
        // Persist an old request to test a real breached deadline; no client clock/status override exists.
        var old=new ReplacementRequest();old.setPlacement(p);old.setEmployer(e);old.setReason("Late demo");old.setStatus(ReplacementStatus.FAILED);old.setRequestedAt(Instant.now().minus(Duration.ofHours(25)));old.setTargetCompletionAt(Instant.now().minus(Duration.ofHours(1)));old=replacements.saveAndFlush(old);id=old.getId();
        var a=ready(s);join(a,s);var retry=postOk("/api/replacements/"+id+"/retry",e.getUser(),"{}");assertThat(Instant.parse(retry.get("targetCompletionAt").asText())).isBefore(Instant.now());
        postOk("/api/replacements/"+id+"/accept",e.getUser(),"{}");assertThat(postOk("/api/replacements/"+id+"/complete",e.getUser(),"{}").get("slaStatus").asText()).isEqualTo("BREACHED");
    }
    @Test void changedEligibilityRematchesAndCancellationReleasesOriginalPosition() throws Exception {
        var e=employer();var s=skill(candidate());var p=original(e,s);var a=ready(s);var b=ready(s);var aid=join(a,s);var bid=join(b,s);var r=request(p,e);var id=r.get("id").asLong();postOk("/api/replacements/"+id+"/accept",e.getUser(),"{}");
        a.setAvailability(Availability.UNAVAILABLE);candidates.saveAndFlush(a);var next=postOk("/api/replacements/"+id+"/complete",e.getUser(),"{}");assertThat(next.get("status").asText()).isEqualTo("CANDIDATE_SELECTED");assertThat(next.get("selectedCandidate").get("id").asLong()).isEqualTo(b.getId());assertThat(next.get("targetCompletionAt")).isEqualTo(r.get("targetCompletionAt"));assertThat(queue.findById(aid).orElseThrow().getStatus()).isEqualTo(QueueStatus.EXITED);
        var joined=queue.findById(bid).orElseThrow().getJoinedAt();postOk("/api/replacements/"+id+"/cancel",e.getUser(),"{}");assertThat(queue.findById(bid).orElseThrow().getStatus()).isEqualTo(QueueStatus.QUEUED);assertThat(queue.findById(bid).orElseThrow().getJoinedAt()).isEqualTo(joined);
    }
    @Test void fifoTiesUseStableIdNotAssessmentScore() throws Exception {
        var e=employer();var s=skill(candidate());var p=original(e,s);var a=ready(s);var b=ready(s);
        var joined=Instant.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        for(var c:List.of(a,b)) {var entry=new WaitingListEntry();entry.setCandidate(c);entry.setSkill(s);entry.setJoinedAt(joined);queue.saveAndFlush(entry);}
        assertThat(request(p,e).get("selectedCandidate").get("id").asLong()).isEqualTo(a.getId());
    }
    @Test void concurrentDuplicateRequestsHaveOneWinner() throws Exception {
        var e=employer();var s=skill(candidate());var p=original(e,s);join(ready(s),s);
        try(var pool=Executors.newFixedThreadPool(2)) { var gate=new CountDownLatch(1);var a=pool.submit(()->{gate.await();return requestStatus(p,e);});var b=pool.submit(()->{gate.await();return requestStatus(p,e);});gate.countDown();assertThat(List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409); }
    }
    @Test void simultaneousCrossSkillMatchingNeverDoubleReservesCandidate() throws Exception {
        var e=employer();var s=skill(candidate());var t=skill(candidate());var p=original(e,s);var q=original(e,t);var a=ready(s);link(a,t);join(a,s);join(a,t);
        try(var pool=Executors.newFixedThreadPool(2)) { var gate=new CountDownLatch(1);var first=pool.submit(()->{gate.await();return request(p,e).get("status").asText();});var second=pool.submit(()->{gate.await();return request(q,e).get("status").asText();});gate.countDown();assertThat(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder("CANDIDATE_SELECTED","FAILED"); }
        assertThat(queue.findByCandidateIdAndStatusIn(a.getId(),List.of(QueueStatus.RESERVED))).hasSize(1);
    }
}
