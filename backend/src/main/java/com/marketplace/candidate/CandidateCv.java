package com.marketplace.candidate;

import com.marketplace.common.persistence.TimestampedEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity @Table(name = "candidate_cvs") @Getter @Setter
public class CandidateCv extends TimestampedEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private CandidateProfile candidate;
    @Column(length = 4000) private String summary;
    @Column(length = 2048) private String linkedinUrl;
    @Column(length = 2048) private String githubUrl;
    @Version private long version;
    @ElementCollection
    @CollectionTable(name = "cv_education", joinColumns = @JoinColumn(name = "cv_id"))
    @OrderColumn(name = "display_order")
    private List<CvEntries.Education> education = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "cv_experience", joinColumns = @JoinColumn(name = "cv_id"))
    @OrderColumn(name = "display_order")
    private List<CvEntries.Experience> experience = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "cv_projects", joinColumns = @JoinColumn(name = "cv_id"))
    @OrderColumn(name = "display_order")
    private List<CvEntries.Project> projects = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "cv_certifications", joinColumns = @JoinColumn(name = "cv_id"))
    @OrderColumn(name = "display_order")
    private List<CvEntries.Certification> certifications = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "cv_languages", joinColumns = @JoinColumn(name = "cv_id"))
    @OrderColumn(name = "display_order")
    private List<CvEntries.Language> languages = new ArrayList<>();
    @ElementCollection
    @CollectionTable(name = "cv_achievements", joinColumns = @JoinColumn(name = "cv_id"))
    @OrderColumn(name = "display_order")
    private List<CvEntries.Achievement> achievements = new ArrayList<>();
    public boolean hasContent() {
        return (summary != null && !summary.isBlank()) || !education.isEmpty() || !experience.isEmpty()
            || !projects.isEmpty() || !certifications.isEmpty() || !languages.isEmpty() || !achievements.isEmpty();
    }
}
