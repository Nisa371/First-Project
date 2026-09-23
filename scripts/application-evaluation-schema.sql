-- Apply once after job-applications-schema.sql for validate/none deployments.
-- Development ddl-auto=update adds these columns with the same defaults.
ALTER TABLE job_applications
    ADD COLUMN cv_score DECIMAL(20,16) NULL,
    ADD COLUMN portfolio_score DECIMAL(20,16) NULL,
    ADD COLUMN assessment_score DECIMAL(20,16) NULL,
    ADD CONSTRAINT ck_application_cv_score CHECK (cv_score BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_application_portfolio_score CHECK (portfolio_score BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_application_assessment_score CHECK (assessment_score BETWEEN 0 AND 1);
ALTER TABLE jobs
    ADD COLUMN cv_weight DECIMAL(20,16) NOT NULL DEFAULT 0.25,
    ADD COLUMN portfolio_weight DECIMAL(20,16) NOT NULL DEFAULT 0.25,
    ADD COLUMN experience_weight DECIMAL(20,16) NOT NULL DEFAULT 0.25,
    ADD COLUMN assessment_weight DECIMAL(20,16) NOT NULL DEFAULT 0.25,
    ADD CONSTRAINT ck_job_cv_weight CHECK (cv_weight BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_job_portfolio_weight CHECK (portfolio_weight BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_job_experience_weight CHECK (experience_weight BETWEEN 0 AND 1),
    ADD CONSTRAINT ck_job_assessment_weight CHECK (assessment_weight BETWEEN 0 AND 1);
-- Experience and final scores are calculated on read from current source values.
