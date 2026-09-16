package com.marketplace.replacement;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.skill.SkillRepository;
import com.marketplace.common.api.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class QueueService {
    private final jakarta.persistence.EntityManager em;
    private final CurrentAccount current;
    private final CandidateProfileRepository candidates;
    private final WaitingListEntryRepository entries;
    private final SkillRepository skills;
    private final QueueEligibilityService eligibility;
    public record QueueView(Long id, Long candidateId, String candidateName, Long skillId, String skill,
        QueueStatus status, Instant joinedAt, Instant reservedAt, Integer position, String exitReason) {}
    @PreAuthorize("hasRole('CANDIDATE')")
    public List<QueueView> mine() { return entries.findByCandidateIdOrderByJoinedAtDescIdDesc(own().getId()).stream().map(this::view).toList(); }
    @PreAuthorize("hasRole('ADMIN')")
    public List<QueueView> all() { return entries.findAllByOrderByJoinedAtAscIdAsc().stream().filter(e->e.getStatus()!=QueueStatus.EXITED).map(this::view).toList(); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public QueueView join(Long skillId) {
        var c=own(); candidates.findByIdForUpdate(c.getId()).orElseThrow(CandidateService::missing); em.refresh(c);
        if(!eligibility.canAdmit(c.getId(),skillId)) throw new ApiException(409,"NOT_QUEUE_READY","Complete readiness checks and remove duplicate membership before joining this skill queue.");
        var e=new WaitingListEntry(); e.setCandidate(c); e.setSkill(skills.findById(skillId).orElseThrow(CandidateService::missing));
        e.setJoinedAt(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)); return view(entries.saveAndFlush(e));
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public void leave(Long id) {
        var c=own(); candidates.findByIdForUpdate(c.getId()).orElseThrow(CandidateService::missing); em.refresh(c);
        var e=entries.findByIdForUpdate(id).filter(v->v.getCandidate().getId().equals(c.getId())).orElseThrow(CandidateService::missing);
        if(e.getStatus()!=QueueStatus.QUEUED) throw new ApiException(409,"QUEUE_STATE","Only a queued entry can be withdrawn. Contact the employer about a reservation.");
        e.setStatus(QueueStatus.EXITED); e.setExitReason("Candidate withdrew");
    }
    private CandidateProfile own() { return candidates.findByUserId(current.requireActive().getId()).orElseThrow(CandidateService::missing); }
    public QueueView view(WaitingListEntry e) {
        Integer position=null;
        if(e.getStatus()==QueueStatus.QUEUED) {
            var ordered=entries.findBySkillIdAndStatusOrderByJoinedAtAscIdAsc(e.getSkill().getId(),QueueStatus.QUEUED);
            for(int i=0;i<ordered.size();i++) if(ordered.get(i).getId().equals(e.getId())) position=i+1;
        }
        return new QueueView(e.getId(),e.getCandidate().getId(),e.getCandidate().getFullName(),e.getSkill().getId(),e.getSkill().getName(),e.getStatus(),e.getJoinedAt(),e.getReservedAt(),position,e.getExitReason());
    }
}
