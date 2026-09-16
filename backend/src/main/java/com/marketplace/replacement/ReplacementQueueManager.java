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
        Instant actualCompletionAt, SlaStatus slaStatus, String failureReason) {}
    @PreAuthorize("hasRole('EMPLOYER')")
    public View request(Long placementId, String reason) {
        lockMatching();
        var p=placements.findByIdForUpdate(placementId).orElseThrow(CandidateService::missing);
        current.requireOwner(p.getEmployer().getUser().getId());
        var now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        if(p.getStatus()!=PlacementStatus.ACTIVE || !p.isGuaranteeEligible() || p.getGuaranteeExpiresAt()==null || !now.isBefore(p.getGuaranteeExpiresAt())) throw conflict("This placement has no active replacement coverage.");
        if(requests.findByPlacementIdAndActiveRequestTrue(placementId).isPresent()) throw conflict("A replacement request is already active.");
        var r=new ReplacementRequest(); r.setPlacement(p); r.setEmployer(p.getEmployer()); r.setReason(reason.trim());
        r.setRequestedAt(now); r.setTargetCompletionAt(now.plus(Duration.ofHours(24))); requests.saveAndFlush(r);
        publish(r,"REPLACEMENT_REQUESTED","Replacement requested"); match(r); return view(r);
    }
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER','ADMIN')")
    public List<View> list() { var u=current.requireActive(); return requests.findAll().stream().filter(r->visible(r,u)).sorted(Comparator.comparing(ReplacementRequest::getId).reversed()).map(this::view).toList(); }
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER','ADMIN')")
    public View get(Long id) { var r=requests.findById(id).filter(v->visible(v,current.requireActive())).orElseThrow(CandidateService::missing); return view(r); }
    @PreAuthorize("hasRole('EMPLOYER')")
    public View accept(Long id) {
        lockMatching(); var r=owned(id); if(r.getStatus()!=ReplacementStatus.CANDIDATE_SELECTED) throw conflict("Only a selected candidate can be confirmed.");
        if(!stillEligible(r)) { release(r); match(r); return view(r); }
        r.setStatus(ReplacementStatus.ACCEPTED); publish(r,"REPLACEMENT_ACCEPTED","Replacement confirmed"); return view(r);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public View complete(Long id) {
        lockMatching(); var r=owned(id); if(r.getStatus()!=ReplacementStatus.ACCEPTED) throw conflict("Confirm the selected candidate before activation.");
        if(!stillEligible(r)) { release(r); match(r); return view(r); }
        var now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS); var p=new Placement(); p.setCandidate(r.getSelectedCandidate()); p.setEmployer(r.getEmployer()); p.setJob(r.getPlacement().getJob()); p.setSkill(r.getPlacement().getSkill());
        placementService.activate(p,now); r.getPlacement().setStatus(PlacementStatus.REPLACED);
        r.setReplacementPlacement(p); r.setStatus(ReplacementStatus.COMPLETED); r.setActualCompletionAt(now);
        r.setSlaStatus(now.isAfter(r.getTargetCompletionAt())?SlaStatus.BREACHED:SlaStatus.ON_TIME);
        publish(r,"REPLACEMENT_COMPLETED","Replacement active"); return view(r);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public View cancel(Long id) {
        lockMatching(); var r=owned(id); if(r.getStatus()!=ReplacementStatus.CANDIDATE_SELECTED && r.getStatus()!=ReplacementStatus.ACCEPTED) throw conflict("Only an active selection can be cancelled.");
        publish(r,"REPLACEMENT_CANCELLED","Replacement cancelled"); release(r); fail(r,"Employer cancelled the request."); return view(r);
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public View retry(Long id) {
        lockMatching(); var r=owned(id); if(r.getStatus()!=ReplacementStatus.FAILED) throw conflict("Only a failed request can be retried.");
        if(r.getPlacement().getStatus()!=PlacementStatus.ACTIVE || requests.findByPlacementIdAndActiveRequestTrue(r.getPlacement().getId()).isPresent()) throw conflict("Placement is no longer available for this retry.");
        r.setFailureReason(null); match(r); return view(r);
    }
    private ReplacementRequest owned(Long id) {
        // Always placement before request; this also serializes retry against new requests/end actions.
        var reference=requests.findById(id).orElseThrow(CandidateService::missing);
        current.requireOwner(reference.getEmployer().getUser().getId());
        placements.findByIdForUpdate(reference.getPlacement().getId()).orElseThrow(CandidateService::missing);
        var r=requests.findByIdForUpdate(id).orElseThrow(CandidateService::missing); em.refresh(r); return r;
    }
    private void match(ReplacementRequest r) {
        r.setStatus(ReplacementStatus.MATCHING);
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
        fail(r,"No eligible candidates are currently available in this skill queue.");
    }
    private boolean stillEligible(ReplacementRequest r) {
        var c=candidates.findByIdForUpdate(r.getSelectedCandidate().getId()).orElseThrow(CandidateService::missing); em.refresh(c);
        var reserved=queue.findByCandidateIdAndStatusIn(c.getId(),List.of(QueueStatus.RESERVED));
        return reserved.size()==1 && reserved.getFirst().getSkill().getId().equals(r.getPlacement().getSkill().getId())
            && eligibility.check(c.getId(),r.getPlacement().getSkill().getId()).checks().stream().filter(v->!v.code().equals("NOT_RESERVED")).allMatch(QueueEligibilityService.Check::passed);
    }
    private void release(ReplacementRequest r) {
        var c=candidates.findByIdForUpdate(r.getSelectedCandidate().getId()).orElseThrow(CandidateService::missing);
        var entries=queue.findByCandidateIdAndStatusIn(c.getId(),List.of(QueueStatus.RESERVED));
        for(var e:entries) { e.setStatus(QueueStatus.QUEUED); e.setReservedAt(null); } queue.flush();
        for(var e:entries) if(!eligibility.check(c.getId(),e.getSkill().getId()).eligible()) { e.setStatus(QueueStatus.EXITED); e.setExitReason("No longer eligible after reservation release"); }
        r.setSelectedCandidate(null); queue.flush();
    }
    private void fail(ReplacementRequest r,String reason) { r.setStatus(ReplacementStatus.FAILED); r.setFailureReason(reason); publish(r,"REPLACEMENT_FAILED","Replacement needs attention"); }
    private void publish(ReplacementRequest r,String action,String title) {
        var recipients=new ArrayList<User>(); recipients.add(r.getEmployer().getUser());
        if(r.getSelectedCandidate()!=null) recipients.add(r.getSelectedCandidate().getUser());
        if(r.getStatus()==ReplacementStatus.COMPLETED) recipients.add(r.getPlacement().getCandidate().getUser());
        events.publishEvent(new MarketplaceEvent(current.requireActive(),action,"REPLACEMENT",r.getId(),List.copyOf(recipients),title,"Replacement #"+r.getId()+" · "+r.getPlacement().getSkill().getName()+" · "+r.getStatus().name().toLowerCase().replace('_',' ')));
    }
    private boolean visible(ReplacementRequest r,User u) { return u.getRole()==Role.ADMIN || r.getEmployer().getUser().getId().equals(u.getId()) || r.getPlacement().getCandidate().getUser().getId().equals(u.getId()) || (r.getSelectedCandidate()!=null && r.getSelectedCandidate().getUser().getId().equals(u.getId())); }
    public View view(ReplacementRequest r) {
        var c=r.getSelectedCandidate();
        var sla=r.getSlaStatus()==SlaStatus.PENDING && Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS).isAfter(r.getTargetCompletionAt())?SlaStatus.BREACHED:r.getSlaStatus();
        return new View(r.getId(),placementService.view(r.getPlacement()),r.getReason(),r.getStatus(),c==null?null:new Selected(c.getId(),c.getFullName(),c.getLocation(),r.getPlacement().getSkill().getName()),r.getReplacementPlacement()==null?null:r.getReplacementPlacement().getId(),r.getRequestedAt(),r.getTargetCompletionAt(),r.getActualCompletionAt(),sla,r.getFailureReason());
    }
}
