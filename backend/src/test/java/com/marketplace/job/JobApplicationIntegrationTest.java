package com.marketplace.job;
import com.marketplace.auth.JwtService;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.user.*;
import com.marketplace.verification.*;
import com.marketplace.notification.*;
import static org.mockito.Mockito.*;
import static org.awaitility.Awaitility.await;
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
    @Autowired VerificationChecklist checklist;
    @Autowired VerificationRecordRepository records;
    @Autowired com.marketplace.companytype.CompanyTypeRepository types;
    @Autowired NotificationRepository notices;
    @Autowired CandidateCvRepository cvs;
    @Autowired com.marketplace.interview.AssessmentSessionRepository sessions;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.marketplace.ai.AiEvaluationProvider provider;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    NotificationService notificationService;
    void verification(User u, VerificationStatus status) {
        records.deleteAll(records.forUser(u.getId()));
        if(status==null) return;
        for(var requirement:checklist.applicable(u)) {
            var r=new VerificationRecord();r.setOwner(u);r.setRequirement(requirement);r.setStatus(status);
            if(u.getRole()==Role.CANDIDATE) r.setCandidate(candidates.findByUserId(u.getId()).orElseThrow());
            records.saveAndFlush(r);
        }
    }
    User user(Role role) { var u=new User();u.setRole(role);u.setEmail(UUID.randomUUID()+"@example.test");u.setPasswordHash("test-only");return users.saveAndFlush(u); }
    User employer() { var u=user(Role.EMPLOYER);var e=new EmployerProfile();e.setUser(u);e.setCompanyName("Application Company");e.setCompanyType(types.findByCode("OTHER").orElseThrow());employers.saveAndFlush(e);verification(u,VerificationStatus.VERIFIED);return u; }
    User candidate() { var u=user(Role.CANDIDATE);var c=new CandidateProfile();c.setUser(u);c.setFullName("Applicant");c.setCandidateType(CandidateType.TECH);c.setAvailability(Availability.AVAILABLE);candidates.saveAndFlush(c);verification(u,VerificationStatus.VERIFIED);return u; }
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
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"NONE","PENDING","FAILED"})
    void nonVerifiedUsersCannotPublishOrApply(String state) throws Exception {
        var e=employer();var c=candidate();Long active=job(e);
        Long draft=id(mvc.perform(post("/api/jobs").header("Authorization",auth(e)).contentType("application/json").content(body(6))).andExpect(status().isCreated()));
        Long payment=id(mvc.perform(post("/api/jobs/"+draft+"/payment").header("Authorization",auth(e))).andExpect(status().isOk()));
        var status=state.equals("NONE")?null:VerificationStatus.valueOf(state);
        verification(e,status);verification(c,status);
        mvc.perform(post("/api/jobs/"+draft+"/payment").header("Authorization",auth(e))).andExpect(status().isForbidden());
        mvc.perform(post("/api/payments/"+payment+"/demo-success").header("Authorization",auth(e))).andExpect(status().isForbidden());
        apply(c,active).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("VERIFICATION_REQUIRED"));
        assertThat(applications.countByJobId(active)).isZero();verifyNoInteractions(provider);
        assertThat(notices.findByUserIdOrderByCreatedAtDescIdDesc(e.getId())).isEmpty();
        assertThat(notices.findByUserIdOrderByCreatedAtDescIdDesc(c.getId())).isEmpty();
        mvc.perform(get("/api/jobs/"+active).header("Authorization",auth(e))).andExpect(status().isOk());
        mvc.perform(get("/api/candidates/me/jobs/"+active).header("Authorization",auth(c))).andExpect(status().isOk());
        assertThat(jobs.findById(draft).orElseThrow().getStatus()).isEqualTo(JobStatus.DRAFT);
    }
    @Test void unavailableOnlyBlocksNewApplicationsAndStandaloneAssessmentIsForbidden() throws Exception {
        var e=employer();var c=candidate();Long j=job(e),next=job(e);Long a=id(apply(c,j).andExpect(status().isCreated()));
        mvc.perform(put("/api/candidates/me").header("Authorization",auth(c)).contentType("application/json")
            .content("{\"fullName\":\"Updated applicant\",\"availability\":\"UNAVAILABLE\"}")).andExpect(status().isOk());
        apply(c,next).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("CANDIDATE_UNAVAILABLE"));
        assertThat(applications.countByJobId(next)).isZero();
        assertThat(applications.findById(a).orElseThrow().getStatus()).isEqualTo(ApplicationStatus.APPLIED);
        mvc.perform(get("/api/candidates/me/jobs").header("Authorization",auth(c))).andExpect(status().isOk());
        mvc.perform(get("/api/candidates/me/applications").header("Authorization",auth(c))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(post("/api/assessments/1/attempts").header("Authorization",auth(c))).andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("APPLICATION_REQUIRED"));
        assertThat(notices.findByUserIdOrderByCreatedAtDescIdDesc(e.getId())).hasSize(1);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"true,true,false","true,false,false","false,true,false","false,false,false","true,true,true"})
    void committedApplicationsEvaluateOnlyPresentInputsAndSurviveProviderFailure(boolean hasCv,boolean hasPortfolio,boolean fails) throws Exception {
        var e=employer();var u=candidate();var c=candidates.findByUserId(u.getId()).orElseThrow();Long j=job(e);
        if(hasCv) {var cv=new CandidateCv();cv.setCandidate(c);cv.setSummary("Backend developer");cvs.saveAndFlush(cv);}
        if(hasPortfolio) {c.setPortfolioUrl("https://example.test/portfolio");candidates.saveAndFlush(c);}
        when(provider.evaluateCv(any())).thenAnswer(call -> {
            assertThat(applications.findByJobIdAndCandidateId(j,c.getId())).isPresent();
            if(fails) throw new IllegalStateException("Mock provider 503/retry exhaustion");
            return new com.marketplace.ai.AiEvaluationDtos.CvEvaluationResult(0.8);
        });
        when(provider.evaluatePortfolio(any())).thenAnswer(call -> {
            assertThat(applications.findByJobIdAndCandidateId(j,c.getId())).isPresent();
            if(fails) throw new IllegalStateException("Mock provider 503/retry exhaustion");
            return new com.marketplace.ai.AiEvaluationDtos.PortfolioEvaluationResult(0.6);
        });
        Long a=id(apply(u,j).andExpect(status().isCreated()));
        await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() -> {
            var saved=applications.findById(a).orElseThrow();
            assertThat(saved.getCvAttempt()).isNotNull();assertThat(saved.getCvAttempt().getAttemptedAt()).isNotNull();
            assertThat(saved.getPortfolioAttempt()).isNotNull();assertThat(saved.getPortfolioAttempt().getAttemptedAt()).isNotNull();
        });
        var saved=applications.findById(a).orElseThrow();
        if(hasCv && !fails) assertThat(saved.getCvScore()).isEqualByComparingTo("0.8");else assertThat(saved.getCvScore()).isNull();
        if(hasPortfolio && !fails) assertThat(saved.getPortfolioScore()).isEqualByComparingTo("0.6");else assertThat(saved.getPortfolioScore()).isNull();
        verify(provider,times(hasCv?1:0)).evaluateCv(any());verify(provider,times(hasPortfolio?1:0)).evaluatePortfolio(any());
        apply(u,j).andExpect(status().isConflict());
        mvc.perform(get("/api/candidates/me/applications").header("Authorization",auth(u))).andExpect(jsonPath("$[0].id").value(a));
        mvc.perform(get("/api/jobs/"+j+"/applications").header("Authorization",auth(e))).andExpect(jsonPath("$[0].id").value(a));
        assertThat(applications.countByJobId(j)).isEqualTo(1);assertThat(sessions.findByJobApplicationId(a)).isEmpty();
        assertThat(notices.findByUserIdOrderByCreatedAtDescIdDesc(e.getId())).singleElement().satisfies(n -> assertThat(n.getTitle()).isEqualTo("New application received"));
        assertThat(notices.findByUserIdOrderByCreatedAtDescIdDesc(u.getId())).singleElement().satisfies(n -> assertThat(n.getTitle()).isEqualTo("Assessment available"));
    }
    @Test void notificationsArePersistentOrderedOwnedAndOnlyRealStatusChangesNotify() throws Exception {
        var e=employer();var c=candidate();var outsider=candidate();Long j=job(e),a=id(apply(c,j).andExpect(status().isCreated()));
        reviewStatus(e,j,a,"APPLIED").andExpect(status().isOk());
        assertThat(notices.findByUserIdOrderByCreatedAtDescIdDesc(c.getId())).hasSize(1);
        reviewStatus(e,j,a,"UNDER_REVIEW").andExpect(status().isOk());reviewStatus(e,j,a,"UNDER_REVIEW").andExpect(status().isOk());
        var candidateNotices=notices.findByUserIdOrderByCreatedAtDescIdDesc(c.getId());assertThat(candidateNotices).hasSize(2);
        Long employerNotice=notices.findByUserIdOrderByCreatedAtDescIdDesc(e.getId()).getFirst().getId();
        for(int i=0;i<2;i++) {
            mvc.perform(get("/api/notifications/me").header("Authorization",auth(c))).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].title").value("Application status updated"));
            mvc.perform(get("/api/notifications/me").header("Authorization",auth(e))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(employerNotice));
        }
        mvc.perform(get("/api/notifications/me").header("Authorization",auth(outsider))).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/notifications/"+employerNotice+"/read").header("Authorization",auth(c))).andExpect(status().isNotFound());
        mvc.perform(post("/api/notifications/"+candidateNotices.getFirst().getId()+"/read").header("Authorization",auth(e))).andExpect(status().isNotFound());
        mvc.perform(post("/api/notifications/"+employerNotice+"/read").header("Authorization",auth(e))).andExpect(status().isOk());
        assertThat(notices.countByUserIdAndReadAtIsNull(e.getId())).isZero();
        mvc.perform(post("/api/notifications/read-all").header("Authorization",auth(c))).andExpect(status().isOk());
        mvc.perform(get("/api/notifications/me").header("Authorization",auth(c))).andExpect(jsonPath("$[0].readAt").isNotEmpty()).andExpect(jsonPath("$[1].readAt").isNotEmpty());
    }
    @Test void notificationDeliveryFailureDoesNotDestroyApplication() throws Exception {
        var e=employer();var c=candidate();Long j=job(e);
        doThrow(new IllegalStateException("Mock delivery failure")).when(notificationService).deliver(any());
        Long a=id(apply(c,j).andExpect(status().isCreated()));
        assertThat(applications.findById(a)).isPresent();
        mvc.perform(get("/api/jobs/"+j+"/applications").header("Authorization",auth(e))).andExpect(jsonPath("$[0].id").value(a));
    }

    @Test void verificationDecisionsNotifyCandidateAndEmployerWithoutDocumentContents() throws Exception {
        var admin=user(Role.ADMIN);
        for(var owner:List.of(candidate(),employer())) {
            for(var decision:List.of("VERIFIED","FAILED")) {
                verification(owner,VerificationStatus.PENDING);
                var record=records.forUser(owner.getId()).getFirst();
                mvc.perform(post("/api/admin/verification-submissions/"+record.getId()+"/review").header("Authorization",auth(admin))
                    .contentType("application/json").content("{\"status\":\""+decision+"\",\"notes\":\"PRIVATE_REVIEW_NOTE\"}")).andExpect(status().isOk());
                var latest=notices.findByUserIdOrderByCreatedAtDescIdDesc(owner.getId()).getFirst();
                assertThat(latest.getTitle()).isEqualTo("Verification result");
                assertThat(latest.getMessage()).contains(decision.equals("VERIFIED")?"approved":"rejected").doesNotContain("PRIVATE_REVIEW_NOTE");
            }
            assertThat(notices.findByUserIdOrderByCreatedAtDescIdDesc(owner.getId())).hasSize(2);
        }
    }

}
