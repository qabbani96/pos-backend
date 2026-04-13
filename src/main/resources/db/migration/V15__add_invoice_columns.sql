-- ============================================================
-- V15: Ensure the invoices table exists with all required columns.
--
-- Background:
--   V4/V5 migrations (now deleted) originally created the invoices
--   table. V6 made invoice_number nullable. V7 added customer_id.
--   On a FRESH database none of those ran, so the table may not
--   exist at all → we CREATE it here with IF NOT EXISTS.
--   On an EXISTING database the table exists but is missing the
--   device / branch / audit columns → we add them idempotently
--   via stored procedures that check information_schema first.
--
-- MySQL does NOT support ADD COLUMN IF NOT EXISTS (MariaDB-only).
-- We use PREPARE/EXECUTE inside stored procedures as the workaround.
--
-- NOTE: No DELIMITER directives — Flyway uses JDBC and handles
-- BEGIN...END blocks natively.
-- ============================================================

-- ══════════════════════════════════════════════════════════════
-- STEP 1: Create the table if it doesn't exist at all
--         (covers fresh-database installs where V4/V5 never ran)
-- ══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS invoices (
    id             BIGINT         NOT NULL AUTO_INCREMENT,
    invoice_number VARCHAR(50)    NULL,
    customer_id    BIGINT         NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_invoice_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE SET NULL
);

-- ══════════════════════════════════════════════════════════════
-- STEP 2: Helper procedures for idempotent DDL
--         (covers existing databases where columns may or may
--          not already be present from a partial V15 run)
-- ══════════════════════════════════════════════════════════════

-- ── Helper: add a column only if it doesn't already exist ────
DROP PROCEDURE IF EXISTS pos_add_col;

CREATE PROCEDURE pos_add_col(
    IN p_table VARCHAR(64),
    IN p_col   VARCHAR(64),
    IN p_def   TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   information_schema.COLUMNS
        WHERE  TABLE_SCHEMA = DATABASE()
          AND  TABLE_NAME   = p_table
          AND  COLUMN_NAME  = p_col
    ) THEN
        SET @_ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_col, '` ', p_def);
        PREPARE _s FROM @_ddl;
        EXECUTE _s;
        DEALLOCATE PREPARE _s;
    END IF;
END;

-- ── Helper: add an FK constraint only if it doesn't exist ────
DROP PROCEDURE IF EXISTS pos_add_fk;

CREATE PROCEDURE pos_add_fk(
    IN p_table      VARCHAR(64),
    IN p_constraint VARCHAR(64),
    IN p_fk_def     TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   information_schema.TABLE_CONSTRAINTS
        WHERE  TABLE_SCHEMA    = DATABASE()
          AND  TABLE_NAME      = p_table
          AND  CONSTRAINT_NAME = p_constraint
          AND  CONSTRAINT_TYPE = 'FOREIGN KEY'
    ) THEN
        SET @_ddl = CONCAT('ALTER TABLE `', p_table,
                           '` ADD CONSTRAINT `', p_constraint, '` ', p_fk_def);
        PREPARE _s FROM @_ddl;
        EXECUTE _s;
        DEALLOCATE PREPARE _s;
    END IF;
END;

-- ── Helper: create an index only if it doesn't exist ─────────
DROP PROCEDURE IF EXISTS pos_add_idx;

CREATE PROCEDURE pos_add_idx(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols  TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM   information_schema.STATISTICS
        WHERE  TABLE_SCHEMA = DATABASE()
          AND  TABLE_NAME   = p_table
          AND  INDEX_NAME   = p_index
    ) THEN
        SET @_ddl = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` ', p_cols);
        PREPARE _s FROM @_ddl;
        EXECUTE _s;
        DEALLOCATE PREPARE _s;
    END IF;
END;

-- ══════════════════════════════════════════════════════════════
-- STEP 3: Add all missing columns (idempotent)
-- ══════════════════════════════════════════════════════════════

-- ── Customer snapshot ─────────────────────────────────────────
CALL pos_add_col('invoices', 'customer_name',   'VARCHAR(100) NULL');
CALL pos_add_col('invoices', 'customer_number', 'VARCHAR(30)  NULL');

-- ── Branch FK ────────────────────────────────────────────────
CALL pos_add_col('invoices', 'branch_id', 'BIGINT NULL');

CALL pos_add_fk('invoices', 'fk_invoice_branch',
    'FOREIGN KEY (branch_id) REFERENCES branches (id) ON DELETE SET NULL');

-- ── Device info ───────────────────────────────────────────────
CALL pos_add_col('invoices', 'device_type',     'VARCHAR(100) NULL');
CALL pos_add_col('invoices', 'device_color',    'VARCHAR(50)  NULL');
CALL pos_add_col('invoices', 'device_question', 'TEXT NULL');
CALL pos_add_col('invoices', 'device_status',
    'ENUM(''PENDING'',''CHECKOUT'',''FIX'',''NOT_FIX'',''CANCELLED'') NULL DEFAULT ''PENDING''');
CALL pos_add_col('invoices', 'device_problem',     'TEXT NULL');
CALL pos_add_col('invoices', 'device_imei',        'VARCHAR(50)  NULL');
CALL pos_add_col('invoices', 'device_note',        'TEXT NULL');
CALL pos_add_col('invoices', 'device_accessories', 'VARCHAR(255) NULL');
CALL pos_add_col('invoices', 'device_price',       'DECIMAL(10,2) NULL');
CALL pos_add_col('invoices', 'hidden_price',       'DECIMAL(10,2) NULL');

-- ── Call center ───────────────────────────────────────────────
CALL pos_add_col('invoices', 'feedback_callcenter', 'TEXT NULL');

-- ── Dates and times ───────────────────────────────────────────
CALL pos_add_col('invoices', 'entry_date',       'DATE NULL');
CALL pos_add_col('invoices', 'entry_time',       'TIME NULL');
CALL pos_add_col('invoices', 'finish_main_date', 'DATE NULL');
CALL pos_add_col('invoices', 'finish_main_time', 'TIME NULL');
CALL pos_add_col('invoices', 'bill_date',        'DATE NULL');
CALL pos_add_col('invoices', 'bill_time',        'TIME NULL');

-- ── Audit ─────────────────────────────────────────────────────
CALL pos_add_col('invoices', 'created_by', 'BIGINT NULL');

CALL pos_add_fk('invoices', 'fk_invoice_created_by',
    'FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL');

CALL pos_add_col('invoices', 'created_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)');

CALL pos_add_col('invoices', 'updated_at',
    'DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)');

-- ══════════════════════════════════════════════════════════════
-- STEP 4: Indexes
-- ══════════════════════════════════════════════════════════════
CALL pos_add_idx('invoices', 'idx_invoices_branch',       '(branch_id)');
CALL pos_add_idx('invoices', 'idx_invoices_status',       '(device_status)');
CALL pos_add_idx('invoices', 'idx_invoices_created_at',   '(created_at)');
CALL pos_add_idx('invoices', 'idx_invoices_customer_num', '(customer_number)');

-- ══════════════════════════════════════════════════════════════
-- STEP 5: Cleanup helper procedures
-- ══════════════════════════════════════════════════════════════
DROP PROCEDURE IF EXISTS pos_add_col;
DROP PROCEDURE IF EXISTS pos_add_fk;
DROP PROCEDURE IF EXISTS pos_add_idx;
