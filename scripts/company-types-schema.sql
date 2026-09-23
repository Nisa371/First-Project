-- Apply once to an existing MySQL database before starting with ddl-auto=validate/none.
-- Development uses Hibernate ddl-auto=update instead; do not apply both approaches.
-- The application seeds the catalog and migrates legacy industry values on startup.
CREATE TABLE company_types (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL UNIQUE,
    code VARCHAR(32) UNIQUE,
    active BIT NOT NULL
);
ALTER TABLE employer_profiles
    ADD COLUMN company_type_id BIGINT NULL,
    ADD COLUMN custom_company_type VARCHAR(120) NULL,
    ADD CONSTRAINT fk_employer_company_type FOREIGN KEY (company_type_id) REFERENCES company_types(id);
-- industry intentionally remains untouched for lossless migration/backward data compatibility.
