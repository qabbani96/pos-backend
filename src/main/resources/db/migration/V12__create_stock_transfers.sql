-- ============================================================
-- V12: Stock transfers — central inventory → shop
-- ============================================================
-- A transfer moves items from the central warehouse (stock)
-- to a specific shop (shop_stock).
-- Status lifecycle: PENDING → COMPLETED | CANCELLED
-- ============================================================

CREATE TABLE stock_transfers (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    shop_id     BIGINT       NOT NULL,
    status      ENUM('PENDING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    note        VARCHAR(500) NULL,
    created_by  BIGINT       NULL,
    completed_at DATETIME(6) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_transfer_shop FOREIGN KEY (shop_id)
        REFERENCES shops (id),
    CONSTRAINT fk_transfer_user FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_transfer_items (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    transfer_id BIGINT NOT NULL,
    item_id     BIGINT NOT NULL,
    quantity    INT    NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_transfer_item_transfer FOREIGN KEY (transfer_id)
        REFERENCES stock_transfers (id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_item_item FOREIGN KEY (item_id)
        REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_transfers_shop   ON stock_transfers (shop_id);
CREATE INDEX idx_transfers_status ON stock_transfers (status);
CREATE INDEX idx_transfer_items   ON stock_transfer_items (transfer_id);
