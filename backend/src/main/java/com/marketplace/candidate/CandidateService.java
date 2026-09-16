package com.marketplace.candidate;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.common.api.ApiException;
import com.marketplace.skill.SkillRepository;
import com.marketplace.verification.VerificationRecordRepository;
import com.marketplace.assessment.EvaluationRepository;
import com.marketplace.user.AccountStatus;
import com.marketplace.user.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import static com.marketplace.candidate.CandidateDtos.*;

@Service
@RequiredArgsConstructor
@Transactional
public class CandidateService {
    private final CurrentAccount current;
    private final CandidateProfileRepository candidates;
    private final CandidateSkillRepository candidateSkills;
    private final SkillRepository skills;
    private final VerificationRecordRepository verifications;
    private final EvaluationRepository evaluations;

    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateProfile own() {
        return candidates.findByUserId(current.requireActive().getId()).orElseThrow(() -> missing());
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public ProfileView profile() { return view(own()); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public ProfileView update(ProfileRequest r) {
        var c = candidates.findByIdForUpdate(own().getId()).orElseThrow(CandidateService::missing);
        c.setFullName(r.fullName().trim()); c.setPhone(r.phone()); c.setLocation(r.location()); c.setBio(r.bio());
        c.setExperienceSummary(r.experienceSummary()); c.setAvailability(r.availability());
        if (c.getCandidateType() == CandidateType.TECH) {
            c.setEducationSummary(r.educationSummary()); c.setPortfolioUrl(r.portfolioUrl());
        } else { c.setPrimaryTradeCategory(r.primaryTradeCategory()); }
        return view(c);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public ProfileView addSkill(SkillRequest r) {
        var c = candidates.findByIdForUpdate(own().getId()).orElseThrow(CandidateService::missing);
        var skill = skills.findById(r.skillId()).filter(s -> s.isActive()).orElseThrow(() -> missing());
        if (candidateSkills.existsByCandidateIdAndSkillId(c.getId(), skill.getId()))
            throw new ApiException(409,"DUPLICATE_SKILL","This skill is already on your profile.");
        var link = new CandidateSkill(); link.setCandidate(c); link.setSkill(skill); link.setProficiencyLevel(r.proficiencyLevel());
        candidateSkills.saveAndFlush(link); return view(c);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public ProfileView removeSkill(Long id) {
        var c = candidates.findByIdForUpdate(own().getId()).orElseThrow(CandidateService::missing);
        candidateSkills.findByCandidateId(c.getId()).stream().filter(s -> s.getSkill().getId().equals(id)).forEach(candidateSkills::delete);
        candidateSkills.flush(); return view(c);
    }
    public List<SkillView> skillViews(Long id) {
        return candidateSkills.findByCandidateId(id).stream().map(s -> new SkillView(s.getSkill().getId(),
            s.getSkill().getName(), s.getSkill().getCategory(), s.getProficiencyLevel())).toList();
    }
    private String verification(Long id) {
        return verifications.findFirstByCandidateIdOrderBySubmittedAtDescIdDesc(id).map(v -> v.getStatus().name()).orElse("NOT_SUBMITTED");
    }
    private List<ResultView> results(Long id) {
        return evaluations.findByAttemptCandidateIdAndReleasedTrue(id).stream().map(e -> new ResultView(e.getScore(), e.getRecommendation().name())).toList();
    }
    public ProfileView view(CandidateProfile c) {
        return new ProfileView(c.getId(),c.getCandidateType(),c.getFullName(),c.getPhone(),c.getLocation(),c.getBio(),
            c.getEducationSummary(),c.getExperienceSummary(),c.getAvailability(),c.getPrimaryTradeCategory(),
            c.getPortfolioUrl(),c.getCvOriginalName(),skillViews(c.getId()),verification(c.getId()),results(c.getId()));
    }
    public CandidateCard card(CandidateProfile c) {
        return new CandidateCard(c.getId(),c.getCandidateType(),c.getFullName(),c.getLocation(),c.getBio(),c.getExperienceSummary(),
            c.getAvailability(),c.getPrimaryTradeCategory(),c.getPortfolioUrl(),c.getCvStoredName()!=null,
            skillViews(c.getId()),verification(c.getId()),results(c.getId()));
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public SearchPage search(CandidateType type, String location, Long skillId, Availability availability, int page) {
        current.requireActive();
        if(page<0) throw new ApiException(400,"INVALID_PAGE","Page must be zero or greater.");
        Specification<CandidateProfile> spec = (root,q,cb) -> cb.and(
            cb.equal(root.get("user").get("accountStatus"),AccountStatus.ACTIVE),cb.equal(root.get("user").get("role"),Role.CANDIDATE));
        if(type!=null) spec=spec.and((root,q,cb)->cb.equal(root.get("candidateType"),type));
        if(availability!=null) spec=spec.and((root,q,cb)->cb.equal(root.get("availability"),availability));
        if(location!=null && !location.isBlank()) {
            String literal=location.trim().toLowerCase(java.util.Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
            spec=spec.and((root,q,cb)->cb.like(cb.lower(root.get("location")),"%"+literal+"%",'!'));
        }
        if(skillId!=null) spec=spec.and((root,q,cb)-> {
            var sub=q.subquery(Long.class); var link=sub.from(CandidateSkill.class);
            sub.select(link.get("candidate").get("id")).where(cb.equal(link.get("skill").get("id"),skillId));
            return root.get("id").in(sub);
        });
        var result=candidates.findAll(spec,PageRequest.of(page,12,Sort.by("id").descending()));
        return new SearchPage(result.getContent().stream().map(this::card).toList(),result.getTotalElements(),page,result.getTotalPages());
    }
    public static ApiException missing() { return new ApiException(404,"NOT_FOUND","The requested record was not found."); }
}
