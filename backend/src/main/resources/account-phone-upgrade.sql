-- Apply once to an existing MySQL database before starting the updated backend.
ALTER TABLE users
    MODIFY COLUMN email VARCHAR(254) NULL,
    ADD COLUMN phone VARCHAR(14) NULL,
    ADD CONSTRAINT uk_user_phone UNIQUE (phone),
    ADD CONSTRAINT ck_user_identifier CHECK (
        (email IS NOT NULL AND CHAR_LENGTH(TRIM(email)) > 0)
        OR (phone IS NOT NULL AND CHAR_LENGTH(TRIM(phone)) > 0)
    );
