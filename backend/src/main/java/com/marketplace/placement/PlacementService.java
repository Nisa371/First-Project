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
@Transactional
public class PlacementService {
    private final PlacementRepository placements;
    private final com.marketplace.skill.SkillRepository catalog;
    private final jakarta.persistence.EntityManager em;
    private final ReplacementGuaranteePolicyService guaranteePolicy;
    private final CandidateProfileRepository candidates;
    private final CandidateSkillRepository skills;
    private final JobRepository jobs;
    private final ShortlistEntryRepository shortlists;
    private final JobApplicationRepository applications;
    private final WaitingListEntryRepository queue;
    private final ReplacementRequestRepository replacements;
    private final QueueEligibilityService eligibility;
    private final QueueService queueService;
    private final CurrentAccount current;
    private final ApplicationEventPublisher events;
    public record PlacementView(Long id, Long candidateId, String candidateName, String company, String job,
        String skill, PlacementStatus status, LocalDate startDate, boolean guaranteeEligible, Instant guaranteeExpiresAt, CandidateType candidateType, Long guaranteeDays, boolean replacementSourceEligible) {}
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
        var replacement=replacements.findByFreeReplacementJobId(jobId).orElse(null);
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
        if(j.getStatus()!=JobStatus.ACTIVE || j.getRequiredSkill()==null || !j.getRequiredSkill().isActive() || !j.getCandidateType().name().equals(j.getRequiredSkill().getCategory())
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
        if(r.getReplacementPlacement()!=null || !r.getPlacement().isReplacementSourceEligible()
            || List.of(ReplacementStatus.COMPLETED,ReplacementStatus.CANCELLED,ReplacementStatus.FAILED).contains(r.getStatus()))
            throw conflict("This replacement request has already been resolved.");
        r.setReplacementPlacement(p); r.setSelectedCandidate(p.getCandidate());
        r.setStatus(ReplacementStatus.COMPLETED); r.setActualCompletionAt(now); r.setFailureReason(null);
        r.getPlacement().setStatus(PlacementStatus.REPLACED); if(r.getPlacement().getEndedAt()==null) r.getPlacement().setEndedAt(now);
        if(r.getFreeReplacementJob()!=null) { r.getFreeReplacementJob().setStatus(JobStatus.CLOSED); releaseUnhiredApplicants(r.getFreeReplacementJob()); }
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
        p.setGuaranteeExpiresAt(p.isGuaranteeEligible()?p.getStartDate().plusDays(guaranteePolicy.days()).atStartOfDay(ZoneOffset.UTC).toInstant():null);
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
        var r=replacements.findByFreeReplacementJobId(job.getId()).orElse(null);
        if(r!=null) {
            placements.findByIdForUpdate(r.getPlacement().getId()).orElseThrow(CandidateService::missing);
            replacements.findByIdForUpdate(r.getId()).orElseThrow(CandidateService::missing); em.refresh(r);
            if(!List.of(ReplacementStatus.COMPLETED,ReplacementStatus.CANCELLED,ReplacementStatus.FAILED).contains(r.getStatus())) {
                releaseReservation(r); r.setStatus(ReplacementStatus.CANCELLED); r.setFailureReason(null);
            }
        }
        releaseUnhiredApplicants(job);
    }
    public PlacementView view(Placement p) { return new PlacementView(p.getId(),p.getCandidate().getId(),p.getCandidate().getFullName(),p.getEmployer().getCompanyName(),p.getJob()==null?null:p.getJob().getTitle(),p.getSkill().getName(),p.getStatus(),p.getStartDate(),p.isGuaranteeEligible(),p.getGuaranteeExpiresAt(),p.getCandidate().getCandidateType(),p.getGuaranteeExpiresAt()==null?null:java.time.temporal.ChronoUnit.DAYS.between(p.getStartDate(),LocalDate.ofInstant(p.getGuaranteeExpiresAt(),ZoneOffset.UTC)),p.isReplacementSourceEligible()); }
    public static ApiException conflict(String message) { return new ApiException(409,"INVALID_STATE",message); }
}
