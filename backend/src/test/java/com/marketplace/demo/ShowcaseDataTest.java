package com.marketplace.demo;
import com.marketplace.user.*;
import com.marketplace.candidate.*;
import com.marketplace.replacement.*;
import com.marketplace.training.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@SpringBootTest(properties={"app.demo.enabled=true","spring.datasource.url=jdbc:h2:mem:showcase-fixture;DB_CLOSE_DELAY=-1",
 "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password="}) @ActiveProfiles("dev")
class ShowcaseDataTest {
 @DynamicPropertySource static void password(DynamicPropertyRegistry r){r.add("app.demo.password",()->java.util.UUID.randomUUID().toString());}
 @Autowired ShowcaseData data; @Autowired UserRepository users; @Autowired CandidateProfileRepository candidates;
 @Autowired QueueEligibilityService eligibility; @Autowired WaitingListEntryRepository queue;
 @Autowired ReferralRepository referrals; @Autowired ReplacementRequestRepository replacements;
 @Autowired org.springframework.test.web.servlet.MockMvc mvc; @Autowired com.marketplace.auth.JwtService jwt;
 @Test void fixtureIsAtomicCoherentAndIdempotent() throws Exception {
  assertThat(users.count()).isEqualTo(12);assertThat(candidates.count()).isEqualTo(8);assertThat(referrals.count()).isEqualTo(1);assertThat(replacements.count()).isEqualTo(1);
  var c=candidates.findByUserId(users.findByEmailIgnoreCase("trade3@showcase.example.test").orElseThrow().getId()).orElseThrow();
  assertThat(eligibility.check(c.getId(),null).eligible()).isTrue();assertThat(queue.count()).isEqualTo(3);
  data.run(new DefaultApplicationArguments());assertThat(users.count()).isEqualTo(12);assertThat(queue.count()).isEqualTo(3);assertThat(referrals.count()).isEqualTo(1);
  var employer=users.findByEmailIgnoreCase("employer@showcase.example.test").orElseThrow();var id=replacements.findAll().getFirst().getId();
  for(var action:java.util.List.of("accept","complete")) mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/replacements/"+id+"/"+action).header("Authorization","Bearer "+jwt.issue(employer.getId()))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
  assertThat(replacements.findById(id).orElseThrow().getStatus()).isEqualTo(ReplacementStatus.COMPLETED);
 }
}
