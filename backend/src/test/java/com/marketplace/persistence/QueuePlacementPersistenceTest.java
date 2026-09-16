package com.marketplace.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.marketplace.user.*;
import com.marketplace.candidate.*;
import com.marketplace.employer.*;
import com.marketplace.skill.*;
import com.marketplace.placement.*;
import com.marketplace.replacement.*;
import com.marketplace.notification.*;
import com.marketplace.audit.*;
import com.marketplace.booking.*;
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
class QueuePlacementPersistenceTest {
    @Autowired EntityManager em;
    @Autowired WaitingListEntryRepository queues;
    @Autowired PlacementRepository placements;
    @Autowired ReplacementRequestRepository replacements;
    @Autowired NotificationRepository notifications;
    @Autowired AuditLogRepository audits;
    @Autowired CandidateProfileRepository candidates;
    @Autowired AppointmentSlotRepository slots;

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

    private WaitingListEntry queue(CandidateProfile candidate, Skill skill, QueueStatus status, Instant joinedAt) {
        WaitingListEntry entry = new WaitingListEntry();
        entry.setCandidate(candidate);
        entry.setSkill(skill);
        entry.setStatus(status);
        entry.setJoinedAt(joinedAt);
        if (status == QueueStatus.RESERVED) entry.setReservedAt(Instant.now());
        return persist(entry);
    }

    private Placement placement() {
        Placement placement = new Placement();
        placement.setCandidate(candidate("placed@example.test"));
        placement.setEmployer(employer("employer@example.test"));
        placement.setSkill(skill("Electrical work"));
        placement.setStatus(PlacementStatus.ACTIVE);
        placement.setStartDate(LocalDate.of(2026, 9, 13));
        placement.setGuaranteeEligible(true);
        placement.setGuaranteeExpiresAt(Instant.parse("2026-10-13T00:00:00Z"));
        return persist(placement);
    }

    private ReplacementRequest request(Placement placement) {
        ReplacementRequest request = new ReplacementRequest();
        request.setPlacement(placement);
        request.setEmployer(placement.getEmployer());
        request.setReason("Fictional availability change");
        request.setRequestedAt(Instant.parse("2026-09-13T12:00:00Z"));
        request.setTargetCompletionAt(request.getRequestedAt().plusSeconds(86400));
        return persist(request);
    }

    @Test
    void fifoFiltersSkillAndStateAndBreaksTiesById() {
        Skill skill = skill("Electrical work");
        Skill otherSkill = skill("Plumbing");
        Instant joined = Instant.parse("2026-09-13T09:00:00Z");
        WaitingListEntry later = queue(candidate("later@example.test"), skill, QueueStatus.QUEUED, joined.plusSeconds(60));
        WaitingListEntry first = queue(candidate("first@example.test"), skill, QueueStatus.QUEUED, joined);
        WaitingListEntry second = queue(candidate("second@example.test"), skill, QueueStatus.QUEUED, joined);
        queue(candidate("reserved@example.test"), skill, QueueStatus.RESERVED, joined.minusSeconds(60));
        queue(candidate("exited@example.test"), skill, QueueStatus.EXITED, joined.minusSeconds(60));
        queue(candidate("other@example.test"), otherSkill, QueueStatus.QUEUED, joined);
        em.clear();
        assertThat(queues.findBySkillIdAndStatusOrderByJoinedAtAscIdAsc(skill.getId(), QueueStatus.QUEUED))
                .extracting(WaitingListEntry::getId).containsExactly(first.getId(), second.getId(), later.getId());
    }

