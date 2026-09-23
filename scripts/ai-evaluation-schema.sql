-- Apply once after application-evaluation-schema.sql for validate/none deployments.
ALTER TABLE job_applications
    ADD COLUMN cv_evaluation_status VARCHAR(24) NULL,
    ADD COLUMN cv_failure_code VARCHAR(48) NULL,
    ADD COLUMN cv_attempted_at TIMESTAMP(6) NULL,
    ADD COLUMN cv_evaluated_at TIMESTAMP(6) NULL,
    ADD COLUMN cv_evaluation_version VARCHAR(16) NULL,
    ADD COLUMN portfolio_evaluation_status VARCHAR(24) NULL,
    ADD COLUMN portfolio_failure_code VARCHAR(48) NULL,
    ADD COLUMN portfolio_attempted_at TIMESTAMP(6) NULL,
    ADD COLUMN portfolio_evaluated_at TIMESTAMP(6) NULL,
    ADD COLUMN portfolio_evaluation_version VARCHAR(16) NULL;
