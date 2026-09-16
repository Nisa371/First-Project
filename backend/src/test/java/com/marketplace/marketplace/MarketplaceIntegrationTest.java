package com.marketplace.marketplace;

import com.marketplace.candidate.*;
import com.marketplace.user.*;
import com.marketplace.skill.*;
import com.marketplace.assessment.*;
import com.marketplace.verification.*;
import java.util.*;
import java.nio.file.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceIntegrationTest {
    static final Path cvRoot;
    static { try { cvRoot=Files.createTempDirectory("marketplace-cv-test-"); } catch(Exception e) { throw new RuntimeException(e); } }
    @DynamicPropertySource static void props(DynamicPropertyRegistry r) { r.add("app.cv.directory",()->cvRoot.toString()); }
    @AfterAll static void cleanup() throws Exception { try(var files=Files.list(cvRoot)) { for(var p:files.toList()) Files.delete(p); } Files.delete(cvRoot); }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CandidateProfileRepository candidates;
    @Autowired UserRepository users;
    @Autowired SkillRepository skills;
    @Autowired AssessmentRepository assessments;
    @Autowired AssessmentAttemptRepository attempts;
    @Autowired EvaluationRepository evaluations;
    @Autowired VerificationRecordRepository verifications;
    record Account(String token, Long userId, Long candidateId) { String auth() { return "Bearer "+token; } }
    Account register(String role, String track) throws Exception {
        var data=new HashMap<String,Object>(Map.of("email",UUID.randomUUID()+"@example.com","password","SafePass123!",
            "accountType",role,"fullName","Test Candidate","companyName","Test Company"));
        if(track!=null) data.put("candidateType",track);
        var result=mvc.perform(post("/api/auth/register").contentType("application/json").content(mapper.writeValueAsString(data)))
            .andExpect(status().isCreated()).andReturn();
        var tree=mapper.readTree(result.getResponse().getContentAsString());
        Long userId=tree.get("user").get("id").asLong();
        return new Account(tree.get("token").asText(),userId,candidates.findByUserId(userId).map(CandidateProfile::getId).orElse(null));
    }
    String profile(String availability) { return """
        {"fullName":"Updated Candidate","phone":"+880 1700000000","location":"Dhaka","bio":"Building useful products",
        "educationSummary":"BSc CSE","experienceSummary":"Two years of Java","availability":"%s","portfolioUrl":"https://example.com","primaryTradeCategory":"Electrician"}
        """.formatted(availability); }
    void available(Account c) throws Exception { mvc.perform(put("/api/candidates/me").header("Authorization",c.auth()).contentType("application/json").content(profile("AVAILABLE"))).andExpect(status().isOk()); }
    Long skill() { return skills.findByNameIgnoreCase("M4 Java").orElseGet(() -> {
        var s=new Skill(); s.setName("M4 Java"); s.setCategory("TECH"); return skills.saveAndFlush(s);
    }).getId(); }
    void addSkill(Account c) throws Exception { mvc.perform(post("/api/candidates/me/skills").header("Authorization",c.auth()).contentType("application/json").content("{\"skillId\":"+skill()+",\"proficiencyLevel\":\"Experienced\"}")).andExpect(status().isOk()); }
    String jobBody() { return "{\"title\":\"Java Developer\",\"description\":\"Build reliable applications\",\"location\":\"Dhaka\",\"candidateType\":\"TECH\",\"requiredSkillId\":"+skill()+"}"; }
    Long job(Account e) throws Exception {
        var result=mvc.perform(post("/api/jobs").header("Authorization",e.auth()).contentType("application/json").content(jobBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE")).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void profileSkillsValidationAndAuthority() throws Exception {
        var c=register("CANDIDATE","TECH"); var other=register("CANDIDATE","TECH"); var e=register("EMPLOYER",null);
        available(c); addSkill(c);
        mvc.perform(get("/api/candidates/me").header("Authorization",c.auth())).andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Updated Candidate")).andExpect(jsonPath("$.skills[0].name").value("M4 Java"))
            .andExpect(jsonPath("$.primaryTradeCategory").isEmpty()).andExpect(jsonPath("$.cvStoredName").doesNotExist());
        mvc.perform(get("/api/candidates/me").header("Authorization",other.auth())).andExpect(jsonPath("$.fullName").value("Test Candidate"));
        mvc.perform(post("/api/candidates/me/skills").header("Authorization",c.auth()).contentType("application/json").content("{\"skillId\":"+skill()+"}"))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/candidates/me").header("Authorization",c.auth()).contentType("application/json").content(profile("AVAILABLE").replace("https://example.com","javascript:alert(1)")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.portfolioUrl").exists());
        mvc.perform(put("/api/candidates/me").header("Authorization",c.auth()).contentType("application/json").content(profile("AVAILABLE").replace("\"bio\":", "\"verificationStatus\":\"VERIFIED\",\"bio\":")))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/candidates/me").header("Authorization",e.auth())).andExpect(status().isForbidden());
        mvc.perform(get("/api/employers/me").header("Authorization",c.auth())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/candidates/me/skills/"+skill()).header("Authorization",c.auth())).andExpect(status().isOk()).andExpect(jsonPath("$.skills").isEmpty());
        var trade=register("CANDIDATE","TRADE"); available(trade);
        mvc.perform(get("/api/candidates/me").header("Authorization",trade.auth())).andExpect(jsonPath("$.primaryTradeCategory").value("Electrician")).andExpect(jsonPath("$.portfolioUrl").isEmpty());
    }
    @Test void cvTypeSizeOwnershipAndReplacement() throws Exception {
        var c=register("CANDIDATE","TECH"); var other=register("CANDIDATE","TECH"); var trade=register("CANDIDATE","TRADE"); var e=register("EMPLOYER",null);
        byte[] pdf="%PDF-1.4\nexample\n%%EOF".getBytes();
        for(String badName:List.of("../cv.pdf","resume.html","a\\cv.pdf")) {
            mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file",badName,"application/pdf",pdf)).header("Authorization",c.auth())).andExpect(status().isBadRequest());
        }
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","resume.pdf","application/pdf","not a PDF".getBytes())).header("Authorization",c.auth())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","resume.pdf","text/html",pdf)).header("Authorization",c.auth())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","resume.pdf","application/pdf",new byte[5*1024*1024+1])).header("Authorization",c.auth())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","resume.pdf","application/pdf",pdf)).header("Authorization",trade.auth())).andExpect(status().isForbidden());
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","resume.pdf","application/pdf",pdf)).header("Authorization",c.auth())).andExpect(status().isOk());
        var original=candidates.findById(c.candidateId).orElseThrow().getCvStoredName();
        assertThat(original).matches("[a-f0-9-]+\\.pdf"); assertThat(Files.exists(cvRoot.resolve(original))).isTrue();
        mvc.perform(get("/api/candidates/me/cv").header("Authorization",c.auth())).andExpect(status().isOk()).andExpect(content().bytes(pdf)).andExpect(header().string("Cache-Control","no-store"));
        mvc.perform(get("/api/candidates/"+c.candidateId+"/cv").header("Authorization",other.auth())).andExpect(status().isForbidden());
        mvc.perform(get("/api/candidates/"+c.candidateId+"/cv").header("Authorization",e.auth())).andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=resume.pdf"));
        mvc.perform(get("/api/candidates/"+c.candidateId+"/cv")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/candidates/me/cv").file(new MockMultipartFile("file","updated.pdf","application/pdf",pdf)).header("Authorization",c.auth())).andExpect(status().isOk());
        assertThat(Files.exists(cvRoot.resolve(original))).isFalse();
    }
    @Test void employerJobsOwnershipAndShortlistLifecycle() throws Exception {
        var e=register("EMPLOYER",null); var outsider=register("EMPLOYER",null); var c=register("CANDIDATE","TECH");
        mvc.perform(put("/api/employers/me").header("Authorization",e.auth()).contentType("application/json").content("{\"companyName\":\"Dhaka Studio\",\"industry\":\"Software\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.companyName").value("Dhaka Studio"));
        mvc.perform(get("/api/employers/me").header("Authorization",outsider.auth())).andExpect(jsonPath("$.companyName").value("Test Company"));
        Long id=job(e);
        mvc.perform(get("/api/jobs").header("Authorization",outsider.auth())).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/jobs/"+id).header("Authorization",outsider.auth())).andExpect(status().isNotFound());
        mvc.perform(put("/api/jobs/"+id).header("Authorization",outsider.auth()).contentType("application/json").content(jobBody())).andExpect(status().isNotFound());
        mvc.perform(post("/api/jobs/"+id+"/close").header("Authorization",outsider.auth())).andExpect(status().isNotFound());
        mvc.perform(post("/api/jobs").header("Authorization",c.auth()).contentType("application/json").content(jobBody())).andExpect(status().isForbidden());
        String path="/api/jobs/"+id+"/shortlist/"+c.candidateId;
        mvc.perform(post(path).header("Authorization",outsider.auth())).andExpect(status().isNotFound());
        mvc.perform(post(path).header("Authorization",e.auth())).andExpect(status().isConflict());
        available(c);
        mvc.perform(post(path).header("Authorization",e.auth())).andExpect(status().isConflict());
        addSkill(c);
        mvc.perform(post(path).header("Authorization",e.auth())).andExpect(status().isCreated());
        mvc.perform(post(path).header("Authorization",e.auth())).andExpect(status().isConflict());
        mvc.perform(get("/api/jobs/"+id+"/shortlist").header("Authorization",e.auth())).andExpect(jsonPath("$[0].id").value(c.candidateId)).andExpect(jsonPath("$[0].phone").doesNotExist());
        mvc.perform(get("/api/jobs/"+id+"/shortlist").header("Authorization",outsider.auth())).andExpect(status().isNotFound());
        mvc.perform(delete(path).header("Authorization",outsider.auth())).andExpect(status().isNotFound());
        mvc.perform(put("/api/jobs/"+id).header("Authorization",e.auth()).contentType("application/json").content(jobBody().replace("Java Developer","Senior Developer"))).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Senior Developer"));
        mvc.perform(post("/api/jobs/"+id+"/close").header("Authorization",e.auth())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
        mvc.perform(put("/api/jobs/"+id).header("Authorization",e.auth()).contentType("application/json").content(jobBody())).andExpect(status().isConflict());
        mvc.perform(delete(path).header("Authorization",e.auth())).andExpect(status().isNoContent());
        mvc.perform(post(path).header("Authorization",e.auth())).andExpect(status().isConflict());
        mvc.perform(get("/api/jobs/"+id).header("Authorization",e.auth())).andExpect(jsonPath("$.shortlistCount").value(0));
    }
    @Test void searchFiltersAndProjectionNeverLeakPrivateData() throws Exception {
        var c=register("CANDIDATE","TECH"); var e=register("EMPLOYER",null); available(c); addSkill(c);
        var candidate=candidates.findById(c.candidateId).orElseThrow(); String location="Private Test "+UUID.randomUUID(); candidate.setLocation(location); candidates.save(candidate);
        var v=new VerificationRecord(); v.setCandidate(candidate); v.setIdentityReference("PRIVATE_NID"); v.setReviewerNotes("PRIVATE_VERIFICATION_NOTE"); v.setStatus(VerificationStatus.VERIFIED); verifications.save(v);
        var a=new Assessment(); a.setTitle("Assessment"); assessments.save(a);
        for(boolean released:new boolean[]{false,true}) {
            var attempt=new AssessmentAttempt(); attempt.setAssessment(a); attempt.setCandidate(candidate); attempts.save(attempt);
            var evaluation=new Evaluation(); evaluation.setAttempt(attempt); evaluation.setScore(BigDecimal.valueOf(released?88:11)); evaluation.setRecommendation(Recommendation.HIRE_READY); evaluation.setInternalNotes("PRIVATE_EVALUATOR_NOTE"); evaluation.setReleased(released); evaluations.save(evaluation);
        }
        var response=mvc.perform(get("/api/candidates").param("location",location).param("candidateType","TECH").param("availability","AVAILABLE").param("skillId",skill().toString()).header("Authorization",e.auth()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].verificationStatus").value("VERIFIED"))
            .andExpect(jsonPath("$.content[0].releasedResults.length()").value(1)).andExpect(jsonPath("$.content[0].releasedResults[0].score").value(88)).andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("PRIVATE_","password","phone","cvStoredName","identityReference","internalNotes","email");
        mvc.perform(get("/api/candidates").param("location",location).param("candidateType","TRADE").header("Authorization",e.auth())).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/candidates").param("location","%").header("Authorization",e.auth())).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/candidates").param("page","-1").header("Authorization",e.auth())).andExpect(status().isBadRequest());
        mvc.perform(get("/api/candidates").header("Authorization",c.auth())).andExpect(status().isForbidden());
        var user=users.findById(c.userId).orElseThrow(); user.setAccountStatus(AccountStatus.SUSPENDED); users.save(user);
        mvc.perform(get("/api/candidates").param("location",location).header("Authorization",e.auth())).andExpect(jsonPath("$.totalElements").value(0));
    }
}
