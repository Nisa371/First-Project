package com.marketplace.candidate;

import com.marketplace.auth.CurrentAccount;
import com.marketplace.common.api.ApiException;
import com.marketplace.job.JobApplicationRepository;
import com.marketplace.user.*;
import java.net.URI;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import static com.marketplace.candidate.StructuredCvDtos.*;

@Service @RequiredArgsConstructor @Transactional
public class StructuredCvService {
    private final CandidateCvRepository cvs;
    private final CandidateProfileRepository candidates;
    private final CandidateService profiles;
    private final CurrentAccount current;
    private final JobApplicationRepository applications;

    @PreAuthorize("hasRole('CANDIDATE')")
    @Transactional(readOnly = true)
    public View own() { return view(profiles.own()); }

    @PreAuthorize("hasRole('EMPLOYER')")
    @Transactional(readOnly = true)
    public View applicant(Long id) {
        if (!applications.existsByCandidateIdAndJobEmployerUserId(id, current.requireActive().getId()))
            throw CandidateService.missing();
        var c = candidates.findById(id).filter(p -> p.getUser().getAccountStatus() == AccountStatus.ACTIVE
            && p.getUser().getRole() == Role.CANDIDATE).orElseThrow(CandidateService::missing);
        return view(c);
    }
    @PreAuthorize("hasRole('CANDIDATE')")
    public View save(Content r) {
        var candidate = candidates.findByIdForUpdate(profiles.own().getId()).orElseThrow(CandidateService::missing);
        url(r.linkedinUrl()); url(r.githubUrl());
        r.education().forEach(e -> dates(e.startDate(), e.endDate(), e.current()));
        r.experience().forEach(e -> dates(e.startDate(), e.endDate(), e.current()));
        r.projects().forEach(e -> { dates(e.startDate(), e.endDate(), false); url(e.projectUrl()); url(e.repositoryUrl()); });
        r.certifications().forEach(e -> url(e.credentialUrl()));
        var cv = cvs.findByCandidateId(candidate.getId()).orElseGet(() -> {
            var created = new CandidateCv(); created.setCandidate(candidate); return created;
        });
        cv.setSummary(r.summary()); cv.setLinkedinUrl(r.linkedinUrl()); cv.setGithubUrl(r.githubUrl());
        cv.getEducation().clear();
        r.education().forEach(e -> cv.getEducation().add(new CvEntries.Education(e.institution(), e.qualification(), e.fieldOfStudy(), e.startDate(), e.endDate(), e.current(), e.grade(), e.description())));
        cv.getExperience().clear();
        r.experience().forEach(e -> cv.getExperience().add(new CvEntries.Experience(e.organization(), e.title(), e.startDate(), e.endDate(), e.current(), e.description())));
        cv.getProjects().clear();
        r.projects().forEach(e -> cv.getProjects().add(new CvEntries.Project(e.name(), e.description(), e.technologies(), e.projectUrl(), e.repositoryUrl(), e.startDate(), e.endDate())));
        cv.getCertifications().clear();
        r.certifications().forEach(e -> cv.getCertifications().add(new CvEntries.Certification(e.name(), e.organization(), e.issueDate(), e.credentialUrl())));
        cv.getLanguages().clear();
        r.languages().forEach(e -> cv.getLanguages().add(new CvEntries.Language(e.name(), e.proficiency())));
        cv.getAchievements().clear();
        r.achievements().forEach(e -> cv.getAchievements().add(new CvEntries.Achievement(e.title(), e.issuer(), e.awardDate(), e.description())));
        cvs.saveAndFlush(cv);
        return view(candidate);
    }
    private View view(CandidateProfile candidate) {
        var cv = cvs.findByCandidateId(candidate.getId()).orElseGet(CandidateCv::new);
        var content = content(cv);
        return new View(new Header(candidate.getFullName(), candidate.getUser().getEmail(), candidate.getPhone(),
            candidate.getLocation(), CandidatePhotoService.url(candidate), candidate.getPortfolioUrl(),
            profiles.skillViews(candidate.getId())), content, cv.getUpdatedAt(), !cv.hasContent());
    }
    public static Content content(CandidateCv cv) {
        return new Content(cv.getSummary(), cv.getLinkedinUrl(), cv.getGithubUrl(),
            cv.getEducation().stream().map(e -> new Education(e.getInstitution(), e.getQualification(), e.getFieldOfStudy(), e.getStartDate(), e.getEndDate(), e.isCurrent(), e.getGrade(), e.getDescription())).toList(),
            cv.getExperience().stream().map(e -> new Experience(e.getOrganization(), e.getTitle(), e.getStartDate(), e.getEndDate(), e.isCurrent(), e.getDescription())).toList(),
            cv.getProjects().stream().map(e -> new Project(e.getName(), e.getDescription(), e.getTechnologies(), e.getProjectUrl(), e.getRepositoryUrl(), e.getStartDate(), e.getEndDate())).toList(),
            cv.getCertifications().stream().map(e -> new Certification(e.getName(), e.getOrganization(), e.getIssueDate(), e.getCredentialUrl())).toList(),
            cv.getLanguages().stream().map(e -> new Language(e.getName(), e.getProficiency())).toList(),
            cv.getAchievements().stream().map(e -> new Achievement(e.getTitle(), e.getIssuer(), e.getAwardDate(), e.getDescription())).toList());
    }
    private void dates(LocalDate start, LocalDate end, boolean current) {
        if ((start != null && end != null && end.isBefore(start)) || (current && end != null))
            throw new ApiException(400, "INVALID_CV_DATES", "End date must follow start date. Current entries must have no end date.");
    }
    static void url(String value) {
        if (value == null || value.isBlank()) return;
        try {
            var uri = URI.create(value);
            if (("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getRawUserInfo() == null) return;
        } catch (IllegalArgumentException ignored) { }
        throw new ApiException(400, "INVALID_CV_URL", "Use a complete http or https URL without credentials.");
    }
}
