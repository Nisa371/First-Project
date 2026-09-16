package com.marketplace.training;
import com.marketplace.auth.CurrentAccount;
import com.marketplace.assessment.*;
import com.marketplace.candidate.*;
import com.marketplace.common.api.ApiException;
import com.marketplace.replacement.MarketplaceEvent;
import com.marketplace.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import java.time.Instant;
@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class TrainingService {
    private final CurrentAccount account;
    private final TrainingProgramRepository programs;
    private final ReferralRepository referrals;
    private final EvaluationRepository evaluations;
    private final CandidateProfileRepository candidates;
    private final ApplicationEventPublisher events;
    public record Program(Long id,String title,String providerName,String description,Long skillId,String skill) {}
    public record ReferralView(Long id,String candidate,Program program,ReferralStatus status,Instant createdAt) {}
    public record Eligible(Long evaluationId,String candidate,String assessment) {}
    private User staff() { var u=account.requireActive(); if(u.getRole()!=Role.ADMIN && u.getRole()!=Role.EVALUATOR) throw new ApiException(403,"FORBIDDEN","Evaluator or admin access required."); return u; }
    private Program view(TrainingProgram p) { return new Program(p.getId(),p.getTitle(),p.getProviderName(),p.getDescription(),p.getSkill()==null?null:p.getSkill().getId(),p.getSkill()==null?null:p.getSkill().getName()); }
    private ReferralView view(Referral r) { return new ReferralView(r.getId(),r.getCandidate().getFullName(),view(r.getTrainingProgram()),r.getStatus(),r.getCreatedAt()); }
    public List<Program> catalog() { account.requireActive(); return programs.findByActiveTrueOrderByTitleAsc().stream().map(this::view).toList(); }
    public List<Eligible> eligible() { var u=staff(); return evaluations.findAll().stream().filter(e->eligible(e,u)).map(e->new Eligible(e.getId(),e.getAttempt().getCandidate().getFullName(),e.getAttempt().getAssessment().getTitle())).toList(); }
    private boolean eligible(Evaluation e,User u) { return e.isReleased() && e.getRecommendation()==Recommendation.NEEDS_TRAINING && e.getAttempt().getCandidate().getUser().getAccountStatus()==AccountStatus.ACTIVE && (u.getRole()==Role.ADMIN || e.getEvaluatorUser()!=null && e.getEvaluatorUser().getId().equals(u.getId())); }
    public List<ReferralView> list() { var u=account.requireActive(); if(u.getRole()==Role.CANDIDATE) { var c=candidates.findByUserId(u.getId()).orElseThrow(); return referrals.findByCandidateIdOrderByCreatedAtDesc(c.getId()).stream().map(this::view).toList(); } staff(); return referrals.findAll(org.springframework.data.domain.Sort.by("createdAt").descending()).stream().filter(r->u.getRole()==Role.ADMIN || r.getCreatedByUser().getId().equals(u.getId())).map(this::view).toList(); }
    @Transactional public ReferralView refer(Long evaluationId,Long programId) {
        var u=staff(); var e=evaluations.findById(evaluationId).orElseThrow(()->new ApiException(404,"NOT_FOUND","Evaluation not found."));
        if(!eligible(e,u)) throw new ApiException(403,"REFERRAL_NOT_ALLOWED","Use your released Needs training evaluation for an active candidate.");
        var c=candidates.findByIdForUpdate(e.getAttempt().getCandidate().getId()).orElseThrow();
        var p=programs.findById(programId).filter(TrainingProgram::isActive).orElseThrow(()->new ApiException(404,"NOT_FOUND","Active training program not found."));
        if(p.getSkill()!=null && !p.getSkill().getCategory().equals(c.getCandidateType().name())) throw new ApiException(400,"TRACK_MISMATCH","Choose a program for the candidate’s track.");
        if(referrals.findByCandidateIdOrderByCreatedAtDesc(c.getId()).stream().anyMatch(r->r.getTrainingProgram().getId().equals(p.getId()) && r.getStatus()!=ReferralStatus.CANCELLED && r.getStatus()!=ReferralStatus.COMPLETED)) throw new ApiException(409,"DUPLICATE_REFERRAL","This candidate already has an active referral to this program.");
        var r=new Referral(); r.setCandidate(c); r.setTrainingProgram(p); r.setCreatedByUser(u); referrals.save(r);
        events.publishEvent(new MarketplaceEvent(u,"TRAINING_REFERRED","REFERRAL",r.getId(),List.of(c.getUser()),"Your next learning step", "You have been referred to "+p.getTitle()+". Open Training to see the program.")); return view(r);
    }
}
