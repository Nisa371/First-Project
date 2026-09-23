-- Apply once for MySQL deployments using ddl-auto=validate/none, after earlier phase schema updates.
-- Development uses Hibernate ddl-auto=update. Existing jobs and bookings retain their statuses:
-- ACTIVE jobs and BOOKED appointments are grandfathered; no retroactive payment is required.
ALTER TABLE bookings MODIFY COLUMN status ENUM('PENDING_PAYMENT','BOOKED','COMPLETED','CANCELLED','NO_SHOW') NOT NULL;
CREATE TABLE payment_transactions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    payer_id BIGINT NOT NULL,
    purpose ENUM('JOB_POSTING','SESSION_BOOKING') NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status ENUM('PENDING','SUCCESS','FAILED','CANCELLED') NOT NULL,
    reference VARCHAR(50) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    job_id BIGINT NULL UNIQUE,
    booking_id BIGINT NULL UNIQUE,
    CONSTRAINT fk_payment_payer FOREIGN KEY (payer_id) REFERENCES users(id),
    CONSTRAINT fk_payment_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT payment_resource_check CHECK (
        (purpose = 'JOB_POSTING' AND job_id IS NOT NULL AND booking_id IS NULL) OR
        (purpose = 'SESSION_BOOKING' AND booking_id IS NOT NULL AND job_id IS NULL)
    )
);
