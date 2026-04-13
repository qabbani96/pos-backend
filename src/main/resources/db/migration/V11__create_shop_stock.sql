-- ============================================================
-- V11: Shop stock — per-shop item quantities
-- ============================================================
-- Each shop maintains its own stock level per item.
-- Central stock (stock table) is the warehouse — transfers
-- move quantity from central → shop. Sales deduct from shop.
-- ============================================================

CREATE TABLE shop_stock (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    shop_id      BIGINT    NOT NULL,
    item_id      BIGINT    NOT NULL,
    quantity     INT       NOT NULL DEFAULT 0,
    min_quantity INT       NOT NULL DEFAULT 5,
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_shop_stock (shop_id, item_id),
    CONSTRAINT fk_shop_stock_shop FOREIGN KEY (shop_id)
        REFERENCES shops (id) ON DELETE CASCADE,
    CONSTRAINT fk_shop_stock_item FOREIGN KEY (item_id)
        REFERENCES items (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_shop_stock_item ON shop_stock (item_id);
CREATE INDEX idx_shop_stock_shop ON shop_stock (shop_id);
