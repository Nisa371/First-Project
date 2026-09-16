package com.marketplace.demo;

import com.marketplace.user.*;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.skill.*;
import com.marketplace.job.*;
import com.marketplace.assessment.*;
import com.marketplace.verification.*;
import com.marketplace.replacement.*;
import com.marketplace.placement.*;
import com.marketplace.training.*;
import com.marketplace.notification.*;
import com.marketplace.booking.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.BigDecimal;

/** Atomic, opt-in development fixture. All identities use reserved example.test addresses. */
@Component @Profile("dev") @RequiredArgsConstructor
@ConditionalOnProperty(name="app.demo.enabled",havingValue="true")
public class ShowcaseData implements ApplicationRunner {
 private final UserRepository users; private final PasswordEncoder encoder;
 private final CandidateProfileRepository candidates; private final CandidateSkillRepository candidateSkills;
 private final EmployerProfileRepository employers; private final SkillRepository skills;
 private final JobRepository jobs; private final ShortlistEntryRepository shortlists;
 private final AssessmentRepository assessments; private final AssessmentAttemptRepository attempts;
 private final EvaluationRepository evaluations; private final VerificationRecordRepository verifications;
 private final WaitingListEntryRepository queue; private final PlacementRepository placements;
 private final ReplacementRequestRepository replacements; private final ReferralRepository referrals;
 private final TrainingProgramRepository programs; private final NotificationRepository notifications;
 private final AppointmentSlotRepository slots;
 private final SkillCatalog skillCatalog; private final AssessmentDemoInitializer assessmentCatalog; private final TrainingCatalog trainingCatalog;
 @Value("${app.demo.password:}") private String password;
 @Override @Transactional public void run(ApplicationArguments args) {
  if(password.length()<10 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("Set DEMO_PASSWORD to a private 10–72 character password before enabling showcase data.");
  // The admin is the transaction's marker: a committed marker means the entire fixture committed.
  if(users.existsByEmailIgnoreCase("admin@showcase.example.test")) return;
  for(String name:new String[]{"evaluator","employer","studio","trade1","trade2","trade3","trade4","trade5","trade6","tech1","tech2"})
   if(users.existsByEmailIgnoreCase(name+"@showcase.example.test")) throw new IllegalStateException("Showcase email collision; use a fresh development database.");
  skillCatalog.run(args); assessmentCatalog.run(args); trainingCatalog.run(args);
  var now=Instant.now().truncatedTo(ChronoUnit.MILLIS); var hash=encoder.encode(password);
  user("admin",Role.ADMIN,hash); var evaluator=user("evaluator",Role.EVALUATOR,hash);
  var company=company("employer","Padma Field Services (fictional)","Facilities & logistics",hash);
  var studio=company("studio","Meghna Digital Studio (fictional)","Software & design",hash);
  var driving=skills.findByNameIgnoreCase("Driving").orElseThrow(); var java=skills.findByNameIgnoreCase("Java").orElseThrow();
  var tradeAssessment=assessments.findAll().stream().filter(a->a.getCandidateType()==CandidateType.TRADE).findFirst().orElseThrow();
  var techAssessment=assessments.findAll().stream().filter(a->a.getCandidateType()==CandidateType.TECH).findFirst().orElseThrow();
  var worker=new CandidateProfile[8]; String[] names={"আরিফ হাসান","মীনা আক্তার","রাফি করিম","সাবিনা রহমান","তানভীর আলম","নাবিলা নূর","Ishra Sen","Ayan Rahman"};
  for(int i=0;i<8;i++) {
   boolean trade=i<6;var c=new CandidateProfile();c.setUser(user((trade?"trade"+(i+1):"tech"+(i-5)),Role.CANDIDATE,hash));c.setFullName(names[i]);c.setCandidateType(trade?CandidateType.TRADE:CandidateType.TECH);
   c.setLocation(i%2==0?"Dhaka":"Gazipur");c.setAvailability(i==0 || i==6?Availability.UNAVAILABLE:Availability.AVAILABLE);
   c.setBio(trade?"Fictional showcase profile: dependable field worker with a focus on safe service.":"Fictional showcase profile: early-career developer building thoughtful, accessible software.");
   c.setExperienceSummary(trade?"Two years of simulated fleet and customer service experience.":"University projects in Java and web development.");
   if(trade)c.setPrimaryTradeCategory("Driving");else c.setEducationSummary("BSc in Computer Science · Fictional university cohort");
   candidates.save(c);worker[i]=c;var link=new CandidateSkill();link.setCandidate(c);link.setSkill(trade?driving:java);link.setProficiencyLevel("Intermediate");candidateSkills.save(link);
   var v=new VerificationRecord();v.setCandidate(c);v.setIdentityReference("FICTIONAL-SHOWCASE-"+(i+1));v.setSubmittedAt(now.minus(4,ChronoUnit.DAYS));
   v.setStatus(i==5?VerificationStatus.PENDING:VerificationStatus.VERIFIED);if(i!=5){v.setReviewerUser(evaluator);v.setReviewedAt(now.minus(3,ChronoUnit.DAYS));v.setReviewerNotes("Fictional platform/manual review fixture; no real identity evidence.");}verifications.save(v);
   var a=new AssessmentAttempt();a.setCandidate(c);a.setAssessment(trade?tradeAssessment:techAssessment);a.setStartedAt(now.minus(3,ChronoUnit.DAYS));a.setSubmittedAt(now.minus(2,ChronoUnit.DAYS));a.setStatus(i==5?AttemptStatus.SUBMITTED:AttemptStatus.EVALUATED);if(i!=5)a.setEvaluatedAt(now.minus(1,ChronoUnit.DAYS));attempts.save(a);
   if(i!=5){var e=new Evaluation();e.setAttempt(a);e.setEvaluatorUser(evaluator);e.setScore(BigDecimal.valueOf(i==4?55:85+i));e.setRecommendation(i==4?Recommendation.NEEDS_TRAINING:Recommendation.HIRE_READY);e.setCandidateFeedback(i==4?"Practice safety checks and workplace communication. A training referral is ready for you.":"Strong foundations and clear communication. Ready for the next hiring step.");e.setReleased(true);evaluations.save(e);}
   notify(c.getUser(),"Welcome to your showcase workspace","This is a fictional demo profile. Explore your assessment, readiness and career activity.");
  }
  var driverJob=job(company,"Fleet driver · Day shift","Support a local service team with safe, scheduled journeys.",driving,CandidateType.TRADE);
  job(company,"Relief driver · Gazipur","Join a managed relief team with clear schedules and safety-first work.",driving,CandidateType.TRADE);
  var techJob=job(studio,"Junior Java developer","Build and test useful web services with a small mentoring team.",java,CandidateType.TECH);
  job(studio,"Graduate developer","Turn university skills into practical product experience.",java,CandidateType.TECH);
  for(int i:new int[]{0,2,3}) {var s=new ShortlistEntry();s.setJob(driverJob);s.setCandidate(worker[i]);shortlists.save(s);}
  var shortlist=new ShortlistEntry();shortlist.setJob(techJob);shortlist.setCandidate(worker[7]);shortlists.save(shortlist);
  for(int i=1;i<=3;i++){var q=new WaitingListEntry();q.setCandidate(worker[i]);q.setSkill(driving);q.setJoinedAt(now.minus(8-i,ChronoUnit.HOURS));if(i==1){q.setStatus(QueueStatus.RESERVED);q.setReservedAt(now.minus(1,ChronoUnit.HOURS));}queue.save(q);}
  var original=placement(worker[0],company,driverJob,driving,now,true);
  placement(worker[6],studio,techJob,java,now,false);
  var r=new ReplacementRequest();r.setPlacement(original);r.setEmployer(company);r.setReason("Fictional demo: original worker is unavailable for the next scheduled shift.");r.setRequestedAt(now.minus(1,ChronoUnit.HOURS));r.setTargetCompletionAt(now.plus(23,ChronoUnit.HOURS));r.setStatus(ReplacementStatus.CANDIDATE_SELECTED);r.setSelectedCandidate(worker[1]);replacements.save(r);
  notify(company.getUser(),"A replacement is ready to confirm","A verified worker is reserved. Open Replacements to confirm and activate the placement.");
  notify(worker[1].getUser(),"You have been selected","You are reserved for a managed replacement. Employer confirmation is the next step.");
  var referral=new Referral();referral.setCandidate(worker[4]);referral.setCreatedByUser(evaluator);referral.setTrainingProgram(programs.findByActiveTrueOrderByTitleAsc().stream().filter(p->p.getTitle().startsWith("Workplace")).findFirst().orElseThrow());referrals.save(referral);
  notify(worker[4].getUser(),"Your next learning step","Open Training to view your workplace safety and communication referral.");
  var slot=new AppointmentSlot();slot.setEvaluatorUser(evaluator);slot.setStartTime(now.plus(1,ChronoUnit.DAYS));slot.setEndTime(now.plus(1,ChronoUnit.DAYS).plus(1,ChronoUnit.HOURS));slot.setCapacity(3);slots.save(slot);
 }
 private User user(String name,Role role,String hash){var u=new User();u.setEmail(name+"@showcase.example.test");u.setRole(role);u.setPasswordHash(hash);return users.save(u);}
 private EmployerProfile company(String account,String name,String industry,String hash){var e=new EmployerProfile();e.setUser(user(account,Role.EMPLOYER,hash));e.setCompanyName(name);e.setIndustry(industry);e.setAddress("Dhaka · fictional demo office");e.setDescription("A fictional showcase employer supporting fair, skills-based hiring.");return employers.save(e);}
 private Job job(EmployerProfile company,String title,String description,Skill skill,CandidateType type){var j=new Job();j.setEmployer(company);j.setTitle(title);j.setDescription(description);j.setLocation("Dhaka");j.setRequiredSkill(skill);j.setCandidateType(type);j.setStatus(JobStatus.ACTIVE);return jobs.save(j);}
 private Placement placement(CandidateProfile c,EmployerProfile e,Job j,Skill skill,Instant now,boolean guarantee){var p=new Placement();p.setCandidate(c);p.setEmployer(e);p.setJob(j);p.setSkill(skill);p.setStatus(PlacementStatus.ACTIVE);p.setStartDate(LocalDate.ofInstant(now,ZoneOffset.UTC).minusDays(2));p.setGuaranteeEligible(guarantee);if(guarantee)p.setGuaranteeExpiresAt(now.plus(28,ChronoUnit.DAYS));return placements.save(p);}
 private void notify(User u,String title,String message){var n=new Notification();n.setUser(u);n.setTitle(title);n.setMessage(message);notifications.save(n);}
}
