-- Apply once after Phases 1 and 2 before validate/none startup.
-- Development uses Hibernate ddl-auto=update and the same zero defaults.
ALTER TABLE jobs
    ADD COLUMN public_expectations VARCHAR(5000) NULL,
    ADD COLUMN private_expectations VARCHAR(5000) NULL,
    ADD COLUMN expected_experience_months INT NOT NULL DEFAULT 0;
ALTER TABLE candidate_profiles ADD COLUMN total_experience_months INT NOT NULL DEFAULT 0;
CREATE TABLE job_applications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    candidate_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_application_job_candidate UNIQUE (job_id, candidate_id),
    CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT fk_application_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_profiles(id)
);
-- Existing descriptions/experience text and shortlist rows are preserved.
-- Old employer-created shortlist rows are not converted into candidate applications.
-- Application.created_at is returned as appliedAt by the API.
