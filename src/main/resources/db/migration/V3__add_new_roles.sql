-- ============================================================
-- V3: Add RECEPTION and CALL_CENTER roles
-- ============================================================
-- MySQL ENUM columns must list ALL values when altered.
-- Always append new values — never remove existing ones or
-- existing rows will fail the column constraint.
-- ============================================================

ALTER TABLE users
    MODIFY COLUMN role ENUM('ADMIN', 'CASHIER', 'RECEPTION', 'CALL_CENTER') NOT NULL;
