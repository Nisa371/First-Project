-- Apply once after job-applications-schema.sql and application-evaluation-schema.sql.
-- Historical applications need no session; absence means NOT_STARTED. Scores remain on job_applications.
CREATE TABLE application_assessment_sessions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    status VARCHAR(24) NOT NULL,
    started_at TIMESTAMP(6) NULL,
    completed_at TIMESTAMP(6) NULL,
    current_turn INT NOT NULL,
    max_turns INT NOT NULL,
    failure_code VARCHAR(48) NULL,
    evaluation_summary VARCHAR(2000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_assessment_application UNIQUE (application_id),
    CONSTRAINT fk_assessment_application FOREIGN KEY (application_id) REFERENCES job_applications(id)
);
CREATE TABLE application_assessment_messages (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    sender_role VARCHAR(16) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    sequence_number INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_assessment_message_sequence UNIQUE (session_id, sequence_number),
    CONSTRAINT fk_assessment_message_session FOREIGN KEY (session_id) REFERENCES application_assessment_sessions(id)
);
