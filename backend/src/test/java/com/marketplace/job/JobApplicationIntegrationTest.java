package com.marketplace.job;
import com.marketplace.auth.JwtService;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.user.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class JobApplicationIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JwtService jwt;
    @Autowired UserRepository users; @Autowired EmployerProfileRepository employers; @Autowired CandidateProfileRepository candidates;
    @Autowired JobRepository jobs; @Autowired JobApplicationRepository applications; @Autowired ShortlistEntryRepository shortlists;
    User user(Role role) { var u=new User();u.setRole(role);u.setEmail(UUID.randomUUID()+"@example.test");u.setPasswordHash("test-only");return users.saveAndFlush(u); }
    User employer() { var u=user(Role.EMPLOYER);var e=new EmployerProfile();e.setUser(u);e.setCompanyName("Application Company");employers.saveAndFlush(e);return u; }
    User candidate() { var u=user(Role.CANDIDATE);var c=new CandidateProfile();c.setUser(u);c.setFullName("Applicant");c.setCandidateType(CandidateType.TECH);c.setAvailability(Availability.AVAILABLE);candidates.saveAndFlush(c);return u; }
    Long cid(User u) { return candidates.findByUserId(u.getId()).orElseThrow().getId(); }
    String auth(User u) { return "Bearer "+jwt.issue(u.getId()); }
    String body(int months) throws Exception { return mapper.writeValueAsString(Map.of("title","Java role","description","Build useful services","location","Dhaka","candidateType","TECH","publicExpectations","Communicate clearly","privateExpectations","PRIVATE_EXPECTATIONS","expectedExperienceMonths",months)); }
    Long id(ResultActions r) throws Exception { return mapper.readTree(r.andReturn().getResponse().getContentAsString()).get("id").asLong(); }
    Long job(User e) throws Exception { Long jobId=id(mvc.perform(post("/api/jobs").header("Authorization",auth(e)).contentType("application/json").content(body(6))).andExpect(status().isCreated()).andExpect(jsonPath("$.privateExpectations").value("PRIVATE_EXPECTATIONS")));
        Long paymentId=id(mvc.perform(post("/api/jobs/"+jobId+"/payment").header("Authorization",auth(e))).andExpect(status().isOk()));
        mvc.perform(post("/api/payments/"+paymentId+"/demo-success").header("Authorization",auth(e))).andExpect(status().isOk());
        return jobId; }
    ResultActions apply(User c,Long job) throws Exception { return mvc.perform(post("/api/jobs/"+job+"/apply").header("Authorization",auth(c))); }
    ResultActions reviewStatus(User e,Long job,Long a,String status) throws Exception { return mvc.perform(patch("/api/jobs/"+job+"/applications/"+a+"/status").header("Authorization",auth(e)).contentType("application/json").content(mapper.writeValueAsString(Map.of("status",status)))); }
    @Test void applicationsAreExplicitUniqueAndCandidateOwned() throws Exception {
        var e=employer();var c=candidate();var other=candidate();Long j=job(e);
        Long a=id(apply(c,j).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("APPLIED")));
        apply(c,j).andExpect(status().isConflict()).andExpect(jsonPath("$.error").value("ALREADY_APPLIED"));
        for(var u:List.of(e,user(Role.ADMIN),user(Role.EVALUATOR))) apply(u,j).andExpect(status().isForbidden());
        mvc.perform(post("/api/jobs/"+j+"/apply")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/candidates/me/applications").header("Authorization",auth(c))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(a));
        mvc.perform(get("/api/candidates/me/applications").header("Authorization",auth(other))).andExpect(jsonPath("$").isEmpty());
        mvc.perform(patch("/api/applications/"+a+"/withdraw").header("Authorization",auth(other))).andExpect(status().isNotFound());
        mvc.perform(get("/api/jobs/"+j+"/applications/"+a).header("Authorization",auth(other))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/applications/"+a+"/withdraw").header("Authorization",auth(e))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/applications/"+a+"/withdraw").header("Authorization",auth(c))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("WITHDRAWN"));
        apply(c,j).andExpect(status().isConflict());reviewStatus(e,j,a,"UNDER_REVIEW").andExpect(status().isConflict());
        assertThat(applications.findByJobIdAndCandidateId(j,cid(c)).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.WITHDRAWN);
        Long closed=job(e);mvc.perform(post("/api/jobs/"+closed+"/close").header("Authorization",auth(e))).andExpect(status().isOk());apply(c,closed).andExpect(status().isConflict());
        var draft=jobs.findById(job(e)).orElseThrow();draft.setStatus(JobStatus.DRAFT);jobs.saveAndFlush(draft);apply(c,draft.getId()).andExpect(status().isConflict());
    }
    @Test void employerVisibilityAndReviewAreScopedToOwnedJobs() throws Exception {
        var e=employer();var outsider=employer();var c=candidate();var stranger=candidate();Long j=job(e), foreign=job(outsider);
        mvc.perform(get("/api/candidates").header("Authorization",auth(e))).andExpect(status().isForbidden());
        mvc.perform(get("/api/candidates/"+cid(c)).header("Authorization",auth(e))).andExpect(status().isNotFound());
        Long a=id(apply(c,j).andExpect(status().isCreated()));
        mvc.perform(get("/api/jobs/"+j+"/applications").header("Authorization",auth(e))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].candidate.id").value(cid(c)));
        mvc.perform(get("/api/jobs/"+j+"/applications").header("Authorization",auth(outsider))).andExpect(status().isNotFound());
        mvc.perform(get("/api/jobs/"+foreign+"/applications/"+a).header("Authorization",auth(outsider))).andExpect(status().isNotFound());
        mvc.perform(get("/api/jobs/"+j+"/applications/"+a).header("Authorization",auth(e))).andExpect(status().isOk());
        mvc.perform(get("/api/candidates/"+cid(c)).header("Authorization",auth(e))).andExpect(status().isOk());
        mvc.perform(get("/api/candidates/"+cid(stranger)).header("Authorization",auth(e))).andExpect(status().isNotFound());
        mvc.perform(get("/api/candidates/"+cid(c)).header("Authorization",auth(outsider))).andExpect(status().isNotFound());
        reviewStatus(c,j,a,"SHORTLISTED").andExpect(status().isForbidden());reviewStatus(outsider,j,a,"REJECTED").andExpect(status().isNotFound());reviewStatus(outsider,foreign,a,"REJECTED").andExpect(status().isNotFound());
        reviewStatus(e,j,a,"WITHDRAWN").andExpect(status().isBadRequest());
        reviewStatus(e,j,a,"UNDER_REVIEW").andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
        reviewStatus(e,j,a,"SHORTLISTED").andExpect(status().isOk());assertThat(shortlists.existsByJobIdAndCandidateId(j,cid(c))).isTrue();
        reviewStatus(e,j,a,"REJECTED").andExpect(status().isOk());assertThat(shortlists.existsByJobIdAndCandidateId(j,cid(c))).isFalse();
        mvc.perform(post("/api/jobs/"+j+"/shortlist/"+cid(stranger)).header("Authorization",auth(e))).andExpect(status().isConflict());
        mvc.perform(post("/api/jobs/"+j+"/shortlist/"+cid(c)).header("Authorization",auth(e))).andExpect(status().isCreated());
        mvc.perform(patch("/api/applications/"+a+"/withdraw").header("Authorization",auth(c))).andExpect(status().isOk());assertThat(shortlists.existsByJobIdAndCandidateId(j,cid(c))).isFalse();
    }
    @Test void privateExpectationsNeverReachCandidateResponsesAndMonthsAreValidated() throws Exception {
        var e=employer();var c=candidate();Long j=job(e);
        for(String path:List.of("/api/candidates/me/jobs", "/api/candidates/me/jobs/"+j)) {
            String response=mvc.perform(get(path).header("Authorization",auth(c))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(response).contains("Communicate clearly","expectedExperienceMonths").doesNotContain("privateExpectations","PRIVATE_EXPECTATIONS");
        }
        String application=apply(c,j).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();assertThat(application).doesNotContain("privateExpectations","PRIVATE_EXPECTATIONS");
        String mine=mvc.perform(get("/api/candidates/me/applications").header("Authorization",auth(c))).andReturn().getResponse().getContentAsString();assertThat(mine).doesNotContain("privateExpectations","PRIVATE_EXPECTATIONS");
        mvc.perform(get("/api/jobs/"+j).header("Authorization",auth(e))).andExpect(jsonPath("$.privateExpectations").value("PRIVATE_EXPECTATIONS"));
        mvc.perform(get("/api/jobs/"+j).header("Authorization",auth(c))).andExpect(status().isForbidden());
        mvc.perform(put("/api/jobs/"+j).header("Authorization",auth(c)).contentType("application/json").content(body(0))).andExpect(status().isForbidden());
        mvc.perform(post("/api/jobs").header("Authorization",auth(e)).contentType("application/json").content(body(-1))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.expectedExperienceMonths").exists());
        mvc.perform(put("/api/jobs/"+j).header("Authorization",auth(e)).contentType("application/json").content(body(-1))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/jobs/"+j).header("Authorization",auth(e)).contentType("application/json").content(body(0))).andExpect(status().isOk()).andExpect(jsonPath("$.expectedExperienceMonths").value(0));
        for(String months:List.of("-1","1.5")) mvc.perform(put("/api/candidates/me").header("Authorization",auth(c)).contentType("application/json").content("{\"fullName\":\"Applicant\",\"availability\":\"AVAILABLE\",\"totalExperienceMonths\":"+months+"}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/candidates/me").header("Authorization",auth(c))).andExpect(jsonPath("$.totalExperienceMonths").value(0));
        mvc.perform(put("/api/candidates/me").header("Authorization",auth(c)).contentType("application/json").content("{\"fullName\":\"Applicant\",\"availability\":\"AVAILABLE\",\"experienceSummary\":\"Descriptive experience stays\",\"totalExperienceMonths\":18}")).andExpect(status().isOk()).andExpect(jsonPath("$.totalExperienceMonths").value(18)).andExpect(jsonPath("$.experienceSummary").value("Descriptive experience stays"));
        mvc.perform(get("/api/jobs/"+j+"/applications").header("Authorization",auth(e))).andExpect(jsonPath("$[0].candidate.totalExperienceMonths").value(18));
    }
    @Test void legacyShortlistsDoNotCreateApplicationsOrRevealProfiles() throws Exception {
        var e=employer();var c=candidate();Long j=job(e);var s=new ShortlistEntry();s.setJob(jobs.findById(j).orElseThrow());s.setCandidate(candidates.findById(cid(c)).orElseThrow());shortlists.saveAndFlush(s);
        mvc.perform(get("/api/jobs/"+j+"/shortlist").header("Authorization",auth(e))).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/jobs/"+j).header("Authorization",auth(e))).andExpect(jsonPath("$.shortlistCount").value(0));
        mvc.perform(get("/api/candidates/"+cid(c)).header("Authorization",auth(e))).andExpect(status().isNotFound());
        assertThat(shortlists.findById(s.getId())).isPresent();assertThat(applications.existsByJobIdAndCandidateId(j,cid(c))).isFalse();
    }
    @Test void employerCannotInvalidateExistingApplicantsByChangingTrackOrSkill() throws Exception {
        var e=employer(); var c=candidate(); Long j=job(e);
        Long a=id(apply(c,j).andExpect(status().isCreated()));
        reviewStatus(e,j,a,"SHORTLISTED").andExpect(status().isOk());
        mvc.perform(put("/api/jobs/"+j).header("Authorization",auth(e)).contentType("application/json")
            .content(body(6).replace("TECH","TRADE"))).andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("JOB_HAS_APPLICATIONS"));
        var changed=mapper.readTree(body(6));
        ((tools.jackson.databind.node.ObjectNode)changed).put("requiredSkillId",999999L);
        mvc.perform(put("/api/jobs/"+j).header("Authorization",auth(e)).contentType("application/json")
            .content(mapper.writeValueAsString(changed))).andExpect(status().isConflict());
        mvc.perform(put("/api/jobs/"+j).header("Authorization",auth(e)).contentType("application/json")
            .content(body(12))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.expectedExperienceMonths").value(12));
        assertThat(shortlists.existsByJobIdAndCandidateId(j,cid(c))).isTrue();
        assertThat(jobs.findById(j).orElseThrow().getCandidateType()).isEqualTo(CandidateType.TECH);
    }
    @Test void concurrentApplyAndDatabaseUniqueConstraintPreventDuplicates() throws Exception {
        var e=employer();var c=candidate();Long j=job(e);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var gate=new CountDownLatch(1);var futures=new ArrayList<Future<Integer>>();
            for(int i=0;i<2;i++) futures.add(pool.submit(()->{gate.await();return apply(c,j).andReturn().getResponse().getStatus();}));
            gate.countDown();assertThat(List.of(futures.get(0).get(15,TimeUnit.SECONDS),futures.get(1).get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        var duplicate=new JobApplication();duplicate.setJob(jobs.findById(j).orElseThrow());duplicate.setCandidate(candidates.findById(cid(c)).orElseThrow());
        assertThatThrownBy(()->applications.saveAndFlush(duplicate)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(applications.countByJobId(j)).isEqualTo(1);
    }
}
