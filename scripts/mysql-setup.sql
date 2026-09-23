-- Database-only bootstrap for manual administrator use.
-- Prefer backend/setup-db.sh to also configure the dedicated application user
-- and load configurable DB_NAME, DB_USERNAME and DB_PASSWORD from backend/.env.
CREATE DATABASE IF NOT EXISTS verified_career_marketplace
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
