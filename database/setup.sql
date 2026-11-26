-- One-time database setup. Run as a MySQL admin user:
--   mysql -u root -p < database/setup.sql
--
-- Replace CHANGE_ME with your own password BEFORE running, and use the same value for DB_PASSWORD.
-- Tables and sample data are NOT created here: Flyway creates them automatically
-- the first time the backend starts (see backend/src/main/resources/db/migration).

CREATE DATABASE IF NOT EXISTS hotel_booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'hotel_app'@'localhost' IDENTIFIED BY 'CHANGE_ME';
GRANT ALL PRIVILEGES ON hotel_booking.* TO 'hotel_app'@'localhost';
FLUSH PRIVILEGES;
