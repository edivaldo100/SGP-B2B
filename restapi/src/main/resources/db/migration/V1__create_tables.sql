-- V1__create_tables.sql

-- Tabela partners
CREATE TABLE partners (
    id BIGSERIAL PRIMARY KEY, -- BIGSERIAL para auto-incremento de BIGINT
    name VARCHAR(255) NOT NULL UNIQUE,
    credit_limit DECIMAL(10, 2) NOT NULL,
    current_credit DECIMAL(10, 2) NOT NULL
);

-- Índice na coluna 'name' para buscas eficientes de parceiros por nome
CREATE INDEX idx_partners_name ON partners (name);

-- Tabela orders
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY, -- BIGSERIAL para auto-incremento de BIGINT
    partner_id BIGINT NOT NULL,
    total_value DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL, -- Mapeia OrderStatus (ENUM STRING)
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_order_partner
        FOREIGN KEY(partner_id)
        REFERENCES partners(id)
);

-- Índice na chave estrangeira 'partner_id' na tabela orders para otimizar joins
CREATE INDEX idx_orders_partner_id ON orders (partner_id);
-- Índice na coluna 'status' para buscas eficientes por status do pedido
CREATE INDEX idx_orders_status ON orders (status);


-- Tabela order_items
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY, -- BIGSERIAL para auto-incremento de BIGINT
    order_id BIGINT NOT NULL,
    product VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_order_item_order
        FOREIGN KEY(order_id)
        REFERENCES orders(id)
);

-- Índice na chave estrangeira 'order_id' na tabela order_items para otimizar joins
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
-- Índice na coluna 'product' para buscas eficientes por nome do produto
CREATE INDEX idx_order_items_product ON order_items (product);