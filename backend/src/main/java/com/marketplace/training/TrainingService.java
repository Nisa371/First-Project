package com.marketplace.training;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.candidate.*;
import com.marketplace.common.api.ApiException;
import com.marketplace.replacement.MarketplaceEvent;
import com.marketplace.skill.*;
import com.marketplace.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import java.util.*;
import java.time.Instant;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class TrainingService {
    private final CurrentAccount account;
    private final TrainingProgramRepository programs;
    private final ReferralRepository referrals;
    private final CandidateProfileRepository candidates;
    private final CandidateSkillRepository candidateSkills;
    private final SkillRepository skills;
    private final ApplicationEventPublisher events;
    public record SkillView(Long id,String name,String category,boolean active) {}
    public record Program(Long id,String title,String providerName,String description,boolean active,List<SkillView> skills) {}
    public record ReferralView(Long id,String candidate,Program program,ReferralStatus status,Instant createdAt) {}
    public record CandidateView(Long candidateId,String fullName,String phone,CandidateType candidateType,String location,Availability availability,List<SkillView> skills) {}
    private User staff() { var u=account.requireActive(); if(u.getRole()!=Role.ADMIN && u.getRole()!=Role.EVALUATOR) throw new ApiException(403,"FORBIDDEN","Evaluator or admin access required."); return u; }
    private void admin() { if(staff().getRole()!=Role.ADMIN) throw new ApiException(403,"FORBIDDEN","Only administrators can manage training programs."); }
    private SkillView view(Skill s) { return new SkillView(s.getId(),s.getName(),s.getCategory(),s.isActive()); }
    private Program view(TrainingProgram p) { return new Program(p.getId(),p.getTitle(),p.getProviderName(),p.getDescription(),p.isActive(),p.getSkills().stream().sorted(Comparator.comparing(Skill::getName)).map(this::view).toList()); }
    private ReferralView view(Referral r) { return new ReferralView(r.getId(),r.getCandidate().getFullName(),view(r.getTrainingProgram()),r.getStatus(),r.getCreatedAt()); }
    public List<Program> catalog() {
        var u=account.requireActive();
        String track=u.getRole()==Role.CANDIDATE ? candidates.findByUserId(u.getId()).orElseThrow(CandidateService::missing).getCandidateType().name() : null;
        return (u.getRole()==Role.ADMIN ? programs.findAll(org.springframework.data.domain.Sort.by("title")) : programs.findByActiveTrueOrderByTitleAsc()).stream()
            .filter(p -> track==null || p.getSkills().stream().anyMatch(s -> track.equals(s.getCategory())))
            .map(this::view).toList();
    }
    @Transactional public Program save(Long id,TrainingController.ProgramInput input) {
        admin();
        var p=id==null?new TrainingProgram():programs.findByIdForUpdate(id).orElseThrow(()->new ApiException(404,"NOT_FOUND","Training program not found."));
        var selected=skills.findAllById(input.skillIds());
        if(selected.size()!=input.skillIds().size() || selected.stream().anyMatch(s->!s.isActive()))
            throw new ApiException(400,"INVALID_SKILLS","Choose one or more active skills.");
        p.setTitle(input.title().strip()); p.setProviderName(input.providerName().strip()); p.setDescription(input.description().strip());
        p.setActive(input.active()); p.setSkills(new LinkedHashSet<>(selected)); p.setSkill(null);
        return view(programs.save(p));
    }
    public List<CandidateView> search(String name,String phone,CandidateType track,Long skillId,int page) {
        staff(); if(page<0 || page>100000) throw new ApiException(400,"INVALID_PAGE","Choose a valid page.");
        return candidates.searchForTraining(name.strip(),phone.strip(),track,skillId,PageRequest.of(page,20)).stream()
            .map(c->new CandidateView(c.getId(),c.getFullName(),c.getPhone(),c.getCandidateType(),c.getLocation(),c.getAvailability(),
                candidateSkills.findByCandidateId(c.getId()).stream().map(cs->view(cs.getSkill())).toList())).toList();
    }
    public List<ReferralView> list() { var u=account.requireActive(); if(u.getRole()==Role.CANDIDATE) { var c=candidates.findByUserId(u.getId()).orElseThrow(); return referrals.findByCandidateIdOrderByCreatedAtDesc(c.getId()).stream().map(this::view).toList(); } staff(); return referrals.findAll(org.springframework.data.domain.Sort.by("createdAt").descending()).stream().filter(r->u.getRole()==Role.ADMIN || r.getCreatedByUser().getId().equals(u.getId())).map(this::view).toList(); }
    @Transactional public ReferralView refer(Long candidateId,Long programId) {
        var u=staff();
        var c=candidates.findByIdForUpdate(candidateId).orElseThrow(()->new ApiException(404,"NOT_FOUND","Candidate not found."));
        if(c.getUser().getRole()!=Role.CANDIDATE || c.getUser().getAccountStatus()!=AccountStatus.ACTIVE)
            throw new ApiException(400,"REFERRAL_NOT_ALLOWED","Choose an active candidate.");
        var p=programs.findByIdForUpdate(programId).filter(TrainingProgram::isActive).orElseThrow(()->new ApiException(404,"NOT_FOUND","Active training program not found."));
        if(!p.getSkills().isEmpty() && p.getSkills().stream().noneMatch(s->s.getCategory().equals(c.getCandidateType().name())))
            throw new ApiException(400,"TRACK_MISMATCH","Choose a program for the candidate’s track.");
        if(referrals.findByCandidateIdOrderByCreatedAtDesc(c.getId()).stream().anyMatch(r->r.getTrainingProgram().getId().equals(p.getId()) && r.getStatus()!=ReferralStatus.CANCELLED && r.getStatus()!=ReferralStatus.COMPLETED)) throw new ApiException(409,"DUPLICATE_REFERRAL","This candidate already has an active referral to this program.");
        var r=new Referral(); r.setCandidate(c); r.setTrainingProgram(p); r.setCreatedByUser(u); referrals.save(r);
        events.publishEvent(new MarketplaceEvent(u,"TRAINING_REFERRED","REFERRAL",r.getId(),List.of(c.getUser()),"Your next learning step", "You have been referred to "+p.getTitle()+". Open Training to see the program.")); return view(r);
    }
}
