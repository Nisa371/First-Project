package com.marketplace.placement;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.job.*;
import com.marketplace.replacement.*;
import com.marketplace.user.*;
import com.marketplace.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.context.ApplicationEventPublisher;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
public class PlacementService {
    private final PlacementRepository placements;
    private final com.marketplace.skill.SkillRepository catalog;
    private final jakarta.persistence.EntityManager em;
    private final ReplacementGuaranteePolicyService guaranteePolicy;
    private final CandidateProfileRepository candidates;
    private final CandidateSkillRepository skills;
    private final JobRepository jobs;
    private final com.marketplace.payment.PaymentRepository payments;
    private final ShortlistEntryRepository shortlists;
    private final JobApplicationRepository applications;
    private final WaitingListEntryRepository queue;
    private final ReplacementRequestRepository replacements;
    private final QueueEligibilityService eligibility;
    private final QueueService queueService;
    private final CurrentAccount current;
    private final ApplicationEventPublisher events;
    public record PlacementView(Long id, Long candidateId, String candidateName, String company, String job,
        String skill, PlacementStatus status, LocalDate startDate, boolean guaranteeEligible, Instant guaranteeExpiresAt, CandidateType candidateType, Long guaranteeDays, boolean replacementSourceEligible, Instant replacementWindowStartedAt, long replacementNeededCount) {}
    public record CandidateHistory(Long id,String company,String job,String skill,PlacementStatus status,LocalDate startDate,Instant endDate) {}
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER','ADMIN')")
    public List<?> mine(ManagedRecordFilter filter) {
        var u=current.requireActive();
        if(u.getRole()==Role.CANDIDATE) {
            var c=candidates.findByUserId(u.getId()).orElseThrow(CandidateService::missing);
            return placements.findByCandidateIdOrderByCreatedAtDesc(c.getId()).stream().map(p->new CandidateHistory(
                p.getId(),p.getEmployer().getCompanyName(),p.getJob()==null?null:p.getJob().getTitle(),p.getSkill().getName(),
                p.getStatus()==PlacementStatus.REPLACED?PlacementStatus.COMPLETED:p.getStatus(),p.getStartDate(),historyEnd(p))).toList();
        }
        return placements.findAll(filter.placements(u),org.springframework.data.domain.Sort.by("createdAt","id").descending()).stream().map(this::view).toList();
    }
    public record HiringContext(boolean replacement) {}
    @PreAuthorize("hasRole('EMPLOYER')")
    public HiringContext hiringContext(Long jobId) {
        var job=jobs.findOwnedForUpdate(jobId,current.requireActive().getId()).orElseThrow(CandidateService::missing);
        return new HiringContext(replacements.findByFreeReplacementJobId(job.getId()).isPresent());
    }
    private Instant historyEnd(Placement p) {
        if(p.getStatus()==PlacementStatus.ACTIVE || p.getStatus()==PlacementStatus.PENDING) return null;
        if(p.getEndedAt()!=null) return p.getEndedAt();
        if(p.getStatus()==PlacementStatus.REPLACED)
            return replacements.findByPlacementIdOrderByRequestedAtDescIdDesc(p.getId()).stream()
                .filter(r->r.getStatus()==ReplacementStatus.COMPLETED && r.getActualCompletionAt()!=null)
                .map(ReplacementRequest::getActualCompletionAt).findFirst().orElse(null);
        return null;
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public PlacementView create(Long jobId, Long candidateId, boolean guaranteed, PlacementFeeAgreement.Input agreement) {
        var u=current.requireActive();
        catalog.findFirstByOrderByIdAsc().orElseThrow(CandidateService::missing);
        var j=jobs.findOwnedForUpdate(jobId,u.getId()).orElseThrow(CandidateService::missing);
        if(!j.portalOpen()) throw conflict("This job portal is closed.");
        var replacement=outstanding(j).stream().findFirst().orElse(null);
        if(j.getOriginalJob()!=null && replacement==null) throw conflict("No replacement positions remain.");
        if(replacement!=null) requireGuarantee(replacement);
        if(replacement!=null) {
            placements.findByIdForUpdate(replacement.getPlacement().getId()).orElseThrow(CandidateService::missing);
            replacements.findByIdForUpdate(replacement.getId()).orElseThrow(CandidateService::missing);
            em.refresh(replacement);
            em.refresh(replacement.getPlacement());
            if(!List.of(ReplacementStatus.HIRING,ReplacementStatus.WAITING_FOR_CANDIDATE,ReplacementStatus.CANDIDATE_SELECTED,ReplacementStatus.ACCEPTED).contains(replacement.getStatus()) || replacement.getReplacementPlacement()!=null || !replacement.getPlacement().isReplacementSourceEligible()
                || !replacement.getEmployer().getId().equals(j.getEmployer().getId()) || j.getCandidateType()!=replacement.getPlacement().getCandidate().getCandidateType())
                throw conflict("This replacement job is no longer available for hiring.");
        }
        // Release this request's reservation under the same matching/job/placement/request locks.
        // A rejected hire rolls this back, preserving the suggested worker.
        if(replacement!=null) releaseReservation(replacement);
        var c=candidates.findByIdForUpdate(candidateId).orElseThrow(CandidateService::missing);
        em.refresh(c);
        if(c.getAvailability()!=Availability.AVAILABLE) throw conflict("This candidate is currently unavailable for another placement.");
        if(placements.existsByJobIdAndCandidateId(jobId,candidateId))
            throw conflict("This applicant has already been hired for this job. View the existing placement.");
        if(!j.portalOpen() || j.getRequiredSkill()==null || !j.getRequiredSkill().isActive() || !j.getCandidateType().name().equals(j.getRequiredSkill().getCategory())
            || !applications.existsByJobIdAndCandidateIdAndStatus(jobId,candidateId,ApplicationStatus.SHORTLISTED)
            || !shortlists.existsByJobIdAndCandidateId(jobId,candidateId) || c.getCandidateType()!=j.getCandidateType()
            || (c.getCandidateType()!=CandidateType.TRADE && (!skills.existsByCandidateIdAndSkillId(candidateId,j.getRequiredSkill().getId())
            || c.getUser().getRole()!=Role.CANDIDATE || c.getUser().getAccountStatus()!=AccountStatus.ACTIVE
            || c.getAvailability()!=Availability.AVAILABLE || queue.existsByCandidateIdAndStatus(candidateId,QueueStatus.RESERVED))))
            throw conflict("Hire an available, matching shortlisted candidate for an active job with a required skill.");
        if(c.getCandidateType()==CandidateType.TRADE) {
            var readiness=eligibility.check(candidateId,j.getRequiredSkill().getId());
            if(!readiness.eligible()) {
                var reasons=readiness.checks().stream().filter(check->!check.passed())
                    .map(check->tradeHiringFailure(check.code())).distinct().toList();
                throw conflict("Cannot hire this candidate:\n• "+String.join("\n• ",reasons));
            }
        }
        var p=new Placement(); p.setCandidate(c); p.setEmployer(j.getEmployer()); p.setJob(j); p.setSkill(j.getRequiredSkill());
        if(replacement==null) PlacementFeeAgreement.record(p,agreement,u.getId());
        else PlacementFeeAgreement.waiveReplacement(p);
        p.setGuaranteeEligible(replacement==null?guaranteed:replacement.getPlacement().isGuaranteeEligible());
        activate(p,Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        if(replacement!=null) completeReplacement(replacement,p,Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        events.publishEvent(new MarketplaceEvent(u, "PLACEMENT_CREATED", "PLACEMENT",p.getId(),List.of(u,c.getUser()),"Placement active", "Your placement in "+p.getSkill().getName()+" is now active."));
        return view(p);
    }
    public void releaseReservation(ReplacementRequest r) {
        if(r.getSelectedCandidate()==null) return;
        var candidate=candidates.findByIdForUpdate(r.getSelectedCandidate().getId()).orElseThrow(CandidateService::missing);
        var entries=queue.findByCandidateIdAndStatusIn(candidate.getId(),List.of(QueueStatus.RESERVED));
        for(var entry:entries) { entry.setStatus(QueueStatus.QUEUED); entry.setReservedAt(null); }
        queue.flush();
        for(var entry:entries) if(!eligibility.check(candidate.getId(),entry.getSkill().getId()).eligible()) {
            entry.setStatus(QueueStatus.EXITED); entry.setExitReason("No longer eligible after reservation release");
        }
        r.setSelectedCandidate(null); queue.flush(); queueService.synchronize(candidate.getId());
    }
    // Call only after matching -> job -> original placement -> request locks and eligibility checks.
    public void completeReplacement(ReplacementRequest r, Placement p, Instant now) {
        requireGuarantee(r);
        if(r.getReplacementPlacement()!=null || !r.getPlacement().isReplacementSourceEligible()
            || List.of(ReplacementStatus.COMPLETED,ReplacementStatus.CANCELLED,ReplacementStatus.FAILED).contains(r.getStatus()))
            throw conflict("This replacement request has already been resolved.");
        r.setReplacementPlacement(p); r.setSelectedCandidate(p.getCandidate());
        r.setStatus(ReplacementStatus.COMPLETED); r.setActualCompletionAt(now); r.setFailureReason(null);
        r.getPlacement().setStatus(PlacementStatus.REPLACED); if(r.getPlacement().getEndedAt()==null) r.getPlacement().setEndedAt(now);
        if(r.getFreeReplacementJob()!=null) refreshShared(r.getFreeReplacementJob());
        if(r.getTargetCompletionAt()!=null) r.setSlaStatus(now.isAfter(r.getTargetCompletionAt())?SlaStatus.BREACHED:SlaStatus.ON_TIME);
        queueService.synchronize(r.getPlacement().getCandidate().getId());
    }
    private static String tradeHiringFailure(String code) {
        return switch(code) {
            case "TRADE" -> "This candidate is not registered as a Trade candidate.";
            case "ACTIVE_ACCOUNT" -> "This candidate's account is not active for hiring.";
            case "VERIFIED" -> "This Trade candidate has not completed verification.";
            case "TRADE_SKILL" -> "This candidate does not have the required active Trade skill.";
            case "AVAILABLE" -> "This candidate is currently unavailable.";
            case "NOT_RESERVED" -> "This candidate is already reserved for another placement or replacement.";
            default -> "This candidate does not meet a required Trade hiring requirement.";
        };
    }
    public void activate(Placement p, Instant now) {
        var candidate=candidates.findByIdForUpdate(p.getCandidate().getId()).orElseThrow(CandidateService::missing);
        em.refresh(candidate);
        if(candidate.getAvailability()!=Availability.AVAILABLE) throw conflict("This candidate is currently unavailable for another placement.");
        if(p.getJob()!=null && placements.existsByJobIdAndCandidateId(p.getJob().getId(),candidate.getId()))
            throw conflict("This candidate already has a placement for this job.");
        p.setCandidate(candidate);
        candidate.setAvailability(Availability.UNAVAILABLE);
        p.setStatus(PlacementStatus.ACTIVE); p.setStartDate(LocalDate.ofInstant(now,ZoneOffset.UTC));
        if(p.getJob()!=null) {
            var origin=original(p.getJob());
            ensureWindow(origin,now);
            p.setGuaranteeExpiresAt(p.isGuaranteeEligible()?origin.getReplacementWindowExpiresAt():null);
        } else p.setGuaranteeExpiresAt(null);
        if(p.getJob()!=null && !p.getJob().portalOpen()) throw conflict("This job portal is closed.");
        placements.saveAndFlush(p);
        for(var e:queue.findByCandidateIdAndStatusIn(p.getCandidate().getId(),List.of(QueueStatus.QUEUED,QueueStatus.RESERVED))) {
            e.setStatus(QueueStatus.EXITED); e.setExitReason("Placement activated");
        }
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public PlacementView end(Long id, boolean complete) {
        var p=placements.findByIdForUpdate(id).orElseThrow(CandidateService::missing); current.requireOwner(p.getEmployer().getUser().getId());
        candidates.findByIdForUpdate(p.getCandidate().getId()).orElseThrow(CandidateService::missing);
        if(p.getStatus()!=PlacementStatus.ACTIVE || replacements.findByPlacementIdAndActiveRequestTrue(id).isPresent()) throw conflict("Only active placements without an open replacement can be ended.");
        p.setStatus(complete?PlacementStatus.COMPLETED:PlacementStatus.TERMINATED); p.setEndedAt(Instant.now());
        queueService.synchronize(p.getCandidate().getId());
        events.publishEvent(new MarketplaceEvent(current.requireActive(),"PLACEMENT_ENDED","PLACEMENT",id,List.of(p.getEmployer().getUser(),p.getCandidate().getUser()),"Placement ended","Placement #"+id+" is "+p.getStatus().name().toLowerCase()+"."));
        return view(p);
    }
    private CandidateHistory history(Placement p) {
        return new CandidateHistory(p.getId(),p.getEmployer().getCompanyName(),p.getJob()==null?null:p.getJob().getTitle(),
            p.getSkill().getName(),p.getStatus()==PlacementStatus.REPLACED?PlacementStatus.COMPLETED:p.getStatus(),p.getStartDate(),historyEnd(p));
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public List<CandidateHistory> experience(Long jobId, Long applicationId) {
        jobs.findOwnedForUpdate(jobId,current.requireActive().getId()).orElseThrow(CandidateService::missing);
        var application=applications.findByIdAndJobId(applicationId,jobId).orElseThrow(CandidateService::missing);
        return placements.findByCandidateIdOrderByCreatedAtDesc(application.getCandidate().getId()).stream().map(this::history).toList();
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateHistory leave(Long id) {
        var user=current.requireActive();
        var p=placements.findByIdForUpdate(id).orElseThrow(CandidateService::missing);
        current.requireOwner(p.getCandidate().getUser().getId());
        if(p.getStatus()!=PlacementStatus.ACTIVE) throw conflict("Only a currently active placement can be marked as left.");
        var now=Instant.now(); p.setStatus(PlacementStatus.COMPLETED); p.setEndedAt(now); p.setCandidateLeftAt(now);
        events.publishEvent(new MarketplaceEvent(user,"CANDIDATE_LEFT_PLACEMENT","PLACEMENT",id,List.of(p.getEmployer().getUser()),
            "Candidate reported leaving",p.getCandidate().getFullName()+" reported leaving "+(p.getJob()==null?p.getSkill().getName():p.getJob().getTitle())+"."));
        return history(p);
    }
    public void releaseUnhiredApplicants(Job job) {
        for(var a:applications.findByJobIdOrderByCreatedAtDescIdDesc(job.getId())) {
            if(placements.existsByJobIdAndCandidateId(job.getId(),a.getCandidate().getId())) continue;
            shortlists.findByJobIdAndCandidateId(job.getId(),a.getCandidate().getId()).ifPresent(shortlists::delete);
            if(!a.getStatus().isTerminal()) a.setStatus(ApplicationStatus.REJECTED);
        }
    }
    public void closeSelections(Job job) {
        for(var r:outstanding(job)) {
            releaseReservation(r); r.setStatus(ReplacementStatus.CANCELLED); r.setFailureReason("Replacement portal closed.");
        }
        releaseUnhiredApplicants(job);
    }
    public PlacementView view(Placement p) { return new PlacementView(p.getId(),p.getCandidate().getId(),p.getCandidate().getFullName(),p.getEmployer().getCompanyName(),p.getJob()==null?null:p.getJob().getTitle(),p.getSkill().getName(),p.getStatus(),p.getStartDate(),p.isGuaranteeEligible(),p.getGuaranteeExpiresAt(),p.getCandidate().getCandidateType(),p.getJob()==null || original(p.getJob()).getReplacementGuaranteeDaysSnapshot()==null?null:original(p.getJob()).getReplacementGuaranteeDaysSnapshot().longValue(),p.isReplacementSourceEligible(),p.getJob()==null?null:original(p.getJob()).getReplacementWindowStartedAt(),p.getJob()==null?0:replacementNeeded(p.getJob())); }
    public Job original(Job j) { return j.getOriginalJob()==null?j:j.getOriginalJob(); }
    public void ensureWindow(Job origin, Instant now) {
        jobs.findByIdForUpdate(origin.getId()).orElseThrow(CandidateService::missing);
        if(origin.getReplacementWindowStartedAt()!=null) return;
        // Preserve the earliest historical placement's policy when upgrading existing jobs.
        var historical=placements.findByJobIdOrderByStartDateAscIdAsc(origin.getId()).stream().filter(p->p.getStartDate()!=null).findFirst().orElse(null);
        var start=historical==null?now:historical.getStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        var end=historical==null?null:historical.getGuaranteeExpiresAt();
        int days=end==null?(historical==null?guaranteePolicy.days():0):(int)Duration.between(start,end).toDays();
        origin.setReplacementWindowStartedAt(start); origin.setReplacementGuaranteeDaysSnapshot(days);
        origin.setReplacementWindowExpiresAt(end==null?start.plus(Duration.ofDays(days)):end);
        for(var p:placements.findByJobIdOrderByStartDateAscIdAsc(origin.getId()))
            if(p.isGuaranteeEligible()) p.setGuaranteeExpiresAt(origin.getReplacementWindowExpiresAt());
    }
    public void requireGuarantee(ReplacementRequest r) {
        var job=r.getPlacement().getJob();
        if(job==null) throw conflict("This placement has no original job guarantee.");
        var origin=original(job); ensureWindow(origin,Instant.now());
        if(!Instant.now().isBefore(origin.getReplacementWindowExpiresAt())) throw conflict("Replacement guarantee window expired.");
    }
    public List<ReplacementRequest> outstanding(Job job) {
        return replacements.findByFreeReplacementJobIdOrderByRequestedAtAscIdAsc(job.getId()).stream()
            .filter(r->!List.of(ReplacementStatus.COMPLETED,ReplacementStatus.CANCELLED,ReplacementStatus.FAILED).contains(r.getStatus()) && r.getReplacementPlacement()==null).toList();
    }
    public long replacementNeeded(Job job) {
        var origin=original(job);
        if(origin.getReplacementWindowExpiresAt()==null || !Instant.now().isBefore(origin.getReplacementWindowExpiresAt())) return 0;
        var shared=job.getOriginalJob()!=null?job:jobs.findFirstByOriginalJobIdOrderByIdAsc(job.getId()).orElse(null);
        return shared==null?0:outstanding(shared).size();
    }
    public void refreshShared(Job job) {
        if(outstanding(job).isEmpty() || job.closingTime()==null || !Instant.now().isBefore(job.closingTime())) {
            job.setStatus(JobStatus.CLOSED); releaseUnhiredApplicants(job);
        }
    }
    public void upgradeLegacyPortals() {
        if(catalog.findFirstByOrderByIdAsc().isEmpty()) return;
        // Link historical free jobs first, including completed requests, without deleting their history.
        var all=replacements.findAll(org.springframework.data.domain.Sort.by("requestedAt","id"));
        for(var r:all) if(r.getFreeReplacementJob()!=null && r.getFreeReplacementJob().getOriginalJob()==null && r.getPlacement().getJob()!=null)
            r.getFreeReplacementJob().setOriginalJob(original(r.getPlacement().getJob()));
        jobs.flush();
        for(var j:jobs.findByReplacementWindowStartedAtIsNullAndOriginalJobIsNull())
            if(!placements.findByJobIdOrderByStartDateAscIdAsc(j.getId()).isEmpty()) ensureWindow(j,Instant.now());
        for(var r:all) {
            var old=r.getFreeReplacementJob(); if(old==null || old.getOriginalJob()==null) continue;
            var origin=original(old); ensureWindow(origin,Instant.now());
            old.setPortalClosesAt(origin.getReplacementWindowExpiresAt());
            var shared=jobs.findFirstByOriginalJobIdOrderByIdAsc(origin.getId()).orElseThrow();
            if(!old.getId().equals(shared.getId())) {
                old.setStatus(JobStatus.CLOSED); releaseUnhiredApplicants(old);
                // Keep completed historical links; move only the outstanding requests.
                if(!List.of(ReplacementStatus.COMPLETED,ReplacementStatus.CANCELLED,ReplacementStatus.FAILED).contains(r.getStatus())) r.setFreeReplacementJob(shared);
            }
            if(r.getPlacement().isGuaranteeEligible()) r.getPlacement().setGuaranteeExpiresAt(origin.getReplacementWindowExpiresAt());
        }
        replacements.flush();
        for(var r:all) if(r.getFreeReplacementJob()!=null) {
            var shared=r.getFreeReplacementJob();
            if(shared.getOriginalJob()!=null && jobs.findFirstByOriginalJobIdOrderByIdAsc(shared.getOriginalJob().getId()).orElseThrow().getId().equals(shared.getId()) && !outstanding(shared).isEmpty())
                shared.setStatus(JobStatus.ACTIVE);
        }
        expirePortals();
    }
    // The same matching lock used by FIFO and hires serializes cleanup and shared-job creation.
    public void expirePortals() {
        if(catalog.findFirstByOrderByIdAsc().isEmpty()) return;
        for(var reference:jobs.findByStatus(JobStatus.ACTIVE)) {
            var job=jobs.findByIdForUpdate(reference.getId()).orElseThrow(CandidateService::missing);
            em.refresh(job);
            if(job.getActivatedAt()==null && job.getOriginalJob()==null)
                job.setActivatedAt(payments.findFirstByJobIdAndStatusOrderByCompletedAtDesc(job.getId(),com.marketplace.payment.PaymentStatus.SUCCESS)
                    .map(com.marketplace.payment.PaymentTransaction::getCompletedAt).orElse(job.getCreatedAt()));
            if(job.getPortalClosesAt()==null) job.setPortalClosesAt(job.closingTime());
            if(!job.portalOpen()) {
                for(var r:outstanding(job)) {
                    releaseReservation(r); r.setStatus(ReplacementStatus.CANCELLED);
                    r.setFailureReason("Replacement guarantee window expired.");
                }
                job.setStatus(JobStatus.CLOSED); releaseUnhiredApplicants(job);
            }
        }
    }
    public static ApiException conflict(String message) { return new ApiException(409,"INVALID_STATE",message); }
}
