-- Add mobile phone number and thermal receipt paper width to branches.
-- mobile       : branch contact number printed on every receipt
-- receipt_width_mm : thermal paper width (58 or 80 mm); default 58

ALTER TABLE branches
    ADD COLUMN mobile           VARCHAR(20)     NULL    COMMENT 'Branch contact phone number printed on receipts',
    ADD COLUMN receipt_width_mm TINYINT UNSIGNED NOT NULL DEFAULT 58
                                                        COMMENT 'Thermal printer paper width in mm (58 or 80)';
