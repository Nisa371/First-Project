package com.marketplace.marketplace;
import com.marketplace.auth.JwtService;
import com.marketplace.user.*;
import com.marketplace.candidate.*;
import com.marketplace.assessment.*;
import com.marketplace.training.*;
import com.marketplace.notification.*;
import com.marketplace.audit.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class TrainingAdminIntegrationTest {
 @Autowired MockMvc mvc; @Autowired JwtService jwt; @Autowired UserRepository users;
 @Autowired CandidateProfileRepository candidates; @Autowired AssessmentRepository assessments;
 @Autowired AssessmentAttemptRepository attempts; @Autowired EvaluationRepository evaluations;
 @Autowired TrainingProgramRepository programs; @Autowired NotificationRepository notifications; @Autowired AuditLogRepository audit;
 User user(Role role){var u=new User();u.setEmail(UUID.randomUUID()+"@example.test");u.setRole(role);u.setPasswordHash("unusable-test-hash");return users.saveAndFlush(u);}
 String auth(User u){return "Bearer "+jwt.issue(u.getId());}
 Evaluation evaluation(User evaluator,boolean released){var c=new CandidateProfile();c.setUser(user(Role.CANDIDATE));c.setFullName("Fictional learner");candidates.saveAndFlush(c);var a=new Assessment();a.setTitle("Demo");a.setCandidateType(CandidateType.TECH);assessments.saveAndFlush(a);var attempt=new AssessmentAttempt();attempt.setCandidate(c);attempt.setAssessment(a);attempt.setStatus(AttemptStatus.EVALUATED);attempts.saveAndFlush(attempt);var e=new Evaluation();e.setAttempt(attempt);e.setEvaluatorUser(evaluator);e.setScore(BigDecimal.valueOf(55));e.setRecommendation(Recommendation.NEEDS_TRAINING);e.setReleased(released);return evaluations.saveAndFlush(e);}
 TrainingProgram program(){var p=new TrainingProgram();p.setTitle("Practice lab");p.setProviderName("Fictional studio");return programs.saveAndFlush(p);}
 String body(Evaluation e,TrainingProgram p){return "{\"evaluationId\":"+e.getId()+",\"programId\":"+p.getId()+"}";}
 @Test void referralRequiresReleasedOwnedEvaluationAndNotifiesOnlyCandidate() throws Exception {
  var reviewer=user(Role.EVALUATOR);var e=evaluation(reviewer,false);var p=program();var c=e.getAttempt().getCandidate().getUser();
  mvc.perform(post("/api/referrals").header("Authorization",auth(reviewer)).contentType("application/json").content(body(e,p))).andExpect(status().isForbidden());
  e.setReleased(true);evaluations.saveAndFlush(e);
  for(var u:List.of(user(Role.EVALUATOR),user(Role.EMPLOYER),c))mvc.perform(post("/api/referrals").header("Authorization",auth(u)).contentType("application/json").content(body(e,p))).andExpect(status().isForbidden());
  mvc.perform(post("/api/referrals").header("Authorization",auth(reviewer)).contentType("application/json").content(body(e,p))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFERRED"));
  mvc.perform(post("/api/referrals").header("Authorization",auth(reviewer)).contentType("application/json").content(body(e,p))).andExpect(status().isConflict());
  mvc.perform(get("/api/referrals").header("Authorization",auth(c))).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].createdByUser").doesNotExist());
  var other=evaluation(reviewer,true).getAttempt().getCandidate().getUser();mvc.perform(get("/api/referrals").header("Authorization",auth(other))).andExpect(jsonPath("$").isEmpty());
  mvc.perform(get("/api/notifications/me").header("Authorization",auth(c))).andExpect(jsonPath("$[0].title").value("Your next learning step"));
 }
 @Test void adminCanReferAndInactiveProgramsAndClientStatusAreRejected() throws Exception {
  var admin=user(Role.ADMIN);var e=evaluation(user(Role.EVALUATOR),true);var p=program();p.setActive(false);programs.saveAndFlush(p);
  mvc.perform(post("/api/referrals").header("Authorization",auth(admin)).contentType("application/json").content(body(e,p))).andExpect(status().isNotFound());
  p.setActive(true);programs.saveAndFlush(p);
  mvc.perform(post("/api/referrals").header("Authorization",auth(admin)).contentType("application/json").content(body(e,p).replace("}",",\"status\":\"COMPLETED\"}"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/referrals").header("Authorization",auth(admin)).contentType("application/json").content(body(e,p))).andExpect(status().isOk());
  mvc.perform(get("/api/training-programs")).andExpect(status().isUnauthorized());
 }
 @Test void adminStatusRevokesExistingTokensAndCanRestoreWithAudit() throws Exception {
  var admin=user(Role.ADMIN);var target=user(Role.EMPLOYER);var token=auth(target);
  mvc.perform(get("/api/admin/stats").header("Authorization",token)).andExpect(status().isForbidden());
  mvc.perform(get("/api/admin/users").header("Authorization",auth(admin))).andExpect(status().isOk()).andExpect(jsonPath("$[0].passwordHash").doesNotExist());
  mvc.perform(patch("/api/admin/users/"+target.getId()+"/status").header("Authorization",token).contentType("application/json").content("{\"status\":\"BLOCKED\"}")).andExpect(status().isForbidden());
  mvc.perform(patch("/api/admin/users/"+target.getId()+"/status").header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"SUSPENDED\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/training-programs").header("Authorization",token)).andExpect(status().isForbidden());
  assertThat(audit.findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc("USER",target.getId())).anyMatch(a->a.getAction().equals("ACCOUNT_STATUS_SUSPENDED") && a.getEntityId().equals(target.getId()));
  mvc.perform(patch("/api/admin/users/"+target.getId()+"/status").header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"ACTIVE\"}")).andExpect(status().isOk());
  mvc.perform(get("/api/training-programs").header("Authorization",token)).andExpect(status().isOk());
  mvc.perform(patch("/api/admin/users/"+admin.getId()+"/status").header("Authorization",auth(admin)).contentType("application/json").content("{\"status\":\"BLOCKED\"}")).andExpect(status().isConflict());
  for(var path:List.of("stats","jobs","skills","verifications"))mvc.perform(get("/api/admin/"+path).header("Authorization",auth(admin))).andExpect(status().isOk());
 }
}
