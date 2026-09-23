package com.marketplace.payment;

import com.marketplace.auth.JwtService;
import com.marketplace.booking.*;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.job.*;
import com.marketplace.user.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class PaymentIntegrationTest {
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JwtService jwt;
    @Autowired UserRepository users; @Autowired EmployerProfileRepository employers;
    @Autowired CandidateProfileRepository candidates; @Autowired JobRepository jobs;
    @Autowired BookingRepository bookings; @Autowired AppointmentSlotRepository slots;
    @Autowired PaymentRepository payments; @Autowired DemoPaymentPrices prices;
    User user(Role role) {
        var u=new User(); u.setRole(role); u.setEmail(UUID.randomUUID()+"@payment.test"); u.setPasswordHash("test-only"); users.saveAndFlush(u);
        if(role==Role.EMPLOYER) { var e=new EmployerProfile(); e.setUser(u); e.setCompanyName("Payment Company"); employers.saveAndFlush(e); }
        if(role==Role.CANDIDATE) { var c=new CandidateProfile(); c.setUser(u); c.setFullName("Payment Candidate"); c.setCandidateType(CandidateType.TECH); candidates.saveAndFlush(c); }
        return u;
    }
    String auth(User u) { return "Bearer "+jwt.issue(u.getId()); }
    ResultActions postAs(User u,String url) throws Exception { return mvc.perform(post(url).header("Authorization",auth(u))); }
    JsonNode json(ResultActions result) throws Exception { return mapper.readTree(result.andReturn().getResponse().getContentAsString()); }
    long job(User u) throws Exception {
        return json(mvc.perform(post("/api/jobs").header("Authorization",auth(u)).contentType("application/json").content(jobBody()))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT"))).get("id").asLong();
    }
    String jobBody() { return "{\"title\":\"Payment role\",\"description\":\"Work with us\",\"location\":\"Dhaka\",\"candidateType\":\"TECH\"}"; }
    long payment(User u,String resource,long id) throws Exception { return json(postAs(u,"/api/"+resource+"/"+id+"/payment").andExpect(status().isOk())).get("id").asLong(); }
    ResultActions complete(User u,long id) throws Exception { return postAs(u,"/api/payments/"+id+"/demo-success"); }
    long slot(Instant start) {
        var s=new AppointmentSlot(); s.setStartTime(start); s.setEndTime(start.plusSeconds(1800)); s.setCapacity(1); return slots.saveAndFlush(s).getId();
    }
    long book(User u,long slot) throws Exception {
        return json(mvc.perform(post("/api/bookings").header("Authorization",auth(u)).contentType("application/json")
            .content("{\"slotId\":"+slot+",\"purpose\":\"CONSULTATION\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))).get("id").asLong();
    }
    @Test void jobGatePricesOwnershipAndIdempotency() throws Exception {
        var e=user(Role.EMPLOYER); var other=user(Role.EMPLOYER); var c=user(Role.CANDIDATE);
        long j=job(e), unpaid=job(e);
        mvc.perform(get("/api/candidates/me/jobs/"+j).header("Authorization",auth(c))).andExpect(status().isNotFound());
        var discovery=json(mvc.perform(get("/api/candidates/me/jobs").header("Authorization",auth(c)).param("search","Payment role")).andExpect(status().isOk()));
        for(var item:discovery.get("content")) assertThat(item.get("id").asLong()).isNotIn(j,unpaid);
        postAs(other,"/api/jobs/"+j+"/payment").andExpect(status().isNotFound());
        postAs(c,"/api/jobs/"+j+"/payment").andExpect(status().isForbidden());
        long p=payment(e,"jobs",j); assertThat(payment(e,"jobs",j)).isEqualTo(p);
        var view=json(mvc.perform(get("/api/payments/"+p).header("Authorization",auth(e))).andExpect(status().isOk()));
        assertThat(view.get("amount").decimalValue()).isEqualByComparingTo(prices.jobPostingFee());
        assertThat(view.get("currency").asText()).isEqualTo(prices.currency());
        assertThat(view.get("reference").asText()).startsWith("DEMO-PAY-");
        for(var stranger:List.of(other,c)) {
            mvc.perform(get("/api/payments/"+p).header("Authorization",auth(stranger))).andExpect(status().isNotFound());
            complete(stranger,p).andExpect(status().isNotFound());
            postAs(stranger,"/api/payments/"+p+"/cancel").andExpect(status().isNotFound());
            mvc.perform(get("/api/payments/me").header("Authorization",auth(stranger))).andExpect(jsonPath("$").isEmpty());
        }
        complete(e,p).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        var completed=payments.findById(p).orElseThrow().getCompletedAt();
        complete(e,p).andExpect(status().isOk());
        assertThat(payments.findById(p).orElseThrow().getCompletedAt()).isEqualTo(completed);
        assertThat(jobs.findById(j).orElseThrow().getStatus()).isEqualTo(JobStatus.ACTIVE);
        assertThat(jobs.findById(unpaid).orElseThrow().getStatus()).isEqualTo(JobStatus.DRAFT);
        mvc.perform(get("/api/candidates/me/jobs/"+j).header("Authorization",auth(c))).andExpect(status().isOk());
        mvc.perform(put("/api/jobs/"+j).header("Authorization",auth(e)).contentType("application/json").content(jobBody())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        postAs(e,"/api/jobs/"+j+"/payment").andExpect(status().isConflict());
        postAs(e,"/api/jobs/"+j+"/close").andExpect(status().isOk());
        complete(e,p).andExpect(status().isOk());
        assertThat(jobs.findById(j).orElseThrow().getStatus()).isEqualTo(JobStatus.CLOSED);
        assertThat(payments.findById(p).orElseThrow().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
    @Test void immutableInputsAndStatusCannotBypassGates() throws Exception {
        var e=user(Role.EMPLOYER); long j=job(e), p=payment(e,"jobs",j);
        for(String body:List.of("{\"amount\":1}","{\"purpose\":\"SESSION_BOOKING\"}","{\"payerId\":1}","{\"jobId\":1}","{\"bookingId\":1}")) {
            mvc.perform(post("/api/payments/"+p+"/demo-success").header("Authorization",auth(e)).contentType("application/json").content(body)).andExpect(status().isBadRequest());
            mvc.perform(post("/api/jobs/"+j+"/payment").header("Authorization",auth(e)).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/jobs/"+j).header("Authorization",auth(e)).contentType("application/json").content(jobBody().replace("}",",\"status\":\"ACTIVE\"}"))).andExpect(status().isBadRequest());
        assertThat(jobs.findById(j).orElseThrow().getStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(payments.findById(p).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        var invalid=new PaymentTransaction(); invalid.setPurpose(PaymentPurpose.JOB_POSTING); invalid.setBooking(new Booking());
        assertThatThrownBy(invalid::validateResource).isInstanceOf(IllegalStateException.class);
        invalid.setJob(new Job()); assertThatThrownBy(invalid::validateResource).isInstanceOf(IllegalStateException.class);
    }
    @Test void bookingPaymentOwnershipConfirmationAndDuplicateRequests() throws Exception {
        var c=user(Role.CANDIDATE); var stranger=user(Role.CANDIDATE); var employer=user(Role.EMPLOYER);
        long s=slot(Instant.now().plusSeconds(10000)), b=book(c,s);
        assertThat(book(c,s)).isEqualTo(b);
        assertThat(bookings.countBySlotIdAndStatus(s,BookingStatus.BOOKED)).isZero();
        long p=payment(c,"bookings",b); assertThat(payment(c,"bookings",b)).isEqualTo(p);
        assertThat(payments.findById(p).orElseThrow().getAmount()).isEqualByComparingTo(prices.sessionBookingFee());
        postAs(stranger,"/api/bookings/"+b+"/payment").andExpect(status().isNotFound());
        postAs(employer,"/api/bookings/"+b+"/payment").andExpect(status().isForbidden());
        complete(stranger,p).andExpect(status().isNotFound()); complete(employer,p).andExpect(status().isNotFound());
        long second=book(c,slot(Instant.now().plusSeconds(20000)));
        complete(c,p).andExpect(status().isOk()); complete(c,p).andExpect(status().isOk());
        assertThat(bookings.findById(b).orElseThrow().getStatus()).isEqualTo(BookingStatus.BOOKED);
        assertThat(bookings.findById(second).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        assertThat(bookings.countBySlotIdAndStatus(s,BookingStatus.BOOKED)).isEqualTo(1);
        postAs(c,"/api/bookings/"+b+"/cancel").andExpect(status().isOk()); complete(c,p).andExpect(status().isOk());
        assertThat(bookings.findById(b).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }
    @Test void cancelledPaymentsAndClosedResourcesCannotComplete() throws Exception {
        var e=user(Role.EMPLOYER); long j=job(e), p=payment(e,"jobs",j);
        postAs(e,"/api/payments/"+p+"/cancel").andExpect(status().isOk()); complete(e,p).andExpect(status().isConflict());
        assertThat(payment(e,"jobs",j)).isEqualTo(p);
        postAs(e,"/api/jobs/"+j+"/close").andExpect(status().isOk()); complete(e,p).andExpect(status().isConflict());
        var c=user(Role.CANDIDATE); long b=book(c,slot(Instant.now().plusSeconds(10000))), bp=payment(c,"bookings",b);
        postAs(c,"/api/payments/"+bp+"/cancel").andExpect(status().isOk());
        assertThat(bookings.findById(b).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        complete(c,bp).andExpect(status().isConflict());
        long b2=book(c,slot(Instant.now().plusSeconds(20000))), bp2=payment(c,"bookings",b2);
        postAs(c,"/api/bookings/"+b2+"/cancel").andExpect(status().isOk()); complete(c,bp2).andExpect(status().isConflict());
    }
    @Test void legacyPublishedJobsAndBookingsRemainValid() throws Exception {
        var e=user(Role.EMPLOYER); var c=user(Role.CANDIDATE); long j=job(e);
        var legacy=jobs.findById(j).orElseThrow(); legacy.setStatus(JobStatus.ACTIVE); jobs.saveAndFlush(legacy);
        mvc.perform(get("/api/candidates/me/jobs/"+j).header("Authorization",auth(c))).andExpect(status().isOk());
        var b=new Booking(); b.setCandidate(candidates.findByUserId(c.getId()).orElseThrow()); b.setSlot(slots.findById(slot(Instant.now().plusSeconds(10000))).orElseThrow()); b.setPurpose(BookingPurpose.CONSULTATION); bookings.saveAndFlush(b);
        mvc.perform(get("/api/bookings/me").header("Authorization",auth(c))).andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("BOOKED"));
        postAs(c,"/api/bookings/"+b.getId()+"/payment").andExpect(status().isConflict());
    }
    @Test void competingPaymentsCannotOversellAndPendingDoesNotConsumeCapacity() throws Exception {
        var c=user(Role.CANDIDATE); var d=user(Role.CANDIDATE); long s=slot(Instant.now().plusSeconds(10000));
        long b=book(c,s), other=book(d,s), p=payment(c,"bookings",b), q=payment(d,"bookings",other);
        assertThat(bookings.countBySlotIdAndStatus(s,BookingStatus.BOOKED)).isZero();
        assertThat(race(c,p,d,q)).containsExactlyInAnyOrder(200,409);
        assertThat(bookings.countBySlotIdAndStatus(s,BookingStatus.BOOKED)).isEqualTo(1);
    }
    @Test void concurrentSamePaymentAndOverlappingSessionsAreSafe() throws Exception {
        var c=user(Role.CANDIDATE); var start=Instant.now().plusSeconds(10000);
        long s=slot(start), b=book(c,s), p=payment(c,"bookings",b);
        assertThat(race(c,p,c,p)).containsExactly(200,200);
        assertThat(bookings.countBySlotIdAndStatus(s,BookingStatus.BOOKED)).isEqualTo(1);
        long b1=book(c,slot(start.plusSeconds(10000))), b2=book(c,slot(start.plusSeconds(10100)));
        assertThat(race(c,payment(c,"bookings",b1),c,payment(c,"bookings",b2))).containsExactlyInAnyOrder(200,409);
    }
    @Test void sessionsClosedOrPastBeforePaymentStayUnconfirmed() throws Exception {
        var c=user(Role.CANDIDATE); long s=slot(Instant.now().plusSeconds(10000)), b=book(c,s), p=payment(c,"bookings",b);
        var closed=slots.findById(s).orElseThrow(); closed.setActive(false); slots.saveAndFlush(closed);
        complete(c,p).andExpect(status().isConflict());
        assertThat(payments.findById(p).orElseThrow().getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(bookings.findById(b).orElseThrow().getStatus()).isEqualTo(BookingStatus.PENDING_PAYMENT);
        long pastId=slot(Instant.now().plusSeconds(20000)), pastBooking=book(c,pastId), pastPayment=payment(c,"bookings",pastBooking);
        var past=slots.findById(pastId).orElseThrow(); past.setStartTime(Instant.now().minusSeconds(2000)); past.setEndTime(Instant.now().minusSeconds(1000)); slots.saveAndFlush(past);
        complete(c,pastPayment).andExpect(status().isConflict());
        postAs(c,"/api/bookings/"+pastBooking+"/cancel").andExpect(status().isOk());
    }
    @Test void concurrentJobInitiationAndCompletionReuseOneTransaction() throws Exception {
        var e=user(Role.EMPLOYER); long j=job(e);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var go=new java.util.concurrent.CountDownLatch(1);
            var a=pool.submit(() -> { go.await(); return payment(e,"jobs",j); });
            var b=pool.submit(() -> { go.await(); return payment(e,"jobs",j); });
            go.countDown(); long p=a.get(20,java.util.concurrent.TimeUnit.SECONDS);
            assertThat(b.get(20,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(p);
            assertThat(race(e,p,e,p)).containsExactly(200,200);
        }
    }
    List<Integer> race(User a,long first,User b,long second) throws Exception {
        var go=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var f=pool.submit(() -> { go.await(); return complete(a,first).andReturn().getResponse().getStatus(); });
            var g=pool.submit(() -> { go.await(); return complete(b,second).andReturn().getResponse().getStatus(); });
            go.countDown(); return List.of(f.get(20,java.util.concurrent.TimeUnit.SECONDS),g.get(20,java.util.concurrent.TimeUnit.SECONDS));
        }
    }
}
