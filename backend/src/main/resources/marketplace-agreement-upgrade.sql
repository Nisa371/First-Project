-- Apply once before restart for MySQL/MariaDB ddl-auto=validate/none deployments.
-- Development ddl-auto=update adds the nullable columns automatically.
ALTER TABLE placements
    ADD COLUMN agreed_first_month_salary DECIMAL(14,2) NULL,
    ADD COLUMN placement_fee_rate DECIMAL(3,2) NULL,
    ADD COLUMN placement_fee_amount DECIMAL(14,2) NULL,
    ADD COLUMN fee_agreement_accepted_at TIMESTAMP(6) NULL,
    ADD COLUMN accepted_by_employer_user_id BIGINT NULL,
    ADD COLUMN terms_version VARCHAR(40) NULL;
ALTER TABLE employer_profiles ADD COLUMN employer_notice_acknowledged_at TIMESTAMP(6) NULL;
UPDATE skills SET category=UPPER(category) WHERE LOWER(category) IN ('tech','trade');
