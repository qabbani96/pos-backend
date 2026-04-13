-- ============================================================
-- V10: Shops table + INVENTORY role + shop_id on users/sales
-- ============================================================

-- 1. Extend role ENUM to include INVENTORY
--    Always list ALL existing values plus the new one.
ALTER TABLE users
    MODIFY COLUMN role ENUM(
        'ADMIN',
        'CASHIER',
        'ADMIN_BRANCHES',
        'RECEPTION',
        'CALL_CENTER',
        'INVENTORY'
    ) NOT NULL;

-- 2. Create shops table
--    A shop is a physical point-of-sale location linked to a branch.
CREATE TABLE shops (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100) NOT NULL,
    branch_id   BIGINT       NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uq_shop_name (name),
    CONSTRAINT fk_shop_branch FOREIGN KEY (branch_id)
        REFERENCES branches (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Add shop_id to users (CASHIER/INVENTORY users belong to a shop)
ALTER TABLE users
    ADD COLUMN shop_id BIGINT NULL,
    ADD CONSTRAINT fk_user_shop FOREIGN KEY (shop_id)
        REFERENCES shops (id) ON DELETE SET NULL;

-- 4. Add shop_id to sales (records which shop made the sale)
ALTER TABLE sales
    ADD COLUMN shop_id BIGINT NULL,
    ADD CONSTRAINT fk_sale_shop FOREIGN KEY (shop_id)
        REFERENCES shops (id) ON DELETE SET NULL;

CREATE INDEX idx_sales_shop ON sales (shop_id);
CREATE INDEX idx_users_shop ON users (shop_id);
