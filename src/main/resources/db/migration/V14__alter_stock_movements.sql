-- ============================================================
-- V14: Enhance stock_movements for multi-shop tracking
-- ============================================================
-- Adds:
--   shop_id       — null = central warehouse movement
--   balance_before/after — snapshot for audit trail
--   New movement types:
--     TRANSFER_OUT  — central stock reduced (sent to shop)
--     TRANSFER_IN   — shop stock increased (received from central)
--     RETURN_OUT    — shop stock reduced (returned to central)
--     RETURN_IN     — central stock increased (received from shop)
--     SALE          — shop stock reduced by a sale
-- ============================================================

ALTER TABLE stock_movements
    ADD COLUMN shop_id        BIGINT NULL AFTER item_id,
    ADD COLUMN balance_before INT    NULL AFTER quantity,
    ADD COLUMN balance_after  INT    NULL AFTER balance_before,
    MODIFY COLUMN type ENUM(
        'IN',
        'OUT',
        'ADJUSTMENT',
        'TRANSFER_OUT',
        'TRANSFER_IN',
        'RETURN_OUT',
        'RETURN_IN',
        'SALE'
    ) NOT NULL,
    ADD CONSTRAINT fk_movement_shop FOREIGN KEY (shop_id)
        REFERENCES shops (id) ON DELETE SET NULL;

CREATE INDEX idx_movements_shop ON stock_movements (shop_id);
