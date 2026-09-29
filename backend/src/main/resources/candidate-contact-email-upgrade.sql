-- Apply once to an existing MySQL database before starting the updated backend.
ALTER TABLE candidate_profiles ADD COLUMN contact_email VARCHAR(254) NULL;
