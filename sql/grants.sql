-- StreamVault RBAC: GRANT / REVOKE scripts for 3 database roles
-- Run in MySQL Workbench as root after running setup.sql
USE streamvault;

-- -----------------------------------------------
-- Create application-level MySQL users
-- -----------------------------------------------
CREATE USER IF NOT EXISTS 'sv_viewer'@'localhost'          IDENTIFIED BY 'Viewer@2024!';
CREATE USER IF NOT EXISTS 'sv_content_manager'@'localhost' IDENTIFIED BY 'Manager@2024!';
CREATE USER IF NOT EXISTS 'sv_admin'@'localhost'           IDENTIFIED BY 'Admin@2024!';

-- -----------------------------------------------
-- Role 1: viewer  (SELECT only on content tables)
-- -----------------------------------------------
GRANT SELECT ON streamvault.Content_Items    TO 'sv_viewer'@'localhost';
GRANT SELECT ON streamvault.Episodes         TO 'sv_viewer'@'localhost';
GRANT SELECT ON streamvault.Genres           TO 'sv_viewer'@'localhost';
GRANT SELECT ON streamvault.Content_Genres   TO 'sv_viewer'@'localhost';
GRANT SELECT ON streamvault.Languages        TO 'sv_viewer'@'localhost';
GRANT SELECT ON streamvault.Content_Languages TO 'sv_viewer'@'localhost';
GRANT SELECT ON streamvault.Studios          TO 'sv_viewer'@'localhost';
GRANT SELECT ON streamvault.Subscription_Plans TO 'sv_viewer'@'localhost';

-- -----------------------------------------------
-- Role 2: content_manager (SELECT + INSERT + UPDATE on content)
-- -----------------------------------------------
GRANT SELECT, INSERT, UPDATE ON streamvault.Content_Items     TO 'sv_content_manager'@'localhost';
GRANT SELECT, INSERT, UPDATE ON streamvault.Episodes          TO 'sv_content_manager'@'localhost';
GRANT SELECT, INSERT, UPDATE ON streamvault.Genres            TO 'sv_content_manager'@'localhost';
GRANT SELECT, INSERT, UPDATE ON streamvault.Content_Genres    TO 'sv_content_manager'@'localhost';
GRANT SELECT, INSERT, UPDATE ON streamvault.Languages         TO 'sv_content_manager'@'localhost';
GRANT SELECT, INSERT, UPDATE ON streamvault.Content_Languages TO 'sv_content_manager'@'localhost';
GRANT SELECT, INSERT, UPDATE ON streamvault.Studios           TO 'sv_content_manager'@'localhost';
GRANT SELECT, INSERT, UPDATE ON streamvault.Watch_History     TO 'sv_content_manager'@'localhost';
GRANT SELECT ON streamvault.Users                             TO 'sv_content_manager'@'localhost';

-- -----------------------------------------------
-- Role 3: admin (full access to all tables)
-- -----------------------------------------------
GRANT ALL PRIVILEGES ON streamvault.* TO 'sv_admin'@'localhost';

FLUSH PRIVILEGES;

-- -----------------------------------------------
-- Verify grants
-- -----------------------------------------------
SHOW GRANTS FOR 'sv_viewer'@'localhost';
SHOW GRANTS FOR 'sv_content_manager'@'localhost';
SHOW GRANTS FOR 'sv_admin'@'localhost';

-- -----------------------------------------------
-- Revoke example (if role changes)
-- -----------------------------------------------
-- REVOKE INSERT, UPDATE ON streamvault.Content_Items FROM 'sv_content_manager'@'localhost';
-- DROP USER IF EXISTS 'sv_viewer'@'localhost';
