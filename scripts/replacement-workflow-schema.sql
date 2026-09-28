-- Apply once to an existing MySQL database before starting the updated application,
-- including development databases: Hibernate update does not replace old CHECK rules.
-- Stop application writes while applying this migration. Fresh Hibernate-created schemas need no migration.
DELIMITER //
CREATE PROCEDURE migrate_replacement_workflow()
BEGIN
    DECLARE finished BOOLEAN DEFAULT FALSE;
    DECLARE check_name VARCHAR(255);
    DECLARE checks_to_drop CURSOR FOR
        SELECT tc.CONSTRAINT_NAME
        FROM information_schema.TABLE_CONSTRAINTS tc
        JOIN information_schema.CHECK_CONSTRAINTS cc
          ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
        WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = 'replacement_requests'
          AND tc.CONSTRAINT_TYPE = 'CHECK'
          AND (cc.CHECK_CLAUSE LIKE '%active_request%' OR cc.CHECK_CLAUSE LIKE '%status%');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;
    OPEN checks_to_drop;
    drop_checks: LOOP
        FETCH checks_to_drop INTO check_name;
        IF finished THEN LEAVE drop_checks; END IF;
        SET @replacement_ddl = CONCAT('ALTER TABLE replacement_requests DROP CHECK `', REPLACE(check_name, '`', '``'), '`');
        PREPARE replacement_stmt FROM @replacement_ddl;
        EXECUTE replacement_stmt;
        DEALLOCATE PREPARE replacement_stmt;
    END LOOP;
    CLOSE checks_to_drop;
END//
DELIMITER ;
CALL migrate_replacement_workflow();
DROP PROCEDURE migrate_replacement_workflow;

ALTER TABLE replacement_requests
    MODIFY COLUMN status ENUM('REQUESTED','MATCHING','CANDIDATE_SELECTED','ACCEPTED','COMPLETED','FAILED','WAITING_FOR_CANDIDATE','HIRING','CANCELLED') NOT NULL,
    MODIFY COLUMN target_completion_at DATETIME(6) NULL,
    ADD COLUMN free_replacement_job_id BIGINT NULL,
    ADD CONSTRAINT uk_replacement_free_job UNIQUE (free_replacement_job_id),
    ADD CONSTRAINT fk_replacement_free_job FOREIGN KEY (free_replacement_job_id) REFERENCES jobs(id);

UPDATE replacement_requests
SET status = 'CANCELLED', failure_reason = NULL, active_request = NULL, version = version + 1
WHERE status = 'FAILED' AND failure_reason = 'Employer cancelled the request.';

-- Restore only the latest empty-queue Trade request for an active placement.
-- Older history remains terminal, and existing active requests retain priority.
UPDATE replacement_requests r
JOIN placements p ON p.id = r.placement_id
JOIN candidate_profiles c ON c.id = p.candidate_id
LEFT JOIN replacement_requests newer ON newer.placement_id = r.placement_id AND newer.id > r.id
LEFT JOIN replacement_requests active ON active.placement_id = r.placement_id AND active.active_request = TRUE
SET r.status = 'WAITING_FOR_CANDIDATE', r.active_request = TRUE, r.version = r.version + 1,
    r.failure_reason = 'No eligible replacement worker is currently available. This request remains active and matching can continue.'
WHERE r.status = 'FAILED' AND c.candidate_type = 'TRADE' AND p.status = 'ACTIVE'
  AND r.failure_reason = 'No eligible candidates are currently available in this skill queue.'
  AND newer.id IS NULL AND active.id IS NULL;

ALTER TABLE replacement_requests ADD CONSTRAINT ck_replacement_lifecycle CHECK (
    (target_completion_at IS NULL OR target_completion_at >= requested_at)
    AND ((status IN ('COMPLETED','FAILED','CANCELLED') AND active_request IS NULL)
      OR (status NOT IN ('COMPLETED','FAILED','CANCELLED') AND active_request IS NOT NULL AND active_request = TRUE))
);
