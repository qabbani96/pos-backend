-- V7: Customer table + link invoices to customers

CREATE TABLE customers (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    customer_name   VARCHAR(100) NOT NULL,
    customer_number VARCHAR(20)  NOT NULL COMMENT 'Customer phone number — unique identifier',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_customer_number (customer_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Add FK from invoices to customers (nullable — invoice can exist without a customer record)
ALTER TABLE invoices
    ADD COLUMN customer_id BIGINT NULL,
    ADD CONSTRAINT fk_invoice_customer
        FOREIGN KEY (customer_id) REFERENCES customers (id) ON DELETE SET NULL;
