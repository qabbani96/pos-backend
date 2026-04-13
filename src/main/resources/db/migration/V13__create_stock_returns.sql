-- ============================================================
-- V13: Stock returns — shop → central inventory
-- ============================================================
-- A return moves items back from a shop to the central
-- warehouse. Status lifecycle: PENDING → COMPLETED | CANCELLED
-- ============================================================

CREATE TABLE stock_returns (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    shop_id     BIGINT       NOT NULL,
    status      ENUM('PENDING', 'COMPLETED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    note        VARCHAR(500) NULL,
    created_by  BIGINT       NULL,
    completed_at DATETIME(6) NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    CONSTRAINT fk_return_shop FOREIGN KEY (shop_id)
        REFERENCES shops (id),
    CONSTRAINT fk_return_user FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE stock_return_items (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    return_id BIGINT NOT NULL,
    item_id   BIGINT NOT NULL,
    quantity  INT    NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_return_item_return FOREIGN KEY (return_id)
        REFERENCES stock_returns (id) ON DELETE CASCADE,
    CONSTRAINT fk_return_item_item FOREIGN KEY (item_id)
        REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_returns_shop   ON stock_returns (shop_id);
CREATE INDEX idx_returns_status ON stock_returns (status);
CREATE INDEX idx_return_items   ON stock_return_items (return_id);
