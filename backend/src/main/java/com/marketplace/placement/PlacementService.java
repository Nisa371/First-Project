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
    private final CandidateProfileRepository candidates;
    private final CandidateSkillRepository skills;
    private final JobRepository jobs;
    private final ShortlistEntryRepository shortlists;
    private final JobApplicationRepository applications;
    private final WaitingListEntryRepository queue;
    private final ReplacementRequestRepository replacements;
    private final QueueEligibilityService eligibility;
    private final CurrentAccount current;
    private final ApplicationEventPublisher events;
    public record PlacementView(Long id, Long candidateId, String candidateName, String company, String job,
        String skill, PlacementStatus status, LocalDate startDate, boolean guaranteeEligible, Instant guaranteeExpiresAt) {}
    @PreAuthorize("hasAnyRole('CANDIDATE','EMPLOYER','ADMIN')")
    public List<PlacementView> mine() {
        var u=current.requireActive();
        return placements.findAll().stream().filter(p->u.getRole()==Role.ADMIN || p.getCandidate().getUser().getId().equals(u.getId()) || p.getEmployer().getUser().getId().equals(u.getId())).map(this::view).toList();
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public PlacementView create(Long jobId, Long candidateId) {
        var u=current.requireActive();
        var j=jobs.findOwnedForUpdate(jobId,u.getId()).orElseThrow(CandidateService::missing);
        var c=candidates.findByIdForUpdate(candidateId).orElseThrow(CandidateService::missing);
        if(j.getStatus()!=JobStatus.ACTIVE || j.getRequiredSkill()==null || !j.getRequiredSkill().isActive()
            || !applications.existsByJobIdAndCandidateIdAndStatus(jobId,candidateId,ApplicationStatus.SHORTLISTED)
            || !shortlists.existsByJobIdAndCandidateId(jobId,candidateId) || c.getCandidateType()!=j.getCandidateType()
            || !skills.existsByCandidateIdAndSkillId(candidateId,j.getRequiredSkill().getId())
            || c.getUser().getRole()!=Role.CANDIDATE || c.getUser().getAccountStatus()!=AccountStatus.ACTIVE
            || c.getAvailability()!=Availability.AVAILABLE || queue.existsByCandidateIdAndStatus(candidateId,QueueStatus.RESERVED)
            || placements.existsByCandidateIdAndStatusIn(candidateId,List.of(PlacementStatus.PENDING,PlacementStatus.ACTIVE)))
            throw conflict("Hire an available, matching shortlisted candidate for an active job with a required skill.");
        if(c.getCandidateType()==CandidateType.TRADE && !eligibility.check(candidateId,j.getRequiredSkill().getId()).eligible())
            throw conflict("TRADE placement requires verified, hire-ready eligibility.");
        var p=new Placement(); p.setCandidate(c); p.setEmployer(j.getEmployer()); p.setJob(j); p.setSkill(j.getRequiredSkill());
        activate(p,Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        events.publishEvent(new MarketplaceEvent(u, "PLACEMENT_CREATED", "PLACEMENT",p.getId(),List.of(u,c.getUser()),"Placement active", "Your placement in "+p.getSkill().getName()+" is now active."));
        return view(p);
    }
    public void activate(Placement p, Instant now) {
        p.setStatus(PlacementStatus.ACTIVE); p.setStartDate(LocalDate.ofInstant(now,ZoneOffset.UTC));
        p.setGuaranteeEligible(p.getCandidate().getCandidateType()==CandidateType.TRADE);
        p.setGuaranteeExpiresAt(p.isGuaranteeEligible()?now.plus(Duration.ofDays(30)):null);
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
        p.setStatus(complete?PlacementStatus.COMPLETED:PlacementStatus.TERMINATED);
        events.publishEvent(new MarketplaceEvent(current.requireActive(),"PLACEMENT_ENDED","PLACEMENT",id,List.of(p.getEmployer().getUser(),p.getCandidate().getUser()),"Placement ended","Placement #"+id+" is "+p.getStatus().name().toLowerCase()+"."));
        return view(p);
    }
    public PlacementView view(Placement p) { return new PlacementView(p.getId(),p.getCandidate().getId(),p.getCandidate().getFullName(),p.getEmployer().getCompanyName(),p.getJob()==null?null:p.getJob().getTitle(),p.getSkill().getName(),p.getStatus(),p.getStartDate(),p.isGuaranteeEligible(),p.getGuaranteeExpiresAt()); }
    public static ApiException conflict(String message) { return new ApiException(409,"INVALID_STATE",message); }
}
