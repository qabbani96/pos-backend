-- V6: Allow invoice_number to be temporarily NULL during creation.
-- The application sets it immediately after INSERT using the generated PK,
-- so the column will never have NULL in practice.
-- The UNIQUE constraint is kept — collision-free because it derives from the PK.
ALTER TABLE invoices
    MODIFY COLUMN invoice_number VARCHAR(50) NULL;
