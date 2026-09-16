package com.marketplace.replacement;

import com.marketplace.candidate.*;
import com.marketplace.assessment.*;
import com.marketplace.verification.*;
import com.marketplace.placement.*;
import com.marketplace.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Shared hook for M7 admission/selection. Call under the candidate lock before any queue mutation. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class QueueEligibilityService {
    private final CandidateProfileRepository candidates;
    private final CandidateSkillRepository skills;
    private final VerificationRecordRepository verifications;
    private final EvaluationRepository evaluations;
    private final WaitingListEntryRepository queue;
    private final PlacementRepository placements;
    public record Check(String code, boolean passed) {}
    public record Readiness(boolean eligible, List<Check> checks) {}

    public Readiness check(Long candidateId, Long requiredSkillId) {
        var c=candidates.findById(candidateId).orElseThrow(CandidateService::missing);
        var checks=new ArrayList<Check>();
        checks.add(new Check("TRADE", c.getCandidateType()==CandidateType.TRADE));
        checks.add(new Check("ACTIVE_ACCOUNT", c.getUser().getAccountStatus()==AccountStatus.ACTIVE && c.getUser().getRole()==Role.CANDIDATE));
        checks.add(new Check("VERIFIED",verifications.findFirstByCandidateIdOrderBySubmittedAtDescIdDesc(candidateId)
            .map(v->v.getStatus()==VerificationStatus.VERIFIED).orElse(false)));
        var tradeSkills=skills.findByCandidateId(candidateId).stream().filter(s->s.getSkill().isActive() && "TRADE".equals(s.getSkill().getCategory()))
            .filter(s->requiredSkillId==null || s.getSkill().getId().equals(requiredSkillId)).toList();
        checks.add(new Check("TRADE_SKILL",c.getPrimaryTradeCategory()!=null && !c.getPrimaryTradeCategory().isBlank() && !tradeSkills.isEmpty()));
        // General TRADE reviews apply to all trades; skill-specific reviews only to that skill.
        // Latest relevant released evaluation wins so an old positive result cannot override a newer negative result.
        var latest=evaluations.findByAttemptCandidateIdAndReleasedTrue(candidateId).stream()
            .filter(e->e.getAttempt().getAssessment().getCandidateType()==CandidateType.TRADE)
            .filter(e->e.getAttempt().getAssessment().getSkill()==null || tradeSkills.stream().anyMatch(s->s.getSkill().getId().equals(e.getAttempt().getAssessment().getSkill().getId())))
            .max(Comparator.comparing((Evaluation e)->e.getAttempt().getId()).thenComparing(Evaluation::getId));
        checks.add(new Check("HIRE_READY",latest.map(e->e.getRecommendation()==Recommendation.HIRE_READY).orElse(false)));
        checks.add(new Check("AVAILABLE",c.getAvailability()==Availability.AVAILABLE));
        checks.add(new Check("NOT_RESERVED",!queue.existsByCandidateIdAndStatus(candidateId,QueueStatus.RESERVED)));
        checks.add(new Check("NO_ACTIVE_PLACEMENT",!placements.existsByCandidateIdAndStatusIn(candidateId,List.of(PlacementStatus.PENDING,PlacementStatus.ACTIVE))));
        return new Readiness(checks.stream().allMatch(Check::passed),List.copyOf(checks));
    }
    public boolean canAdmit(Long candidateId, Long skillId) {
        return skillId!=null && check(candidateId,skillId).eligible()
            && !queue.existsByCandidateIdAndSkillIdAndActiveMembershipTrue(candidateId,skillId);
    }
}
