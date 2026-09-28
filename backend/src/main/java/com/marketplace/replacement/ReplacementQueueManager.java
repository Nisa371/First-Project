package com.marketplace.replacement;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.placement.*;
import com.marketplace.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.context.ApplicationEventPublisher;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.util.*;
import static com.marketplace.placement.PlacementService.conflict;

/** Spring singleton orchestrator. Database candidate locks, not JVM/static state, protect reservations. */
@Service
@RequiredArgsConstructor
@Transactional
public class ReplacementQueueManager {
    private final com.marketplace.job.JobRepository jobs;
    private final com.marketplace.verification.VerificationChecklist verifications;
    private final ReplacementRequestRepository requests;
    private final PlacementRepository placements;
    private final PlacementService placementService;
    private final CandidateProfileRepository candidates;
    private final WaitingListEntryRepository queue;
    private final QueueEligibilityService eligibility;
    private final CurrentAccount current;
    private final ApplicationEventPublisher events;
    private final EntityManager em;
    private final com.marketplace.skill.SkillRepository skills;
    // A stable catalog row serializes cross-skill matching/release at MVP scale, including across JVMs.
    private void lockMatching() { skills.findFirstByOrderByIdAsc().orElseThrow(CandidateService::missing); }
    public record Selected(Long id, String name, String location, String skill) {}
    public record View(Long id, PlacementService.PlacementView placement, String reason, ReplacementStatus status,
        Selected selectedCandidate, Long replacementPlacementId, Instant requestedAt, Instant targetCompletionAt,
        Instant actualCompletionAt, SlaStatus slaStatus, String failureReason, Long freeReplacementJobId) {}
    @PreAuthorize("hasRole('EMPLOYER')")
    public View request(Long placementId, String reason) {
        lockMatching();
        var p=placements.findByIdForUpdate(placementId).orElseThrow(CandidateService::missing);
        current.requireOwner(p.getEmployer().getUser().getId());
        var now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        if(p.getStatus()!=PlacementStatus.ACTIVE) throw conflict("Only active placements are eligible for replacement.");
        if(!p.isGuaranteeEligible()) throw conflict("No replacement guarantee is included with this placement.");
        if(p.getGuaranteeExpiresAt()==null) throw conflict("This placement has no stored guarantee deadline.");
        if(!now.isBefore(p.getGuaranteeExpiresAt())) throw conflict("Your free replacement guarantee has expired. Create a new job post to hire another candidate.");
        var existing=requests.findByPlacementIdAndActiveRequestTrue(placementId);
        if(existing.isPresent()) throw conflict("A replacement request is already active for this placement. View or cancel the existing request before creating another.");
        var r=new ReplacementRequest(); r.setPlacement(p); r.setEmployer(p.getEmployer()); r.setReason(reason.trim());
        r.setRequestedAt(now);
        boolean tech=p.getCandidate().getCandidateType()==CandidateType.TECH;
        r.setTargetCompletionAt(tech?null:now.plus(Duration.ofHours(24))); requests.saveAndFlush(r);
        if(tech) createFreeJob(r); else match(r);
        publish(r,"REPLACEMENT_REQUESTED","Replacement requested"); return view(r);
    }
    @PreAuthorize("hasAnyRole('EMPLOYER','ADMIN')")
    public List<View> list() { var u=current.requireActive(); return requests.findAll().stream().filter(r->visible(r,u)).sorted(Comparator.comparing(ReplacementRequest::getId).reversed()).map(this::view).toList(); }
    @PreAuthorize("hasAnyRole('EMPLOYER','ADMIN')")
    public View get(Long id) { var r=requests.findById(id).filter(v->visible(v,current.requireActive())).orElseThrow(CandidateService::missing); return view(r); }
    @PreAuthorize("hasRole('EMPLOYER')")
    public View accept(Long id) {
        lockMatching(); var r=owned(id); requireTrade(r); if(r.getStatus()!=ReplacementStatus.CANDIDATE_SELECTED) throw conflict("Only a selected candidate can be confirmed.");
        if(!stillEligible(r)) { release(r); match(r); return view(r); }
        r.setStatus(ReplacementStatus.ACCEPTED); publish(r,"REPLACEMENT_ACCEPTED","Replacement confirmed"); return view(r);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public View complete(Long id) {
        lockMatching(); var r=owned(id); requireTrade(r); if(r.getStatus()!=ReplacementStatus.ACCEPTED) throw conflict("Confirm the selected candidate before activation.");
        if(!stillEligible(r)) { release(r); match(r); return view(r); }
        var now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS); var p=new Placement(); p.setCandidate(r.getSelectedCandidate()); p.setEmployer(r.getEmployer()); p.setJob(r.getPlacement().getJob()); p.setSkill(r.getPlacement().getSkill());
        p.setGuaranteeEligible(r.getPlacement().isGuaranteeEligible());
        placementService.activate(p,now); r.getPlacement().setStatus(PlacementStatus.REPLACED);
        r.setReplacementPlacement(p); r.setStatus(ReplacementStatus.COMPLETED); r.setActualCompletionAt(now);
        r.setSlaStatus(now.isAfter(r.getTargetCompletionAt())?SlaStatus.BREACHED:SlaStatus.ON_TIME);
        publish(r,"REPLACEMENT_COMPLETED","Replacement active"); return view(r);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public View cancel(Long id) {
        lockMatching(); var r=owned(id);
        if(r.getStatus()==ReplacementStatus.CANCELLED) return view(r);
        if(r.getStatus()==ReplacementStatus.COMPLETED || (r.getStatus()==ReplacementStatus.FAILED && !legacyTechFailure(r))) throw conflict("This request is already resolved.");
        // Historical failed requests no longer own any queue reservation.
        boolean releaseReservation=r.getStatus()!=ReplacementStatus.FAILED;
        if(r.getFreeReplacementJob()!=null) r.getFreeReplacementJob().setStatus(com.marketplace.job.JobStatus.CLOSED);
        r.setStatus(ReplacementStatus.CANCELLED); r.setFailureReason(null);
        publish(r,"REPLACEMENT_CANCELLED","Replacement cancelled");
        if(releaseReservation) release(r);
        return view(r);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public View retry(Long id) {
        lockMatching(); var r=owned(id); requireTrade(r);
        if(r.getStatus()==ReplacementStatus.CANDIDATE_SELECTED || r.getStatus()==ReplacementStatus.ACCEPTED) return view(r);
        if(r.getStatus()!=ReplacementStatus.WAITING_FOR_CANDIDATE) throw conflict("Only a waiting request can be retried.");
        if(r.getPlacement().getStatus()!=PlacementStatus.ACTIVE) throw conflict("Placement is no longer available for this retry.");
        r.setFailureReason(null); match(r); return view(r);
    }
    private ReplacementRequest owned(Long id) {
        // Always placement before request; this also serializes retry against new requests/end actions.
        var reference=requests.findById(id).orElseThrow(CandidateService::missing);
        current.requireOwner(reference.getEmployer().getUser().getId());
        if(reference.getFreeReplacementJob()!=null) jobs.findByIdForUpdate(reference.getFreeReplacementJob().getId()).orElseThrow(CandidateService::missing);
        placements.findByIdForUpdate(reference.getPlacement().getId()).orElseThrow(CandidateService::missing);
        var r=requests.findByIdForUpdate(id).orElseThrow(CandidateService::missing); em.refresh(r); return r;
    }
    private void match(ReplacementRequest r) {
        requireTrade(r);
        r.setFailureReason(null); r.setStatus(ReplacementStatus.MATCHING);
        var entries=queue.findBySkillIdAndStatusOrderByJoinedAtAscIdAsc(r.getPlacement().getSkill().getId(),QueueStatus.QUEUED);
        // Lock candidate IDs in one stable order even when different skill queues overlap.
        entries.stream().map(e->e.getCandidate().getId()).distinct().sorted().forEach(id->{ var c=candidates.findByIdForUpdate(id).orElseThrow(CandidateService::missing); em.refresh(c); });
        for(var entry:entries) {
            em.refresh(entry);
            if(entry.getStatus()!=QueueStatus.QUEUED || !eligibility.check(entry.getCandidate().getId(),entry.getSkill().getId()).eligible()) continue;
            entry.setStatus(QueueStatus.RESERVED); entry.setReservedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)); queue.flush();
            r.setSelectedCandidate(entry.getCandidate()); r.setStatus(ReplacementStatus.CANDIDATE_SELECTED);
            publish(r,"REPLACEMENT_SELECTED","A replacement candidate is ready"); return;
        }
        r.setStatus(ReplacementStatus.WAITING_FOR_CANDIDATE);
        r.setFailureReason("No eligible replacement worker is currently available. This request remains active and matching can continue.");
    }
    private boolean stillEligible(ReplacementRequest r) {
        var c=candidates.findByIdForUpdate(r.getSelectedCandidate().getId()).orElseThrow(CandidateService::missing); em.refresh(c);
        var reserved=queue.findByCandidateIdAndStatusIn(c.getId(),List.of(QueueStatus.RESERVED));
        return reserved.size()==1 && reserved.getFirst().getSkill().getId().equals(r.getPlacement().getSkill().getId())
            && eligibility.check(c.getId(),r.getPlacement().getSkill().getId()).checks().stream().filter(v->!v.code().equals("NOT_RESERVED")).allMatch(QueueEligibilityService.Check::passed);
    }
    private void release(ReplacementRequest r) {
        if(r.getSelectedCandidate()==null) return;
        var c=candidates.findByIdForUpdate(r.getSelectedCandidate().getId()).orElseThrow(CandidateService::missing);
        var entries=queue.findByCandidateIdAndStatusIn(c.getId(),List.of(QueueStatus.RESERVED));
        for(var e:entries) { e.setStatus(QueueStatus.QUEUED); e.setReservedAt(null); } queue.flush();
        for(var e:entries) if(!eligibility.check(c.getId(),e.getSkill().getId()).eligible()) { e.setStatus(QueueStatus.EXITED); e.setExitReason("No longer eligible after reservation release"); }
        r.setSelectedCandidate(null); queue.flush();
    }
    private boolean legacyTechFailure(ReplacementRequest r) {
        return r.getPlacement().getCandidate().getCandidateType()==CandidateType.TECH
            && r.getStatus()==ReplacementStatus.FAILED
            && "No eligible candidates are currently available in this skill queue.".equals(r.getFailureReason());
    }
    private void requireTrade(ReplacementRequest r) {
        if(r.getPlacement().getCandidate().getCandidateType()!=CandidateType.TRADE) throw conflict("Tech replacements use the free replacement job and normal hiring workflow.");
    }
    private void createFreeJob(ReplacementRequest r) {
        if(r.getFreeReplacementJob()!=null) return;
        if(!"VERIFIED".equals(verifications.forUser(r.getEmployer().getUser()).status())) throw conflict("Employer verification is required before posting jobs.");
        var original=r.getPlacement().getJob();
        if(original==null || original.getCandidateType()!=CandidateType.TECH || original.getRequiredSkill()==null || !original.getRequiredSkill().isActive()) throw conflict("The original Tech job must have an active required skill.");
        var job=new com.marketplace.job.Job(); job.setEmployer(r.getEmployer());
        job.setTitle(original.getTitle()); job.setDescription(original.getDescription()); job.setLocation(original.getLocation());
        job.setCandidateType(CandidateType.TECH); job.setRequiredSkill(original.getRequiredSkill());
        job.setPublicExpectations(original.getPublicExpectations()); job.setPrivateExpectations(original.getPrivateExpectations());
        job.setExpectedExperienceMonths(original.getExpectedExperienceMonths());
        job.setCvWeight(original.getCvWeight()); job.setPortfolioWeight(original.getPortfolioWeight());
        job.setExperienceWeight(original.getExperienceWeight()); job.setAssessmentWeight(original.getAssessmentWeight());
        // Only this server-created, placement-validated vacancy bypasses publication payment.
        job.setStatus(com.marketplace.job.JobStatus.ACTIVE);
        r.setFreeReplacementJob(jobs.saveAndFlush(job)); r.setStatus(ReplacementStatus.HIRING);
    }
    private void publish(ReplacementRequest r,String action,String title) {
        var recipients=new ArrayList<User>(); recipients.add(r.getEmployer().getUser());
        if(r.getSelectedCandidate()!=null) recipients.add(r.getSelectedCandidate().getUser());
        if(r.getStatus()==ReplacementStatus.COMPLETED) recipients.add(r.getPlacement().getCandidate().getUser());
        events.publishEvent(new MarketplaceEvent(current.requireActive(),action,"REPLACEMENT",r.getId(),List.copyOf(recipients),title,"Replacement #"+r.getId()+" · "+r.getPlacement().getSkill().getName()+" · "+r.getStatus().name().toLowerCase().replace('_',' ')));
    }
    private boolean visible(ReplacementRequest r,User u) { return u.getRole()==Role.ADMIN || (u.getRole()==Role.EMPLOYER && r.getEmployer().getUser().getId().equals(u.getId())); }
    public View view(ReplacementRequest r) {
        var c=r.getSelectedCandidate();
        boolean tech=r.getPlacement().getCandidate().getCandidateType()==CandidateType.TECH;
        String failureReason=legacyTechFailure(r) ? "This legacy Tech request used queue matching. Cancel it or start a new request from the placement while its guarantee is valid." : r.getFailureReason();
        var sla=r.getPlacement().getCandidate().getCandidateType()==CandidateType.TECH || r.getTargetCompletionAt()==null || r.getStatus()==ReplacementStatus.CANCELLED ? null : r.getSlaStatus()==SlaStatus.PENDING && Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).isAfter(r.getTargetCompletionAt())?SlaStatus.BREACHED:r.getSlaStatus();
        return new View(r.getId(),placementService.view(r.getPlacement()),r.getReason(),r.getStatus(),c==null?null:new Selected(c.getId(),c.getFullName(),c.getLocation(),r.getPlacement().getSkill().getName()),r.getReplacementPlacement()==null?null:r.getReplacementPlacement().getId(),r.getRequestedAt(),tech?null:r.getTargetCompletionAt(),r.getActualCompletionAt(),sla,failureReason,r.getFreeReplacementJob()==null?null:r.getFreeReplacementJob().getId());
    }
}
