-- Run this once as a MySQL administrator before starting the Spring Boot backend.
-- Example:
--   mysql -u root -p < scripts/mysql-setup.sql

CREATE DATABASE IF NOT EXISTS verified_career_marketplace
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Optional: use a dedicated application user instead of root.
-- Change the password below before running these statements, then export
-- DB_USERNAME=career_app and DB_PASSWORD=<the same password> before startup.
--
-- CREATE USER IF NOT EXISTS 'career_app'@'localhost'
--   IDENTIFIED BY 'CHANGE_THIS_PASSWORD';
-- GRANT ALL PRIVILEGES ON verified_career_marketplace.*
--   TO 'career_app'@'localhost';
-- FLUSH PRIVILEGES;
