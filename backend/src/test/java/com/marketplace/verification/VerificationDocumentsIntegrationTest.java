package com.marketplace.verification;

import com.marketplace.auth.JwtService;
import com.marketplace.candidate.*;
import com.marketplace.companytype.*;
import com.marketplace.employer.*;
import com.marketplace.user.*;
import java.util.*;
import java.nio.file.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc @Transactional
class VerificationDocumentsIntegrationTest {
    static final Path storage;
    static { try { storage=Files.createTempDirectory("verification-test-"); } catch(Exception e) { throw new RuntimeException(e); } }
    @DynamicPropertySource static void config(DynamicPropertyRegistry r) { r.add("app.verification.directory",()->storage.toString()); }
    @AfterAll static void cleanup() throws Exception { try(var paths=Files.list(storage)) { for(var p:paths.toList()) Files.delete(p); } Files.delete(storage); }
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JwtService jwt;
    @Autowired UserRepository users; @Autowired CandidateProfileRepository candidates; @Autowired EmployerProfileRepository employers;
    @Autowired CompanyTypeRepository types; @Autowired VerificationRequirementRepository requirements; @Autowired VerificationRecordRepository records;
    @Autowired VerificationCatalog catalog; @Autowired VerificationChecklist checklist;
    User account(Role role) {
        var u=new User();u.setEmail(UUID.randomUUID()+"@example.test");u.setPasswordHash("test-only");u.setRole(role);return users.saveAndFlush(u);
    }
    User candidate() { var u=account(Role.CANDIDATE);var c=new CandidateProfile();c.setUser(u);c.setFullName("Candidate");c.setCandidateType(CandidateType.TECH);candidates.saveAndFlush(c);return u; }
    User employer(String key) { var u=account(Role.EMPLOYER);var e=new EmployerProfile();e.setUser(u);e.setCompanyName("Employer");e.setCompanyType(types.findByNormalizedName(key).orElseThrow());employers.saveAndFlush(e);return u; }
    String auth(User u) { return "Bearer "+jwt.issue(u.getId()); }
    Long requirement(String code) { return requirements.findByCode(code).orElseThrow().getId(); }
    ResultActions own(User u) throws Exception { return mvc.perform(get("/api/verifications/me/checklist").header("Authorization",auth(u))); }
    MockMultipartFile file(String name,String mime,byte[] bytes) { return new MockMultipartFile("file",name,mime,bytes); }
    MockMultipartFile pdf() { return file("evidence.pdf","application/pdf","%PDF-1.4 fictional evidence".getBytes()); }
    ResultActions upload(User u,Long requirement,MockMultipartFile file) throws Exception { return mvc.perform(multipart("/api/verifications/me/requirements/"+requirement+"/document").file(file).header("Authorization",auth(u))); }
    Long submit(User u,String code) throws Exception { return id(upload(u,requirement(code),pdf()).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))); }
    Long id(ResultActions result) throws Exception { return mapper.readTree(result.andReturn().getResponse().getContentAsString()).get("id").asLong(); }
    ResultActions review(User admin,Long id,String status,String note) throws Exception { return mvc.perform(post("/api/admin/verification-submissions/"+id+"/review").header("Authorization",auth(admin)).contentType("application/json").content(mapper.writeValueAsString(Map.of("status",status,"notes",note)))); }
    String input(String name,String target,Long type,boolean required,boolean active) throws Exception { var b=new HashMap<String,Object>(Map.of("name",name,"targetType",target,"required",required,"active",active));b.put("companyTypeId",type);return mapper.writeValueAsString(b); }
    Long extra(User admin,String name,String target,Long type,boolean required) throws Exception { return id(mvc.perform(post("/api/admin/verification-requirements").header("Authorization",auth(admin)).contentType("application/json").content(input(name,target,type,required,true))).andExpect(status().isCreated())); }
    @Test void baselinesAndHouseholdIdentityAreStable() throws Exception {
        var c=candidate();var household=employer("household / personal employer");var company=employer("pharmaceuticals");
        own(c).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].requirement.name").value("National ID")).andExpect(jsonPath("$.status").value("NOT_STARTED"));
        own(household).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.items[0].requirement.targetType").value("HOUSEHOLD_EMPLOYER"));
        own(company).andExpect(jsonPath("$.items.length()").value(2)).andExpect(jsonPath("$.items[*].requirement.name",org.hamcrest.Matchers.containsInAnyOrder("Trade License","Tax Document")));
        var type=types.findByCode("HOUSEHOLD").orElseThrow();type.setName("Personal hiring");types.saveAndFlush(type);
        own(household).andExpect(jsonPath("$.items.length()").value(1));
    }
    @Test void targetingTypeChangesAndInactiveRequirementsRecalculateWithoutDeletingHistory() throws Exception {
        var admin=account(Role.ADMIN);var company=employer("pharmaceuticals");var other=employer("education");var c=candidate();
        Long type=types.findByNormalizedName("pharmaceuticals").orElseThrow().getId();
        Long drug=extra(admin,"Drug License","EMPLOYER",type,true);
        extra(admin,"Representative ID","EMPLOYER",null,true);
        extra(admin,"Academic Certificate","CANDIDATE",null,true);
        extra(admin,"Household evidence","HOUSEHOLD_EMPLOYER",null,true);
        own(company).andExpect(jsonPath("$.items.length()").value(4));own(other).andExpect(jsonPath("$.items.length()").value(3));own(c).andExpect(jsonPath("$.items.length()").value(2));
        upload(other,drug,pdf()).andExpect(status().isBadRequest());
        Long document=id(upload(company,drug,pdf()).andExpect(status().isCreated()));
        mvc.perform(put("/api/employers/me").header("Authorization",auth(company)).contentType("application/json").content(mapper.writeValueAsString(Map.of("companyName","Employer","companyTypeId",types.findByCode("HOUSEHOLD").orElseThrow().getId())))).andExpect(status().isOk());
        own(company).andExpect(jsonPath("$.items.length()").value(3)).andExpect(jsonPath("$.history.length()").value(1));
        review(admin,document,"VERIFIED","").andExpect(status().isConflict());
        mvc.perform(get("/api/verifications/documents/"+document).header("Authorization",auth(company))).andExpect(status().isOk());
        mvc.perform(put("/api/admin/verification-requirements/"+drug).header("Authorization",auth(admin)).contentType("application/json").content(input("Drug License","EMPLOYER",type,true,false))).andExpect(status().isOk());
        assertThat(records.findById(document)).isPresent();
        mvc.perform(delete("/api/admin/company-types/"+type).header("Authorization",auth(admin))).andExpect(status().isNoContent());
        assertThat(types.findById(type)).isPresent();
    }
    @Test void reviewResubmissionAndOverallStateRequireEveryRequiredApproval() throws Exception {
        var company=employer("pharmaceuticals");var admin=account(Role.ADMIN);
        Long first=submit(company,"TRADE_LICENSE");own(company).andExpect(jsonPath("$.status").value("INCOMPLETE"));
        Long tax=submit(company,"TAX_DOCUMENT");own(company).andExpect(jsonPath("$.status").value("PENDING"));
        review(admin,first,"VERIFIED","").andExpect(status().isOk()).andExpect(jsonPath("$.reviewerId").value(admin.getId())).andExpect(jsonPath("$.reviewedAt").isNotEmpty());
        own(company).andExpect(jsonPath("$.status").value("PENDING"));
        review(admin,tax,"FAILED","").andExpect(status().isBadRequest());
        review(admin,tax,"FAILED","Please upload a legible tax document.").andExpect(status().isOk());
        own(company).andExpect(jsonPath("$.status").value("REJECTED")).andExpect(jsonPath("$.items[1].submission.reviewNote").value("Please upload a legible tax document."));
        Long replacement=submit(company,"TAX_DOCUMENT");own(company).andExpect(jsonPath("$.history.length()").value(3)).andExpect(jsonPath("$.status").value("PENDING"));
        review(admin,replacement,"VERIFIED","Accepted").andExpect(status().isOk());own(company).andExpect(jsonPath("$.status").value("VERIFIED"));
        Long required=extra(admin,"Additional proof","EMPLOYER",null,true);
        own(company).andExpect(jsonPath("$.status").value("INCOMPLETE"));
        mvc.perform(put("/api/admin/verification-requirements/"+required).header("Authorization",auth(admin)).contentType("application/json").content(input("Additional proof","EMPLOYER",null,true,false))).andExpect(status().isOk());
        extra(admin,"Optional evidence","EMPLOYER",null,false);own(company).andExpect(jsonPath("$.status").value("VERIFIED"));
        review(admin,replacement,"FAILED","Change").andExpect(status().isConflict());
        Long newer=submit(company,"TAX_DOCUMENT");review(admin,replacement,"VERIFIED","").andExpect(status().isConflict());
        assertThat(newer).isNotEqualTo(replacement);own(company).andExpect(jsonPath("$.status").value("PENDING"));
    }
    @Test void privateDocumentsAndManagementAreProtectedAgainstDirectRequests() throws Exception {
        var c=candidate();var other=candidate();var company=employer("education");var admin=account(Role.ADMIN);var evaluator=account(Role.EVALUATOR);
        Long id=submit(c,"CANDIDATE_NID"), employerDoc=submit(company,"TRADE_LICENSE");
        for(var u:List.of(other,company,evaluator)) mvc.perform(get("/api/verifications/documents/"+id).header("Authorization",auth(u))).andExpect(status().isForbidden());
        mvc.perform(get("/api/verifications/documents/"+employerDoc).header("Authorization",auth(c))).andExpect(status().isForbidden());
        mvc.perform(get("/api/verifications/documents/"+id)).andExpect(status().isUnauthorized());
        for(var u:List.of(c,admin)) mvc.perform(get("/api/verifications/documents/"+id).header("Authorization",auth(u))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andExpect(content().bytes(pdf().getBytes()));
        for(var u:List.of(c,company,evaluator)) {
            mvc.perform(post("/api/admin/verification-requirements").header("Authorization",auth(u)).contentType("application/json").content(input("Fake","CANDIDATE",null,true,true))).andExpect(status().isForbidden());
            mvc.perform(put("/api/admin/verification-requirements/"+requirement("CANDIDATE_NID")).header("Authorization",auth(u)).contentType("application/json").content(input("Fake","CANDIDATE",null,true,true))).andExpect(status().isForbidden());
            review(u,id,"VERIFIED","").andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/verification-submissions").header("Authorization",auth(u))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/evaluator/verifications/"+id+"/review").header("Authorization",auth(evaluator)).contentType("application/json").content("{\"status\":\"VERIFIED\",\"notes\":\"fake\"}")).andExpect(status().isForbidden());
        own(other).andExpect(jsonPath("$.history").isEmpty());
        mvc.perform(get("/api/admin/verification-submissions").header("Authorization",auth(admin))).andExpect(status().isOk()).andExpect(jsonPath("$[0].passwordHash").doesNotExist()).andExpect(jsonPath("$[0].storedName").doesNotExist());
    }
    @Test void uploadsValidateSizeContentAndNamesAndPreservePreviousFiles() throws Exception {
        var c=candidate();Long req=requirement("CANDIDATE_NID");
        for(var bad:List.of(file("../nid.pdf","application/pdf",pdf().getBytes()),file("nid.svg","image/svg+xml","<svg/>".getBytes()),file("nid.pdf","application/pdf","fake document".getBytes()),file("nid.pdf","application/pdf",new byte[5*1024*1024+1]),file("nid.pdf","application/pdf",new byte[0]),file("nid.png","image/jpeg",pdf().getBytes()))) upload(c,req,bad).andExpect(status().isBadRequest());
        upload(c,Long.MAX_VALUE,pdf()).andExpect(status().isBadRequest());
        Long first=submit(c,"CANDIDATE_NID"), second=submit(c,"CANDIDATE_NID");
        review(account(Role.ADMIN),first,"VERIFIED","").andExpect(status().isConflict());
        for(Long id:List.of(first,second)) mvc.perform(get("/api/verifications/documents/"+id).header("Authorization",auth(c))).andExpect(status().isOk());
        assertThat(records.findById(second).orElseThrow().getStoredName()).doesNotContain("evidence");
        upload(c,req,file("nid.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10,1})).andExpect(status().isCreated());
        upload(c,req,file("nid.jpg","image/jpeg",new byte[]{(byte)255,(byte)216,(byte)255,1})).andExpect(status().isCreated());
    }
    @Test void legacyMigrationPreservesApprovalAndPrivateNotesWithoutBypassingExtras() throws Exception {
        var c=candidate();var record=new VerificationRecord();record.setCandidate(candidates.findByUserId(c.getId()).orElseThrow());record.setStatus(VerificationStatus.VERIFIED);record.setIdentityReference("PRIVATE-LEGACY");record.setReviewerNotes("PRIVATE-NOTES");records.saveAndFlush(record);
        own(c).andExpect(jsonPath("$.status").value("VERIFIED"));catalog.run(null);catalog.run(null);
        assertThat(record.getRequirement().getCode()).isEqualTo("CANDIDATE_NID");assertThat(record.getIdentityReference()).isEqualTo("PRIVATE-LEGACY");
        String response=own(c).andExpect(jsonPath("$.status").value("VERIFIED")).andReturn().getResponse().getContentAsString();assertThat(response).doesNotContain("PRIVATE");
        extra(account(Role.ADMIN),"Certificate","CANDIDATE",null,true);own(c).andExpect(jsonPath("$.status").value("INCOMPLETE"));
        assertThat(checklist.candidateStatus(record.getCandidate().getId())).isEqualTo("INCOMPLETE");
        mvc.perform(put("/api/admin/verification-requirements/"+requirement("CANDIDATE_NID")).header("Authorization",auth(account(Role.ADMIN))).contentType("application/json").content(input("National ID","CANDIDATE",null,true,false))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/verifications/me").header("Authorization",auth(c)).contentType("application/json").content("{\"identityReference\":\"text cannot bypass upload\"}")).andExpect(status().isGone());
    }
}
