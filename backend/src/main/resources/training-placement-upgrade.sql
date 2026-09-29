-- MySQL/MariaDB deployments using ddl-auto=validate/none: apply once before restart.
-- Development ddl-auto=update creates these structures automatically.
CREATE TABLE IF NOT EXISTS training_program_skills (
    training_program_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    PRIMARY KEY (training_program_id, skill_id),
    FOREIGN KEY (training_program_id) REFERENCES training_programs(id),
    FOREIGN KEY (skill_id) REFERENCES skills(id)
);
INSERT INTO training_program_skills (training_program_id, skill_id)
SELECT p.id, p.skill_id FROM training_programs p
WHERE p.skill_id IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM training_program_skills s WHERE s.training_program_id = p.id AND s.skill_id = p.skill_id
);
ALTER TABLE placements ADD COLUMN ended_at TIMESTAMP(6) NULL;
