package com.marketplace.candidate;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

public final class CvEntries {
    private CvEntries() {}
    @Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Education {
        @Column(name = "institution", length = 160, nullable = false) private String institution;
        @Column(name = "qualification", length = 160, nullable = false) private String qualification;
        @Column(name = "field_of_study", length = 160) private String fieldOfStudy;
        @Column(name = "start_date") private LocalDate startDate;
        @Column(name = "end_date") private LocalDate endDate;
        @Column(name = "is_current", nullable = false) private boolean current;
        @Column(name = "grade", length = 80) private String grade;
        @Column(name = "description", length = 3000) private String description;
    }
    @Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Experience {
        @Column(name = "organization", length = 160, nullable = false) private String organization;
        @Column(name = "title", length = 160, nullable = false) private String title;
        @Column(name = "start_date") private LocalDate startDate;
        @Column(name = "end_date") private LocalDate endDate;
        @Column(name = "is_current", nullable = false) private boolean current;
        @Column(name = "description", length = 3000) private String description;
    }
    @Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Project {
        @Column(name = "name", length = 160, nullable = false) private String name;
        @Column(name = "description", length = 3000) private String description;
        @Column(name = "technologies", length = 500) private String technologies;
        @Column(name = "project_url", length = 2048) private String projectUrl;
        @Column(name = "repository_url", length = 2048) private String repositoryUrl;
        @Column(name = "start_date") private LocalDate startDate;
        @Column(name = "end_date") private LocalDate endDate;
    }
    @Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Certification {
        @Column(name = "name", length = 160, nullable = false) private String name;
        @Column(name = "organization", length = 160, nullable = false) private String organization;
        @Column(name = "issue_date") private LocalDate issueDate;
        @Column(name = "credential_url", length = 2048) private String credentialUrl;
    }
    @Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Language {
        @Column(name = "name", length = 80, nullable = false) private String name;
        @Column(name = "proficiency", length = 80, nullable = false) private String proficiency;
    }
    @Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class Achievement {
        @Column(name = "title", length = 160, nullable = false) private String title;
        @Column(name = "issuer", length = 160) private String issuer;
        @Column(name = "award_date") private LocalDate awardDate;
        @Column(name = "description", length = 3000) private String description;
    }
}
