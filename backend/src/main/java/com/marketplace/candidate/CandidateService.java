package com.marketplace.candidate;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.common.api.ApiException;
import com.marketplace.skill.SkillRepository;
import com.marketplace.verification.VerificationChecklist;
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
    private final CandidateCvRepository builtCvs;
    private final SkillRepository skills;
    private final VerificationChecklist verifications;
    private final EvaluationRepository evaluations;
    private final com.marketplace.job.JobApplicationRepository applications;

    @PreAuthorize("hasRole('CANDIDATE')")
    public CandidateProfile own() {
        return candidates.findByUserId(current.requireActive().getId()).orElseThrow(() -> missing());
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public ProfileView profile() { return view(own()); }
    @PreAuthorize("hasRole('CANDIDATE')")
    public ProfileView update(ProfileRequest r) {
        var c = candidates.findByIdForUpdate(own().getId()).orElseThrow(CandidateService::missing);
        return applyProfile(c, r);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public ProfileView adminUpdate(Long id, ProfileRequest r) {
        current.requireActive();
        return applyProfile(candidates.findByIdForUpdate(id).orElseThrow(CandidateService::missing), r);
    }
    private ProfileView applyProfile(CandidateProfile c, ProfileRequest r) {
        StructuredCvService.url(r.portfolioUrl());
        c.setFullName(r.fullName().trim()); c.setPhone(r.phone()); c.setLocation(r.location()); c.setBio(r.bio());
        c.setExperienceSummary(r.experienceSummary()); if(r.totalExperienceMonths()!=null) c.setTotalExperienceMonths(r.totalExperienceMonths()); c.setAvailability(r.availability());
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
        return verifications.candidateStatus(id);
    }
    private List<ResultView> results(Long id) {
        return evaluations.findByAttemptCandidateIdAndReleasedTrue(id).stream().map(e -> new ResultView(e.getScore(), e.getRecommendation().name())).toList();
    }
    public ProfileView view(CandidateProfile c) {
        return new ProfileView(c.getId(),c.getCandidateType(),c.getFullName(),c.getPhone(),c.getLocation(),c.getBio(),
            c.getEducationSummary(),c.getExperienceSummary(),c.getTotalExperienceMonths(),c.getAvailability(),c.getPrimaryTradeCategory(),
            c.getPortfolioUrl(),c.getCvOriginalName(),skillViews(c.getId()),verification(c.getId()),results(c.getId()),CandidatePhotoService.url(c),builtCvs.findByCandidateId(c.getId()).map(CandidateCv::hasContent).orElse(false));
    }
    public CandidateCard card(CandidateProfile c) {
        return new CandidateCard(c.getId(),c.getCandidateType(),c.getFullName(),c.getLocation(),c.getBio(),c.getExperienceSummary(),c.getTotalExperienceMonths(),
            c.getAvailability(),c.getPrimaryTradeCategory(),c.getPortfolioUrl(),c.getCvStoredName()!=null,
            skillViews(c.getId()),verification(c.getId()),results(c.getId()),CandidatePhotoService.url(c));
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public SearchPage search(CandidateType type, String location, Long skillId, Availability availability, int page) {
        throw new ApiException(403,"JOB_APPLICATION_REQUIRED","Review applicants through one of your own jobs.");
    }
    @PreAuthorize("hasRole('EMPLOYER')")
    public CandidateCard applicant(Long id) {
        if(!applications.existsByCandidateIdAndJobEmployerUserId(id,current.requireActive().getId())) throw missing();
        return card(candidates.findById(id).orElseThrow(CandidateService::missing));
    }
    public static ApiException missing() { return new ApiException(404,"NOT_FOUND","The requested record was not found."); }
}
