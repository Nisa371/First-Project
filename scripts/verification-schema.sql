-- Apply once to an existing MySQL schema with Phase 1 already applied, before validate/none startup.
-- Development uses Hibernate ddl-auto=update; do not apply both approaches.
CREATE TABLE verification_requirements (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000),
    target_type VARCHAR(32) NOT NULL,
    company_type_id BIGINT,
    required BIT NOT NULL,
    active BIT NOT NULL,
    code VARCHAR(40) UNIQUE,
    CONSTRAINT fk_requirement_company_type FOREIGN KEY (company_type_id) REFERENCES company_types(id)
);
ALTER TABLE verification_records
    MODIFY COLUMN candidate_id BIGINT NULL,
    ADD COLUMN owner_user_id BIGINT NULL,
    ADD COLUMN requirement_id BIGINT NULL,
    ADD COLUMN stored_name VARCHAR(80) NULL,
    ADD COLUMN original_name VARCHAR(180) NULL,
    ADD COLUMN content_type VARCHAR(80) NULL,
    ADD COLUMN file_size BIGINT NULL,
    ADD CONSTRAINT fk_verification_owner FOREIGN KEY (owner_user_id) REFERENCES users(id),
    ADD CONSTRAINT fk_verification_requirement FOREIGN KEY (requirement_id) REFERENCES verification_requirements(id);
-- Startup seeds stable baseline codes, marks the Household company type with code HOUSEHOLD,
-- and links existing candidate verification records to CANDIDATE_NID without changing decisions or notes.
