-- MySQL 8: run against the existing application database with the backend stopped,
-- BEFORE starting this version. Hibernate update adds columns but does not reliably
-- remove the two old one-to-one UNIQUE indexes. Fresh databases need no upgrade.
-- No applications, payments, placements or evaluations are deleted.
DELIMITER $$
CREATE PROCEDURE marketplace_portals_upgrade()
BEGIN
    DECLARE finished BOOLEAN DEFAULT FALSE;
    DECLARE target_table VARCHAR(64);
    DECLARE target_index VARCHAR(64);
    DECLARE old_unique CURSOR FOR
        SELECT table_name, index_name FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND non_unique = 0 AND index_name <> 'PRIMARY'
          AND table_name IN ('payment_transactions', 'replacement_requests')
        GROUP BY table_name, index_name
        HAVING COUNT(*) = 1 AND
          ((table_name = 'payment_transactions' AND MAX(column_name) = 'job_id') OR
           (table_name = 'replacement_requests' AND MAX(column_name) = 'free_replacement_job_id'));
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='payment_transactions' AND index_name='ix_payment_job_history') THEN
        ALTER TABLE payment_transactions ADD INDEX ix_payment_job_history (job_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='replacement_requests' AND index_name='ix_shared_replacement_job') THEN
        ALTER TABLE replacement_requests ADD INDEX ix_shared_replacement_job (free_replacement_job_id);
    END IF;
    OPEN old_unique;
    remove_uniques: LOOP
        FETCH old_unique INTO target_table, target_index;
        IF finished THEN LEAVE remove_uniques; END IF;
        SET @portal_upgrade_sql = CONCAT('ALTER TABLE `', target_table, '` DROP INDEX `', target_index, '`');
        PREPARE portal_upgrade_statement FROM @portal_upgrade_sql;
        EXECUTE portal_upgrade_statement;
        DEALLOCATE PREPARE portal_upgrade_statement;
    END LOOP;
    CLOSE old_unique;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='jobs' AND column_name='employment_type') THEN
        ALTER TABLE jobs ADD COLUMN employment_type ENUM('CONTRACT','FULL_TIME','INTERNSHIP','PART_TIME') NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='jobs' AND column_name='activated_at') THEN
        ALTER TABLE jobs ADD COLUMN activated_at DATETIME(6) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='jobs' AND column_name='portal_closes_at') THEN
        ALTER TABLE jobs ADD COLUMN portal_closes_at DATETIME(6) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='jobs' AND column_name='employer_requested_end_date') THEN
        ALTER TABLE jobs ADD COLUMN employer_requested_end_date DATE NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='jobs' AND column_name='replacement_window_started_at') THEN
        ALTER TABLE jobs ADD COLUMN replacement_window_started_at DATETIME(6) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='jobs' AND column_name='replacement_window_expires_at') THEN
        ALTER TABLE jobs ADD COLUMN replacement_window_expires_at DATETIME(6) NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='jobs' AND column_name='replacement_guarantee_days_snapshot') THEN
        ALTER TABLE jobs ADD COLUMN replacement_guarantee_days_snapshot INT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='jobs' AND column_name='original_job_id') THEN
        ALTER TABLE jobs ADD COLUMN original_job_id BIGINT NULL,
            ADD CONSTRAINT fk_shared_portal_original_job FOREIGN KEY (original_job_id) REFERENCES jobs(id);
    END IF;
END$$
DELIMITER ;
CALL marketplace_portals_upgrade();
DROP PROCEDURE marketplace_portals_upgrade;
-- On startup the application consolidates outstanding legacy replacement requests
-- onto one shared vacancy, preserving historical jobs/applications, and snapshots
-- the earliest placement's stored deadline. Missing historical policy is treated
-- as expired instead of silently granting fresh coverage. Legacy employment type
-- remains NULL ("Not specified") until explicitly selected by the employer.
