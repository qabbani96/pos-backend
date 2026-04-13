-- ============================================================
-- V4: Create branches table, add branch_id to users,
--     and create the base invoices table.
--
-- This migration reconstructs what the original V4 and V5
-- migrations contained (those files were deleted from the repo).
--
-- NOTE:
--   • branches.mobile and branches.receipt_width_mm are added
--     later by V8 — do NOT include them here.
--   • branches.receipt_height_mm was added by V9 (also deleted).
--     We include it here so a fresh install has the full schema.
--   • invoices.invoice_number is NOT NULL here; V6 makes it
--     nullable (kept separate so Flyway history stays intact).
--   • invoices.customer_id is added by V7.
--   • All remaining invoice columns are added by V15.
-- ============================================================

-- ── Branches ──────────────────────────────────────────────────
CREATE TABLE branches (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    branch_name       VARCHAR(100)    NOT NULL,
    receipt_height_mm TINYINT UNSIGNED NULL     COMMENT 'Thermal paper height mm; NULL = auto (content-driven)',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_branch_name (branch_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Link users to branches ────────────────────────────────────
-- RECEPTION and CALL_CENTER users belong to a specific branch.
ALTER TABLE users
    ADD COLUMN branch_id BIGINT NULL,
    ADD CONSTRAINT fk_user_branch
        FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE SET NULL;

CREATE INDEX idx_users_branch ON users (branch_id);

-- ── Invoices (base table) ─────────────────────────────────────
-- V6  → makes invoice_number nullable
-- V7  → adds customer_id FK
-- V15 → adds all device / branch / audit columns
CREATE TABLE invoices (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    invoice_number VARCHAR(50) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_invoice_number (invoice_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
