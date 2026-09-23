-- Apply once for validate/none installations; development ddl-auto=update adds these structures.
ALTER TABLE candidate_profiles ADD COLUMN photo_stored_name VARCHAR(80) NULL;
CREATE TABLE candidate_cvs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    candidate_id BIGINT NOT NULL UNIQUE,
    summary VARCHAR(4000), linkedin_url VARCHAR(2048), github_url VARCHAR(2048),
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
    FOREIGN KEY (candidate_id) REFERENCES candidate_profiles(id)
);
CREATE TABLE cv_education (
    cv_id BIGINT NOT NULL, display_order INT NOT NULL,
    institution VARCHAR(160) NOT NULL,
    qualification VARCHAR(160) NOT NULL,
    field_of_study VARCHAR(160),
    start_date DATE,
    end_date DATE,
    is_current BOOLEAN NOT NULL,
    grade VARCHAR(80),
    description VARCHAR(3000),
    PRIMARY KEY (cv_id, display_order),
    FOREIGN KEY (cv_id) REFERENCES candidate_cvs(id) ON DELETE CASCADE
);
CREATE TABLE cv_experience (
    cv_id BIGINT NOT NULL, display_order INT NOT NULL,
    organization VARCHAR(160) NOT NULL,
    title VARCHAR(160) NOT NULL,
    start_date DATE,
    end_date DATE,
    is_current BOOLEAN NOT NULL,
    description VARCHAR(3000),
    PRIMARY KEY (cv_id, display_order),
    FOREIGN KEY (cv_id) REFERENCES candidate_cvs(id) ON DELETE CASCADE
);
CREATE TABLE cv_projects (
    cv_id BIGINT NOT NULL, display_order INT NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(3000),
    technologies VARCHAR(500),
    project_url VARCHAR(2048),
    repository_url VARCHAR(2048),
    start_date DATE,
    end_date DATE,
    PRIMARY KEY (cv_id, display_order),
    FOREIGN KEY (cv_id) REFERENCES candidate_cvs(id) ON DELETE CASCADE
);
CREATE TABLE cv_certifications (
    cv_id BIGINT NOT NULL, display_order INT NOT NULL,
    name VARCHAR(160) NOT NULL,
    organization VARCHAR(160) NOT NULL,
    issue_date DATE,
    credential_url VARCHAR(2048),
    PRIMARY KEY (cv_id, display_order),
    FOREIGN KEY (cv_id) REFERENCES candidate_cvs(id) ON DELETE CASCADE
);
CREATE TABLE cv_languages (
    cv_id BIGINT NOT NULL, display_order INT NOT NULL,
    name VARCHAR(80) NOT NULL,
    proficiency VARCHAR(80) NOT NULL,
    PRIMARY KEY (cv_id, display_order),
    FOREIGN KEY (cv_id) REFERENCES candidate_cvs(id) ON DELETE CASCADE
);
CREATE TABLE cv_achievements (
    cv_id BIGINT NOT NULL, display_order INT NOT NULL,
    title VARCHAR(160) NOT NULL,
    issuer VARCHAR(160),
    award_date DATE,
    description VARCHAR(3000),
    PRIMARY KEY (cv_id, display_order),
    FOREIGN KEY (cv_id) REFERENCES candidate_cvs(id) ON DELETE CASCADE
);
