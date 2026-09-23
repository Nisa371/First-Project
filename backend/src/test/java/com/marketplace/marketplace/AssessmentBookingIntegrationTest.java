package com.marketplace.marketplace;
import com.marketplace.assessment.*;
import com.marketplace.booking.*;
import com.marketplace.candidate.*;
import com.marketplace.user.*;
import java.util.*;
import java.time.Instant;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class AssessmentBookingIntegrationTest {
    @Autowired com.marketplace.companytype.CompanyTypeRepository companyTypes;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    @Autowired CandidateProfileRepository candidates;
    @Autowired AssessmentRepository assessments;
    @Autowired AssessmentQuestionRepository questions;
    @Autowired AssessmentAttemptRepository attempts;
    @Autowired BookingRepository bookings;
    @Autowired AppointmentSlotRepository slots;
    record Account(String auth,Long userId) {}
    Account account(String role,String track) throws Exception {
        var data=new HashMap<String,Object>(Map.of("email",UUID.randomUUID()+"@example.com","password","SafePass123!",
            "accountType",role.equals("EMPLOYER")?role:"CANDIDATE","fullName","M5 Candidate","companyName","M5 Company"));
        if(!role.equals("EMPLOYER")) data.put("candidateType",track);
        if(role.equals("EMPLOYER")) data.put("companyTypeId",companyTypes.findByNormalizedName("pharmaceuticals").orElseThrow().getId());
        var response=mvc.perform(post("/api/auth/register").contentType("application/json").content(mapper.writeValueAsString(data)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var tree=mapper.readTree(response); Long id=tree.get("user").get("id").asLong();
        if(role.equals("EVALUATOR")) { var u=users.findById(id).orElseThrow(); u.setRole(Role.EVALUATOR); users.saveAndFlush(u); }
        return new Account("Bearer "+tree.get("token").asText(),id);
    }
    Long assessment(CandidateType type) {
        var a=new Assessment(); a.setTitle("M5 test "+UUID.randomUUID()); a.setCandidateType(type); a.setPassingScore(BigDecimal.valueOf(70)); assessments.saveAndFlush(a);
        for(int i=0;i<2;i++) { var q=new AssessmentQuestion(); q.setAssessment(a); q.setPrompt("Question "+i); q.setOptionA("A"); q.setOptionB("B"); q.setOptionC("C"); q.setOptionD("D"); q.setCorrectOption(AnswerOption.A); q.setPoints(i+1); questions.saveAndFlush(q); }
        return a.getId();
    }
    JsonNode start(Account c,Long id) throws Exception { return mapper.readTree(mvc.perform(post("/api/assessments/"+id+"/attempts").header("Authorization",c.auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()); }
    String answers(Long assessmentId) { var qs=questions.findByAssessmentIdOrderByIdAsc(assessmentId); return "{\"answers\":{\""+qs.get(0).getId()+"\":\"B\",\""+qs.get(1).getId()+"\":\"A\"}}"; }
    void submit(Account c,Long assessmentId,Long id) throws Exception {
        mvc.perform(put("/api/assessment-attempts/"+id+"/answers").header("Authorization",c.auth).contentType("application/json").content(answers(assessmentId))).andExpect(status().isOk());
        mvc.perform(post("/api/assessment-attempts/"+id+"/submit").header("Authorization",c.auth)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUBMITTED"));
    }
    String draft="{\"score\":82,\"recommendation\":\"HIRE_READY\",\"feedback\":\"Strong work. Keep practicing.\",\"internalNotes\":\"Private evaluator discussion\"}";
    @Test void techAttemptReviewReleaseAndPrivacy() throws Exception {
        var c=account("CANDIDATE","TECH"); var evaluator=account("EVALUATOR","TECH"); var other=account("EVALUATOR","TECH"); var employer=account("EMPLOYER",null);
        Long assessmentId=assessment(CandidateType.TECH); Long id=start(c,assessmentId).get("id").asLong();
        assertThat(start(c,assessmentId).get("id").asLong()).isEqualTo(id);
        mvc.perform(get("/api/assessments/"+assessmentId).header("Authorization",c.auth)).andExpect(status().isOk()).andExpect(jsonPath("$.questions[0].correctOption").doesNotExist());
        mvc.perform(post("/api/assessment-attempts/"+id+"/submit").header("Authorization",c.auth)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/evaluator/attempts/"+id).header("Authorization",evaluator.auth)).andExpect(status().isNotFound());
        submit(c,assessmentId,id);
        assertThat(attempts.findById(id).orElseThrow().getAutoScore()).isEqualByComparingTo("66.67");
        mvc.perform(post("/api/assessment-attempts/"+id+"/submit").header("Authorization",c.auth)).andExpect(status().isOk());
        mvc.perform(put("/api/assessment-attempts/"+id+"/answers").header("Authorization",c.auth).contentType("application/json").content(answers(assessmentId))).andExpect(status().isConflict());
        mvc.perform(post("/api/evaluator/attempts/"+id+"/release").header("Authorization",evaluator.auth)).andExpect(status().isConflict());
        mvc.perform(put("/api/evaluator/attempts/"+id+"/evaluate").header("Authorization",evaluator.auth).contentType("application/json").content(draft)).andExpect(status().isOk()).andExpect(jsonPath("$.released").value(false));
        var privateView=mvc.perform(get("/api/assessment-attempts/"+id).header("Authorization",c.auth)).andExpect(status().isOk()).andExpect(jsonPath("$.result").isEmpty()).andExpect(jsonPath("$.autoScore").isEmpty()).andReturn().getResponse().getContentAsString();
        assertThat(privateView).doesNotContain("Private evaluator", "Strong work", "correctOption");
        mvc.perform(get("/api/candidates/me").header("Authorization",c.auth)).andExpect(jsonPath("$.releasedResults").isEmpty());
        mvc.perform(put("/api/evaluator/attempts/"+id+"/evaluate").header("Authorization",other.auth).contentType("application/json").content(draft)).andExpect(status().isConflict());
        mvc.perform(post("/api/evaluator/attempts/"+id+"/release").header("Authorization",other.auth)).andExpect(status().isConflict());
        mvc.perform(post("/api/evaluator/attempts/"+id+"/release").header("Authorization",evaluator.auth)).andExpect(status().isOk()).andExpect(jsonPath("$.released").value(true));
        mvc.perform(get("/api/assessment-attempts/"+id).header("Authorization",c.auth)).andExpect(jsonPath("$.status").value("EVALUATED")).andExpect(jsonPath("$.result.score").value(82)).andExpect(jsonPath("$.result.feedback").value("Strong work. Keep practicing.")).andExpect(jsonPath("$.internalNotes").doesNotExist());
        mvc.perform(get("/api/candidates/me").header("Authorization",c.auth)).andExpect(jsonPath("$.releasedResults[0].score").value(82));
        mvc.perform(put("/api/evaluator/attempts/"+id+"/evaluate").header("Authorization",evaluator.auth).contentType("application/json").content(draft)).andExpect(status().isConflict());
        mvc.perform(get("/api/assessment-attempts/"+id).header("Authorization",employer.auth)).andExpect(status().isForbidden());
    }
    @Test void eligibilityOwnershipValidationAndTradeManualStrategy() throws Exception {
        var c=account("CANDIDATE","TECH"); var stranger=account("CANDIDATE","TECH"); var trade=account("CANDIDATE","TRADE"); var evaluator=account("EVALUATOR","TECH");
        var techId=assessment(CandidateType.TECH); var tradeId=assessment(CandidateType.TRADE); var id=start(c,techId).get("id").asLong();
        mvc.perform(post("/api/assessments/"+techId+"/attempts").header("Authorization",trade.auth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/assessment-attempts/"+id).header("Authorization",stranger.auth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/assessment-attempts/"+id+"/submit").header("Authorization",stranger.auth)).andExpect(status().isNotFound());
        mvc.perform(put("/api/assessment-attempts/"+id+"/answers").header("Authorization",stranger.auth).contentType("application/json").content(answers(techId))).andExpect(status().isNotFound());
        for(String body:List.of("{\"answers\":{\"99999999\":\"A\"}}","{\"answers\":{\"1\":null}}","{\"answers\":{},\"autoScore\":100}","{\"answers\":null}")) {
            mvc.perform(put("/api/assessment-attempts/"+id+"/answers").header("Authorization",c.auth).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/evaluator/attempts").header("Authorization",c.auth)).andExpect(status().isForbidden());
        var t=start(trade,tradeId).get("id").asLong(); submit(trade,tradeId,t);
        assertThat(attempts.findById(t).orElseThrow().getAutoScore()).isNull();
        mvc.perform(put("/api/evaluator/attempts/"+t+"/evaluate").header("Authorization",evaluator.auth).contentType("application/json").content(draft.replace("82","101"))).andExpect(status().isBadRequest());
        var inactive=assessments.findById(techId).orElseThrow(); inactive.setActive(false); assessments.saveAndFlush(inactive);
        mvc.perform(post("/api/assessments/"+techId+"/attempts").header("Authorization",stranger.auth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/assessment-attempts/"+id).header("Authorization",c.auth)).andExpect(status().isOk());
    }
    Long slot(Account evaluator,Instant start,int capacity) throws Exception {
        String body=mapper.writeValueAsString(Map.of("startTime",start,"endTime",start.plusSeconds(1800),"capacity",capacity));
        return mapper.readTree(mvc.perform(post("/api/evaluator/appointment-slots").header("Authorization",evaluator.auth).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asLong();
    }
    String bookingBody(Long id) { return "{\"slotId\":"+id+",\"purpose\":\"CONSULTATION\",\"notes\":\"Discuss next steps\"}"; }
    Long book(Account c,Long slot) throws Exception {
        Long id=mapper.readTree(mvc.perform(post("/api/bookings").header("Authorization",c.auth).contentType("application/json").content(bookingBody(slot))).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING_PAYMENT")).andReturn().getResponse().getContentAsString()).get("id").asLong();
        assertThat(pay(c,id)).isEqualTo(200); return id;
    }
    int pay(Account c,Long id) throws Exception {
        Long paymentId=mapper.readTree(mvc.perform(post("/api/bookings/"+id+"/payment").header("Authorization",c.auth)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("id").asLong();
        return mvc.perform(post("/api/payments/"+paymentId+"/demo-success").header("Authorization",c.auth)).andReturn().getResponse().getStatus();
    }
    @Test void bookingCapacityOverlapOwnershipCancellationAndSlotRules() throws Exception {
        var e=account("EVALUATOR","TECH"); var other=account("EVALUATOR","TECH"); var c=account("CANDIDATE","TECH"); var stranger=account("CANDIDATE","TECH");
        var start=Instant.now().plusSeconds(86400); var s=slot(e,start,1); var overlap=slot(other,start.plusSeconds(600),2); var adjacent=slot(e,start.plusSeconds(1800),1);
        for(String body:List.of(mapper.writeValueAsString(Map.of("startTime",start,"endTime",start.minusSeconds(1),"capacity",1)),mapper.writeValueAsString(Map.of("startTime",Instant.now().minusSeconds(300),"endTime",start,"capacity",1)),mapper.writeValueAsString(Map.of("startTime",start,"endTime",start.plusSeconds(1),"capacity",0)))) {
            mvc.perform(post("/api/evaluator/appointment-slots").header("Authorization",e.auth).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/evaluator/appointment-slots").header("Authorization",e.auth).contentType("application/json").content(mapper.writeValueAsString(Map.of("startTime",start.plusSeconds(100),"endTime",start.plusSeconds(200),"capacity",1)))).andExpect(status().isConflict());
        var b=book(c,s);
        for(var pair:List.of(Map.entry(c,s),Map.entry(stranger,s),Map.entry(c,overlap))) mvc.perform(post("/api/bookings").header("Authorization",pair.getKey().auth).contentType("application/json").content(bookingBody(pair.getValue()))).andExpect(status().isConflict());
        book(c,adjacent);
        mvc.perform(post("/api/bookings/"+b+"/cancel").header("Authorization",stranger.auth)).andExpect(status().isNotFound());
        mvc.perform(get("/api/evaluator/bookings").header("Authorization",other.auth)).andExpect(jsonPath("$").isEmpty());
        mvc.perform(post("/api/evaluator/appointment-slots/"+s+"/close").header("Authorization",other.auth)).andExpect(status().isNotFound());
        mvc.perform(post("/api/evaluator/appointment-slots/"+s+"/close").header("Authorization",e.auth)).andExpect(status().isConflict());
        mvc.perform(post("/api/bookings/"+b+"/cancel").header("Authorization",c.auth)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/bookings/"+b+"/cancel").header("Authorization",c.auth)).andExpect(status().isOk());
        book(stranger,s);
        var closed=slot(other,start.plusSeconds(7200),1);
        mvc.perform(post("/api/evaluator/appointment-slots/"+closed+"/close").header("Authorization",other.auth)).andExpect(status().isOk());
        mvc.perform(post("/api/bookings").header("Authorization",c.auth).contentType("application/json").content(bookingBody(closed))).andExpect(status().isConflict());
        mvc.perform(post("/api/bookings").header("Authorization",e.auth).contentType("application/json").content(bookingBody(s))).andExpect(status().isForbidden());
        var past=slots.findById(s).orElseThrow(); past.setStartTime(Instant.now().minusSeconds(3600)); past.setEndTime(Instant.now().minusSeconds(1800)); slots.saveAndFlush(past);
        mvc.perform(post("/api/bookings").header("Authorization",c.auth).contentType("application/json").content(bookingBody(s))).andExpect(status().isConflict());
    }
    @Test void concurrentBookingDoesNotOverflowCapacityOrDoubleBookCandidate() throws Exception {
        var e=account("EVALUATOR","TECH"); var other=account("EVALUATOR","TECH"); var c=account("CANDIDATE","TECH"); var d=account("CANDIDATE","TECH");
        var start=Instant.now().plusSeconds(172800); var s=slot(e,start,1);
        assertThat(race(c,s,d,s)).containsExactlyInAnyOrder(200,409);
        assertThat(bookings.countBySlotIdAndStatus(s,BookingStatus.BOOKED)).isEqualTo(1);
        var a=slot(e,start.plusSeconds(7200),2); var b=slot(other,start.plusSeconds(7500),2);
        assertThat(race(c,a,c,b)).containsExactlyInAnyOrder(200,409);
    }
    List<Integer> race(Account a,Long first,Account b,Long second) throws Exception {
        var ready=new java.util.concurrent.CountDownLatch(2); var go=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var futures=new ArrayList<java.util.concurrent.Future<Integer>>();
            for(var pair:List.of(Map.entry(a,first),Map.entry(b,second))) futures.add(pool.submit(() -> { ready.countDown(); go.await(); var response=mvc.perform(post("/api/bookings").header("Authorization",pair.getKey().auth).contentType("application/json").content(bookingBody(pair.getValue()))).andReturn().getResponse();
                if(response.getStatus()!=201) return response.getStatus();
                return pay(pair.getKey(),mapper.readTree(response.getContentAsString()).get("id").asLong()); }));
            assertThat(ready.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue(); go.countDown();
            return List.of(futures.get(0).get(20,java.util.concurrent.TimeUnit.SECONDS),futures.get(1).get(20,java.util.concurrent.TimeUnit.SECONDS));
        }
    }
}