    @Test
    void duplicateActiveQueueMembershipRejectedEvenWhenReserved() {
        CandidateProfile candidate = candidate("candidate@example.test");
        Skill skill = skill("Electrical work");
        queue(candidate, skill, QueueStatus.RESERVED, Instant.now());
        assertThat(queues.existsByCandidateIdAndSkillIdAndActiveMembershipTrue(candidate.getId(), skill.getId())).isTrue();
        assertThatThrownBy(() -> queue(candidate, skill, QueueStatus.QUEUED, Instant.now()))
                .isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void exitedHistoryAllowsRejoiningAndReservationRelease() {
        CandidateProfile candidate = candidate("candidate@example.test");
        Skill skill = skill("Electrical work");
        WaitingListEntry first = queue(candidate, skill, QueueStatus.RESERVED, Instant.now());
        first.setStatus(QueueStatus.EXITED);
        first.setExitReason("Offer declined");
        em.flush();
        WaitingListEntry second = queue(candidate, skill, QueueStatus.RESERVED, Instant.now());
        second.setStatus(QueueStatus.EXITED);
        em.flush();
        WaitingListEntry third = queue(candidate, skill, QueueStatus.RESERVED, Instant.now());
        third.setStatus(QueueStatus.QUEUED);
        third.setReservedAt(null);
        em.flush(); em.clear();
        assertThat(queues.findByCandidateIdAndStatusIn(candidate.getId(), List.of(QueueStatus.EXITED))).hasSize(2);
        assertThat(queues.existsByCandidateIdAndStatus(candidate.getId(), QueueStatus.RESERVED)).isFalse();
        assertThat(queues.findById(third.getId()).orElseThrow().getActiveMembership()).isTrue();
    }

    @Test
    void reservationAcrossDifferentSkillsCannotDoubleSelectCandidate() {
        CandidateProfile candidate = candidate("candidate@example.test");
        queue(candidate, skill("Electrical work"), QueueStatus.RESERVED, Instant.now());
        WaitingListEntry second = queue(candidate, skill("Plumbing"), QueueStatus.QUEUED, Instant.now());
        second.setStatus(QueueStatus.RESERVED);
        assertThatThrownBy(em::flush).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void directSqlCannotBypassActiveMembershipMarker() {
        WaitingListEntry entry = queue(candidate("candidate@example.test"), skill("Electrical work"), QueueStatus.QUEUED, Instant.now());
        assertThatThrownBy(() -> em.createNativeQuery("update waiting_list_entries set active_membership = null where id = :id")
                .setParameter("id", entry.getId()).executeUpdate()).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void placementAndReplacementOwnershipAndLifecycleLinksPersist() {
        Placement original = placement();
        ReplacementRequest request = request(original);
        Long ownerId = original.getEmployer().getUser().getId();
        Long otherId = user("other@example.test", Role.EMPLOYER).getId();
        assertThat(placements.findByIdAndEmployerUserId(original.getId(), ownerId)).isPresent();
        assertThat(placements.findByIdAndEmployerUserId(original.getId(), otherId)).isEmpty();
        assertThat(replacements.findByIdAndEmployerUserId(request.getId(), otherId)).isEmpty();
        assertThat(replacements.findByIdAndEmployerUserId(request.getId(), ownerId)).isPresent();
        assertThat(replacements.findByPlacementIdAndActiveRequestTrue(original.getId())).isPresent();
        CandidateProfile selected = candidate("replacement@example.test");
        Placement replacement = new Placement();
        replacement.setCandidate(selected);
        replacement.setEmployer(original.getEmployer());
        replacement.setSkill(original.getSkill());
        replacement.setStatus(PlacementStatus.ACTIVE);
        replacement.setStartDate(LocalDate.of(2026, 9, 14));
        persist(replacement);
        request.setSelectedCandidate(selected);
        request.setReplacementPlacement(replacement);
        request.setStatus(ReplacementStatus.COMPLETED);
        request.setActualCompletionAt(request.getTargetCompletionAt());
        request.setSlaStatus(SlaStatus.ON_TIME);
        original.setStatus(PlacementStatus.REPLACED);
        em.flush(); em.clear();
        ReplacementRequest loaded = replacements.findById(request.getId()).orElseThrow();
        assertThat(loaded.getReplacementPlacement().getCandidate().getId()).isEqualTo(selected.getId());
        assertThat(loaded.getPlacement().getStatus()).isEqualTo(PlacementStatus.REPLACED);
        assertThat(loaded.getSlaStatus()).isEqualTo(SlaStatus.ON_TIME);
        assertThat(loaded.getActualCompletionAt()).isEqualTo(loaded.getTargetCompletionAt());
        assertThat(replacements.findByPlacementIdAndActiveRequestTrue(original.getId())).isEmpty();
    }

    @Test
    void duplicateActiveReplacementRejected() {
        Placement placement = placement();
        request(placement);
        assertThatThrownBy(() -> request(placement)).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    @Test
    void retryRetainsClockAndFailedHistoryAllowsAnotherRequest() {
        Placement placement = placement();
        ReplacementRequest first = request(placement);
        Instant requestedAt = first.getRequestedAt();
        Instant target = first.getTargetCompletionAt();
        first.setStatus(ReplacementStatus.FAILED);
        first.setFailureReason("Queue exhausted");
        em.flush();
        assertThat(replacements.findByPlacementIdAndActiveRequestTrue(placement.getId())).isEmpty();
        first.setStatus(ReplacementStatus.MATCHING);
        em.flush(); em.refresh(first);
        assertThat(first.getRequestedAt()).isEqualTo(requestedAt);
        assertThat(first.getTargetCompletionAt()).isEqualTo(target);
        assertThat(replacements.findByPlacementIdAndActiveRequestTrue(placement.getId())).isPresent();
        first.setStatus(ReplacementStatus.FAILED);
        em.flush();
        request(placement);
        assertThat(replacements.findByPlacementIdOrderByRequestedAtDescIdDesc(placement.getId())).hasSize(2);
    }

    @Test
    void repositoryLockQueriesLoadRowsWithinTransaction() {
        Placement placement = placement();
        ReplacementRequest request = request(placement);
        WaitingListEntry entry = queue(candidate("queue@example.test"), placement.getSkill(), QueueStatus.QUEUED, Instant.now());
        AppointmentSlot slot = new AppointmentSlot();
        slot.setStartTime(Instant.parse("2026-10-01T09:00:00Z"));
        slot.setEndTime(slot.getStartTime().plusSeconds(3600));
        persist(slot);
        em.clear();
        assertThat(candidates.findByIdForUpdate(entry.getCandidate().getId())).isPresent();
        assertThat(queues.findByIdForUpdate(entry.getId())).isPresent();
        assertThat(placements.findByIdForUpdate(placement.getId())).isPresent();
        assertThat(replacements.findByIdForUpdate(request.getId())).isPresent();
        assertThat(slots.findByIdForUpdate(slot.getId())).isPresent();
    }

    @Test
    void unreadNotificationsAreScopedToUserAndAuditSupportsSystemActor() {
        User recipient = user("recipient@example.test", Role.CANDIDATE);
        User other = user("other@example.test", Role.CANDIDATE);
        Notification notification = new Notification();
        notification.setUser(recipient);
        notification.setTitle("Replacement selected");
        notification.setMessage("Fictional in-app notification");
        persist(notification);
        assertThat(notifications.countByUserIdAndReadAtIsNull(recipient.getId())).isEqualTo(1);
        assertThat(notifications.findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(other.getId())).isEmpty();
        assertThat(notifications.findByIdAndUserId(notification.getId(), other.getId())).isEmpty();
        notification.setReadAt(Instant.now());
        AuditLog audit = new AuditLog();
        audit.setAction("NOTIFICATION_CREATED");
        audit.setEntityType("Notification");
        audit.setEntityId(notification.getId());
        audit.setDetails("Fictional system action; no identity evidence");
        persist(audit);
        em.clear();
        assertThat(notifications.countByUserIdAndReadAtIsNull(recipient.getId())).isZero();
        assertThat(audits.findByEntityTypeAndEntityIdOrderByCreatedAtAscIdAsc("Notification", notification.getId()))
                .singleElement().satisfies(loaded -> {
                    assertThat(loaded.getActorUser()).isNull();
                    assertThat(loaded.getCreatedAt()).isNotNull();
                });
    }
}
