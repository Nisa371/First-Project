package com.marketplace.persistence;

import java.math.BigDecimal;
import java.time.Instant;

import com.marketplace.user.*;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.skill.*;
import com.marketplace.job.*;
import com.marketplace.assessment.*;
import com.marketplace.booking.*;
import com.marketplace.verification.*;
import com.marketplace.training.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DatabaseFoundationTest {
    @Autowired EntityManager em;
    @Autowired UserRepository users;
    @Autowired JobRepository jobs;
    @Autowired BookingRepository bookings;
    @Autowired EvaluationRepository evaluations;
    @Autowired VerificationRecordRepository verifications;
    @Autowired ReferralRepository referrals;

    private <T> T persist(T value) {
        em.persist(value);
        em.flush();
        return value;
    }

    private User user(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("fictional-test-hash-not-a-login-credential");
        user.setRole(role);
        return persist(user);
    }

    private CandidateProfile candidate(String email) {
        CandidateProfile candidate = new CandidateProfile();
        candidate.setUser(user(email, Role.CANDIDATE));
        candidate.setFullName("Fictional Candidate");
        candidate.setCandidateType(CandidateType.TRADE);
        return persist(candidate);
    }

    private EmployerProfile employer(String email) {
        EmployerProfile employer = new EmployerProfile();
        employer.setUser(user(email, Role.EMPLOYER));
        employer.setCompanyName("Fictional Employer");
        return persist(employer);
    }

    private Skill skill(String name) {
        Skill skill = new Skill();
        skill.setName(name);
        skill.setCategory("TRADE");
        return persist(skill);
    }

    private Job job(EmployerProfile employer) {
        Job job = new Job();
        job.setEmployer(employer);
        job.setTitle("Electrician");
        job.setDescription("Fictional job");
        job.setLocation("Dhaka");
        return persist(job);
    }

    @Test
    void identityRelationshipsEnumsAndTimestampsSurviveReload() {
        CandidateProfile candidate = candidate("  CANDIDATE@example.test ");
        EmployerProfile employer = employer("employer@example.test");
        
        em.clear();
        CandidateProfile loaded = em.find(CandidateProfile.class, candidate.getId());
        assertThat(loaded.getUser().getEmail()).isEqualTo("candidate@example.test");
        assertThat(loaded.getCandidateType()).isEqualTo(CandidateType.TRADE);
        assertThat(loaded.getUser().getRole()).isEqualTo(Role.CANDIDATE);
        assertThat(loaded.getUser().getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(em.find(EmployerProfile.class, employer.getId()).getUser().getRole()).isEqualTo(Role.EMPLOYER);
        Instant originalCreatedAt = loaded.getCreatedAt();
        Instant previousUpdatedAt = loaded.getUpdatedAt();
        loaded.setBio("Updated profile");
        em.flush();
        assertThat(loaded.getUpdatedAt()).isAfter(previousUpdatedAt);
        assertThat(loaded.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(users.findByEmailIgnoreCase("CANDIDATE@example.test")).isPresent();
    }

    @Test
    void duplicateNormalizedEmailIsRejected() {
        user("same@example.test", Role.CANDIDATE);
        assertThatThrownBy(() -> user(" SAME@example.test ", Role.EMPLOYER))
                .isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void duplicateCandidateProfileIsRejected() {
        CandidateProfile first = candidate("candidate@example.test");
        CandidateProfile duplicate = new CandidateProfile();
        duplicate.setUser(first.getUser());
        duplicate.setFullName("Duplicate");
        assertThatThrownBy(() -> persist(duplicate)).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void duplicateEmployerProfileIsRejected() {
        EmployerProfile first = employer("employer@example.test");
        EmployerProfile duplicate = new EmployerProfile();
        duplicate.setUser(first.getUser());
        duplicate.setCompanyName("Duplicate");
        assertThatThrownBy(() -> persist(duplicate)).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void profileRequiresPersistedOwner() {
        CandidateProfile orphan = new CandidateProfile();
        orphan.setFullName("Orphan");
        assertThatThrownBy(() -> persist(orphan)).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void duplicateSkillNameIsRejected() {
        skill("Electrical work");
        assertThatThrownBy(() -> skill("Electrical work")).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void duplicateCandidateSkillIsRejected() {
        CandidateProfile candidate = candidate("candidate@example.test");
        Skill skill = skill("Electrical work");
        CandidateSkill first = new CandidateSkill();
        first.setCandidate(candidate);
        first.setSkill(skill);
        persist(first);
        CandidateSkill duplicate = new CandidateSkill();
        duplicate.setCandidate(candidate);
        duplicate.setSkill(skill);
        assertThatThrownBy(() -> persist(duplicate)).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void jobOwnershipQueryExcludesAnotherEmployer() {
        EmployerProfile owner = employer("owner@example.test");
        EmployerProfile other = employer("other@example.test");
        Job job = job(owner);
        em.clear();
        assertThat(jobs.findByIdAndEmployerUserId(job.getId(), owner.getUser().getId())).isPresent();
        assertThat(jobs.findByIdAndEmployerUserId(job.getId(), other.getUser().getId())).isEmpty();
    }

    @Test
    void duplicateShortlistIsRejected() {
        CandidateProfile candidate = candidate("candidate@example.test");
        Job job = job(employer("employer@example.test"));
        ShortlistEntry first = new ShortlistEntry();
        first.setCandidate(candidate);
        first.setJob(job);
        persist(first);
        ShortlistEntry duplicate = new ShortlistEntry();
        duplicate.setCandidate(candidate);
        duplicate.setJob(job);
        assertThatThrownBy(() -> persist(duplicate)).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    private AssessmentAttempt attempt() {
        Assessment assessment = new Assessment();
        assessment.setTitle("Electrical safety");
        assessment.setCandidateType(CandidateType.TRADE);
        persist(assessment);
        AssessmentQuestion question = new AssessmentQuestion();
        question.setAssessment(assessment);
        question.setPrompt("Which option is safe?");
        question.setOptionA("A"); question.setOptionB("B");
        question.setOptionC("C"); question.setOptionD("D");
        question.setCorrectOption(AnswerOption.B);
        persist(question);
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessment(assessment);
        attempt.setCandidate(candidate("candidate@example.test"));
        attempt.setAnswersJson("{\"" + question.getId() + "\":\"B\"}");
        return persist(attempt);
    }

    private Evaluation evaluation(AssessmentAttempt attempt) {
        Evaluation evaluation = new Evaluation();
        evaluation.setAttempt(attempt);
        evaluation.setScore(BigDecimal.ONE);
        evaluation.setRecommendation(Recommendation.HIRE_READY);
        return persist(evaluation);
    }

    @Test
    void assessmentAnswersEvaluationAndReleasePersist() {
        AssessmentAttempt attempt = attempt();
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(Instant.now());
        Evaluation evaluation = evaluation(attempt);
        assertThat(evaluations.findByAttemptCandidateIdAndReleasedTrue(attempt.getCandidate().getId())).isEmpty();
        evaluation.setReleased(true);
        attempt.setStatus(AttemptStatus.EVALUATED);
        attempt.setEvaluatedAt(Instant.now());
        em.flush(); em.clear();
        Evaluation loaded = evaluations.findByAttemptId(attempt.getId()).orElseThrow();
        assertThat(loaded.getAttempt().getStatus()).isEqualTo(AttemptStatus.EVALUATED);
        assertThat(loaded.getAttempt().getAnswersJson()).contains("\":\"B\"");
        assertThat(loaded.getAttempt().getStartedAt()).isNotNull();
        assertThat(loaded.getRecommendation()).isEqualTo(Recommendation.HIRE_READY);
        assertThat(loaded.getEvaluatorUser()).isNull();
        assertThat(evaluations.findByAttemptCandidateIdAndReleasedTrue(attempt.getCandidate().getId())).hasSize(1);
    }

    @Test
    void duplicateEvaluationForAttemptIsRejected() {
        AssessmentAttempt attempt = attempt();
        evaluation(attempt);
        assertThatThrownBy(() -> evaluation(attempt)).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    private AppointmentSlot slot() {
        AppointmentSlot slot = new AppointmentSlot();
        slot.setStartTime(Instant.parse("2026-10-01T09:00:00Z"));
        slot.setEndTime(slot.getStartTime().plusSeconds(3600));
        return persist(slot);
    }

    @Test
    void bookingRelationshipsCapacityAndOverlapQueries() {
        AppointmentSlot slot = slot();
        CandidateProfile candidate = candidate("candidate@example.test");
        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setCandidate(candidate);
        booking.setPurpose(BookingPurpose.CONSULTATION);
        persist(booking);
        assertThat(bookings.countBySlotIdAndStatus(slot.getId(), BookingStatus.BOOKED)).isEqualTo(1);
        assertThat(bookings.hasOverlappingBooking(candidate.getId(), slot.getStartTime().plusSeconds(60), slot.getEndTime())).isTrue();
        assertThat(bookings.hasOverlappingBooking(candidate.getId(), slot.getEndTime(), slot.getEndTime().plusSeconds(60))).isFalse();
        booking.setStatus(BookingStatus.CANCELLED);
        em.flush(); em.clear();
        assertThat(bookings.hasOverlappingBooking(candidate.getId(), slot.getStartTime(), slot.getEndTime())).isFalse();
        assertThat(em.find(Booking.class, booking.getId()).getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void invalidSlotCapacityIsRejected() {
        AppointmentSlot slot = slot();
        slot.setCapacity(0);
        assertThatThrownBy(em::flush).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void reversedSlotTimeIsRejected() {
        AppointmentSlot slot = slot();
        slot.setEndTime(slot.getStartTime());
        assertThatThrownBy(em::flush).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void verificationAndReferralLifecyclePersists() {
        CandidateProfile candidate = candidate("candidate@example.test");
        User evaluator = user("evaluator@example.test", Role.EVALUATOR);
        VerificationRecord verification = new VerificationRecord();
        verification.setCandidate(candidate);
        verification.setIdentityReference("FICTIONAL-***-42");
        persist(verification);
        for (VerificationStatus status : VerificationStatus.values()) {
            verification.setStatus(status);
            verification.setReviewerUser(evaluator);
            verification.setReviewedAt(Instant.now());
            em.flush(); em.refresh(verification);
            assertThat(verification.getStatus()).isEqualTo(status);
        }
        TrainingProgram program = new TrainingProgram();
        program.setProviderName("Fictional Skills Centre");
        program.setTitle("Electrical safety");
        persist(program);
        Referral referral = new Referral();
        referral.setCandidate(candidate);
        referral.setTrainingProgram(program);
        referral.setCreatedByUser(evaluator);
        persist(referral);
        for (ReferralStatus status : ReferralStatus.values()) {
            referral.setStatus(status);
            em.flush(); em.refresh(referral);
            assertThat(referral.getStatus()).isEqualTo(status);
        }
        em.clear();
        assertThat(verifications.findFirstByCandidateIdOrderBySubmittedAtDescIdDesc(candidate.getId()).orElseThrow()
                .getReviewerUser().getId()).isEqualTo(evaluator.getId());
        assertThat(referrals.findByCandidateIdOrderByCreatedAtDesc(candidate.getId())).hasSize(1);
    }
    @Test
    void foreignKeyPreventsDeletingProfileOwner() {
        CandidateProfile candidate = candidate("candidate@example.test");
        assertThatThrownBy(() -> em.createNativeQuery("delete from users where id = :id")
                .setParameter("id", candidate.getUser().getId()).executeUpdate())
                .isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void questionRequiresPositivePoints() {
        AssessmentAttempt attempt = attempt();
        AssessmentQuestion question = em.createQuery(
                "select q from AssessmentQuestion q where q.assessment.id = :id", AssessmentQuestion.class)
                .setParameter("id", attempt.getAssessment().getId()).getSingleResult();
        question.setPoints(0);
        assertThatThrownBy(em::flush).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

}
