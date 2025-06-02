-- V2__insert_initial_data.sql

-- Inserindo dados na tabela partners
INSERT INTO partners (name, credit_limit, current_credit) VALUES
('Partner A', 10000.00, 5000.00),
('Partner B', 20000.00, 10000.00),
('Partner C', 15000.00, 7500.00),
('Partner D', 25000.00, 12500.00),
('Partner E', 12000.00, 6000.00),
('Partner F', 18000.00, 9000.00),
('Partner G', 22000.00, 11000.00),
('Partner H', 13000.00, 6500.00),
('Partner I', 19000.00, 9500.00),
('Partner J', 24000.00, 12000.00);


-- Inserindo dados na tabela orders (referenciando partners existentes)
-- Nota: Os IDs dos parceiros serão 1, 2, ..., 10, pois são gerados sequencialmente pelo BIGSERIAL
INSERT INTO orders (partner_id, total_value, status, created_at, updated_at) VALUES
(1, 150.00, 'PENDING', NOW(), NOW()),
(2, 250.00, 'COMPLETED', NOW(), NOW()),
(3, 75.50, 'CANCELLED', NOW(), NOW()),
(4, 300.00, 'PENDING', NOW(), NOW()),
(5, 120.00, 'COMPLETED', NOW(), NOW()),
(6, 400.00, 'PENDING', NOW(), NOW()),
(7, 90.00, 'CANCELLED', NOW(), NOW()),
(8, 200.00, 'COMPLETED', NOW(), NOW()),
(9, 110.00, 'PENDING', NOW(), NOW()),
(10, 180.00, 'COMPLETED', NOW(), NOW());


-- Inserindo dados na tabela order_items (referenciando orders existentes)
-- Os IDs dos pedidos serão 1, 2, ..., 10, seguindo a ordem de inserção acima
INSERT INTO order_items (order_id, product, quantity, unit_price) VALUES
(1, 'Product A1', 2, 75.00),
(1, 'Product A2', 1, 0.00),
(2, 'Product B1', 1, 250.00),
(3, 'Product C1', 3, 25.17),
(4, 'Product D1', 1, 300.00),
(5, 'Product E1', 4, 30.00),
(6, 'Product F1', 2, 200.00),
(7, 'Product G1', 1, 90.00),
(8, 'Product H1', 5, 40.00),
(9, 'Product I1', 1, 110.00),
(10, 'Product J1', 2, 90.00);