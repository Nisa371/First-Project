package com.marketplace.admin;
import com.marketplace.auth.JwtService;
import com.marketplace.user.*;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.job.*;
import com.marketplace.payment.*;
import com.marketplace.booking.*;
import com.marketplace.companytype.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.math.BigDecimal;
import java.time.Instant;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class AdminManagementIntegrationTest {
 @Autowired MockMvc mvc; @Autowired JwtService jwt; @Autowired UserRepository users;
 @Autowired CandidateProfileRepository candidates; @Autowired EmployerProfileRepository employers;
 @Autowired JobRepository jobs; @Autowired JobApplicationRepository applications; @Autowired PaymentRepository payments;
 @Autowired CompanyTypeRepository types; @Autowired BookingRepository bookings; @Autowired AppointmentSlotRepository slots;
 User user(Role role) { var u=new User();u.setEmail(UUID.randomUUID()+"@example.test");u.setRole(role);u.setPasswordHash("protected-test-hash");return users.saveAndFlush(u); }
 String auth(User u) {return "Bearer "+jwt.issue(u.getId());}
 CandidateProfile candidate() {var c=new CandidateProfile();c.setUser(user(Role.CANDIDATE));c.setFullName("Candidate "+UUID.randomUUID());c.setAvailability(Availability.AVAILABLE);return candidates.saveAndFlush(c);}
 EmployerProfile employer() {var e=new EmployerProfile();e.setUser(user(Role.EMPLOYER));e.setCompanyName("Company "+UUID.randomUUID());return employers.saveAndFlush(e);}
 Job job(EmployerProfile e) {var j=new Job();j.setEmployer(e);j.setCandidateType(CandidateType.TECH);j.setTitle("Admin vacancy "+UUID.randomUUID());j.setDescription("Description");j.setLocation("Dhaka");j.setStatus(JobStatus.ACTIVE);return jobs.saveAndFlush(j);}
 String jobBody() {return "{\"title\":\"Updated vacancy\",\"description\":\"Description\",\"location\":\"Dhaka\",\"candidateType\":\"TECH\",\"requiredSkillId\":null,\"expectedExperienceMonths\":12,\"publicExpectations\":\"Public\",\"privateExpectations\":\"Private hiring detail\"}";}
 @Test void everyOperationalReadRequiresAdminAndNeverExposesSecrets() throws Exception {
  var admin=user(Role.ADMIN);var c=candidate();var e=employer();job(e);
  for(var section:List.of("users","candidates","employers","jobs","applications","payments","bookings","assessments","attempts","skills","company-types","verification-requirements")) {
   var path="/api/admin/records/"+section;
   mvc.perform(get(path)).andExpect(status().isUnauthorized());
   for(var u:List.of(c.getUser(),e.getUser(),user(Role.EVALUATOR)))mvc.perform(get(path).header("Authorization",auth(u))).andExpect(status().isForbidden());
   var body=mvc.perform(get(path).header("Authorization",auth(admin)).param("size","1")).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.lessThanOrEqualTo(1))).andReturn().getResponse().getContentAsString();
   assertThat(body).doesNotContain("password","passwordHash","token","apiKey","protected-test-hash");
  }
  mvc.perform(get("/api/admin/records/users").param("search",c.getUser().getEmail()).header("Authorization",auth(admin))).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(get("/api/admin/records/users").param("size","999").header("Authorization",auth(admin))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/admin/records/users").param("page","-1").header("Authorization",auth(admin))).andExpect(status().isBadRequest());
  mvc.perform(get("/api/admin/records/jobs/999999999").header("Authorization",auth(admin))).andExpect(status().isNotFound());
  mvc.perform(get("/api/admin/stats").header("Authorization",auth(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.demoTransactions").isNumber());
 }
 @Test void profileEditsWhitelistFieldsAndReuseCompanyTypeRules() throws Exception {
  var admin=user(Role.ADMIN);var c=candidate();var e=employer();
  String input="{\"fullName\":\"Updated name\",\"phone\":\"01700000000\",\"location\":\"Dhaka\",\"availability\":\"AVAILABLE\",\"totalExperienceMonths\":18}";
  mvc.perform(put("/api/admin/candidates/"+c.getId()).header("Authorization",auth(admin)).contentType("application/json").content(input)).andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Updated name"));
  for(String field:List.of("passwordHash","password","token","refreshToken","apiKey","role","candidateType","verificationStatus"))
   mvc.perform(put("/api/admin/candidates/"+c.getId()).header("Authorization",auth(admin)).contentType("application/json").content(input.replace("}",",\""+field+"\":\"ADMIN\"}"))).andExpect(status().isBadRequest());
  assertThat(users.findById(c.getUser().getId()).orElseThrow().getPasswordHash()).isEqualTo("protected-test-hash");
  var type=new CompanyType();type.setName("Admin type "+UUID.randomUUID());type.setNormalizedName(type.getName().toLowerCase());types.saveAndFlush(type);
  String company="{\"companyName\":\"Updated company\",\"companyTypeId\":"+type.getId()+",\"contactPhone\":\"01700000000\"}";
  mvc.perform(put("/api/admin/employers/"+e.getId()).header("Authorization",auth(admin)).contentType("application/json").content(company)).andExpect(status().isOk()).andExpect(jsonPath("$.companyTypeId").value(type.getId()));
  mvc.perform(put("/api/admin/employers/"+e.getId()).header("Authorization",auth(admin)).contentType("application/json").content(company.replace("}",",\"customCompanyType\":\"Bypass\"}"))).andExpect(status().isBadRequest());
  mvc.perform(patch("/api/admin/users/"+c.getUser().getId()+"/status").header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"INVALID\"}")).andExpect(status().isBadRequest());
  mvc.perform(patch("/api/admin/users/"+c.getUser().getId()+"/status").header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"ACTIVE\",\"passwordHash\":\"bad\"}")).andExpect(status().isBadRequest());
 }
 @Test void jobOperationsPreserveApplicationsAndPaymentGateAndPrivateExpectations() throws Exception {
  var admin=user(Role.ADMIN);var c=candidate();var e=employer();var j=job(e);
  var a=new JobApplication();a.setCandidate(c);a.setJob(j);applications.saveAndFlush(a);
  mvc.perform(put("/api/admin/jobs/"+j.getId()).header("Authorization",auth(admin)).contentType("application/json").content(jobBody())).andExpect(status().isOk());
  mvc.perform(put("/api/admin/jobs/"+j.getId()).header("Authorization",auth(admin)).contentType("application/json").content(jobBody().replace("TECH","TRADE"))).andExpect(status().isConflict());
  mvc.perform(get("/api/candidates/me/jobs/"+j.getId()).header("Authorization",auth(c.getUser()))).andExpect(status().isOk()).andExpect(jsonPath("$.privateExpectations").doesNotExist());
  mvc.perform(delete("/api/admin/jobs/"+j.getId()).header("Authorization",auth(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CLOSED"));
  assertThat(applications.findById(a.getId())).isPresent();assertThat(jobs.findById(j.getId())).isPresent();
  mvc.perform(post("/api/admin/jobs/"+j.getId()+"/activate").header("Authorization",auth(admin))).andExpect(status().isConflict());
  var p=new PaymentTransaction();p.setJob(j);p.setPayer(e.getUser());p.setPurpose(PaymentPurpose.JOB_POSTING);p.setAmount(BigDecimal.valueOf(100));p.setCurrency("BDT");p.setReference("DEMO-"+UUID.randomUUID());p.setStatus(PaymentStatus.SUCCESS);payments.saveAndFlush(p);
  mvc.perform(post("/api/admin/jobs/"+j.getId()+"/activate").header("Authorization",auth(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
  mvc.perform(post("/api/admin/jobs").header("Authorization",auth(admin)).contentType("application/json").content("{\"employerId\":"+e.getId()+",\"job\":"+jobBody()+"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
  mvc.perform(get("/api/admin/records/payments/"+p.getId()).header("Authorization",auth(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.fields.amount").value(100));
  mvc.perform(put("/api/admin/payments/"+p.getId()).header("Authorization",auth(admin)).contentType("application/json").content("{\"amount\":0}")).andExpect(status().is4xxClientError());
 }
 @Test void applicationTransitionsReuseShortlistAndRejectScoreOrRelationshipTampering() throws Exception {
  var admin=user(Role.ADMIN);var c=candidate();var j=job(employer());var a=new JobApplication();a.setJob(j);a.setCandidate(c);applications.saveAndFlush(a);
  var path="/api/admin/applications/"+a.getId()+"/status";
  mvc.perform(patch(path).header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"SHORTLISTED\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHORTLISTED"));
  for(var field:List.of("cvScore","portfolioScore","assessmentScore","finalScore","candidateId","jobId"))mvc.perform(patch(path).header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"REJECTED\",\""+field+"\":99}")).andExpect(status().isBadRequest());
  mvc.perform(patch(path).header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"WITHDRAWN\"}")).andExpect(status().isBadRequest());
  mvc.perform(patch(path).header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"REJECTED\"}")).andExpect(status().isOk());
  assertThat(applications.countByJobId(j.getId())).isEqualTo(1);
 }
 @Test void bookingCancellationKeepsDemoPaymentConsistent() throws Exception {
  var admin=user(Role.ADMIN);var c=candidate();var slot=new AppointmentSlot();slot.setStartTime(Instant.now().plusSeconds(7200));slot.setEndTime(Instant.now().plusSeconds(10800));slot.setCapacity(2);slot.setEvaluatorUser(user(Role.EVALUATOR));slots.saveAndFlush(slot);
  var b=new Booking();b.setCandidate(c);b.setSlot(slot);b.setPurpose(BookingPurpose.values()[0]);b.setStatus(BookingStatus.PENDING_PAYMENT);bookings.saveAndFlush(b);
  var p=new PaymentTransaction();p.setBooking(b);p.setPayer(c.getUser());p.setPurpose(PaymentPurpose.SESSION_BOOKING);p.setAmount(BigDecimal.TEN);p.setCurrency("BDT");p.setReference("DEMO-"+UUID.randomUUID());payments.saveAndFlush(p);
  mvc.perform(post("/api/admin/bookings/"+b.getId()+"/cancel").header("Authorization",auth(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
  assertThat(payments.findById(p.getId()).orElseThrow().getStatus()).isEqualTo(PaymentStatus.CANCELLED);
  assertThat(payments.findById(p.getId()).orElseThrow().getAmount()).isEqualByComparingTo(BigDecimal.TEN);
 }
 @Autowired com.marketplace.verification.VerificationRecordRepository verificationRecords;
 @Autowired com.marketplace.verification.VerificationRequirementRepository requirements;
 @Test void pagedVerificationRetainsEmployerAndCandidateOwnershipAndFilters() throws Exception {
  var admin=user(Role.ADMIN); var employer=employer(); var c=candidate();
  var type=new CompanyType();type.setName("Review "+UUID.randomUUID());type.setNormalizedName(type.getName().toLowerCase());types.saveAndFlush(type);employer.setCompanyType(type);employers.saveAndFlush(employer);
  var req=new com.marketplace.verification.VerificationRequirement();req.setName("Document");req.setTargetType(com.marketplace.verification.VerificationTarget.EMPLOYER);requirements.saveAndFlush(req);
  var v=new com.marketplace.verification.VerificationRecord();v.setOwner(employer.getUser());v.setRequirement(req);verificationRecords.saveAndFlush(v);
  var cv=new com.marketplace.verification.VerificationRecord();cv.setOwner(c.getUser());cv.setCandidate(c);verificationRecords.saveAndFlush(cv);
  mvc.perform(get("/api/admin/verification-submissions/page").header("Authorization",auth(admin)).param("role","EMPLOYER").param("companyId",type.getId().toString()).param("status","PENDING").param("size","1"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(v.getId()));
  mvc.perform(get("/api/admin/verification-submissions/page").header("Authorization",auth(admin)).param("role","CANDIDATE").param("size","1"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].role").value("CANDIDATE"));
 }
 @Test void skillCatalogSupportsSafeCreateEditAndFiltering() throws Exception {
  var admin=user(Role.ADMIN);String name="Skill "+UUID.randomUUID();
  mvc.perform(post("/api/admin/skills").header("Authorization",auth(admin)).contentType("application/json").content("{\"name\":\""+name+"\",\"category\":\"TECH\",\"active\":true}")).andExpect(status().isOk());
  mvc.perform(get("/api/admin/records/skills").header("Authorization",auth(admin)).param("search",name).param("status","true")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
  mvc.perform(post("/api/admin/skills").header("Authorization",auth(admin)).contentType("application/json").content("{\"name\":\""+name+"\",\"category\":\"TECH\",\"active\":true}")).andExpect(status().isConflict());
 }

}
