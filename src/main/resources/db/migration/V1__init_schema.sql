-- ============================================================
-- POS System — Full Database Schema
-- V1: Initial schema
-- ============================================================

-- ─── Users ────────────────────────────────────────────────
CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    username   VARCHAR(50)  NOT NULL,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100),
    role       ENUM('ADMIN', 'CASHIER') NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Categories ───────────────────────────────────────────
CREATE TABLE categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),

    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Items ────────────────────────────────────────────────
CREATE TABLE items (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    sku          VARCHAR(50)   NOT NULL,
    name         VARCHAR(200)  NOT NULL,
    description  TEXT,
    category_id  BIGINT,
    price        DECIMAL(10,2) NOT NULL,
    cost_price   DECIMAL(10,2),
    barcode      VARCHAR(100),
    barcode_type ENUM('CODE128', 'EAN13', 'QR') NOT NULL DEFAULT 'CODE128',
    image_url    VARCHAR(255),
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_items_sku     (sku),
    UNIQUE KEY uk_items_barcode (barcode),
    CONSTRAINT fk_items_category FOREIGN KEY (category_id)
        REFERENCES categories (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Stock ────────────────────────────────────────────────
CREATE TABLE stock (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    item_id      BIGINT    NOT NULL,
    quantity     INT       NOT NULL DEFAULT 0,
    min_quantity INT       NOT NULL DEFAULT 5,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_item (item_id),
    CONSTRAINT fk_stock_item FOREIGN KEY (item_id)
        REFERENCES items (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Stock Movements ──────────────────────────────────────
CREATE TABLE stock_movements (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    item_id    BIGINT       NOT NULL,
    type       ENUM('IN', 'OUT', 'ADJUSTMENT') NOT NULL,
    quantity   INT          NOT NULL,
    reference  VARCHAR(100),
    note       VARCHAR(255),
    created_by BIGINT,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_movements_item FOREIGN KEY (item_id)
        REFERENCES items (id),
    CONSTRAINT fk_movements_user FOREIGN KEY (created_by)
        REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Sales ────────────────────────────────────────────────
-- Payment is always CASH — no payment_method column needed
CREATE TABLE sales (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    cashier_id   BIGINT        NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status       ENUM('COMPLETED', 'REFUNDED') NOT NULL DEFAULT 'COMPLETED',
    note         VARCHAR(255),
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_sales_cashier FOREIGN KEY (cashier_id)
        REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Sale Items ───────────────────────────────────────────
CREATE TABLE sale_items (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    sale_id    BIGINT        NOT NULL,
    item_id    BIGINT        NOT NULL,
    item_name  VARCHAR(200)  NOT NULL,   -- snapshot: preserves history
    quantity   INT           NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,   -- snapshot: preserves history
    subtotal   DECIMAL(10,2) NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_sale_items_sale FOREIGN KEY (sale_id)
        REFERENCES sales (id) ON DELETE CASCADE,
    CONSTRAINT fk_sale_items_item FOREIGN KEY (item_id)
        REFERENCES items (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ─── Indexes for common queries ───────────────────────────
CREATE INDEX idx_items_active         ON items (active);
CREATE INDEX idx_items_category       ON items (category_id);
CREATE INDEX idx_stock_movements_item ON stock_movements (item_id);
CREATE INDEX idx_stock_movements_date ON stock_movements (created_at);
CREATE INDEX idx_sales_cashier        ON sales (cashier_id);
CREATE INDEX idx_sales_date           ON sales (created_at);
CREATE INDEX idx_sale_items_sale      ON sale_items (sale_id);
