-- Initial data for testing (optional)
-- Users
INSERT INTO users (id, username, password, role, created_at) VALUES
    ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', 'admin', 'admin123', 'ADMIN', CURRENT_TIMESTAMP),
    ('b2c3d4e5-f6a7-8901-bcde-f12345678901', 'cashier1', 'cashier123', 'CASHIER', CURRENT_TIMESTAMP);

-- Products
INSERT INTO products (id, name, price, stock, created_at) VALUES
    ('c3d4e5f6-a7b8-9012-cdef-123456789012', 'Laptop', 1500000.00, 50, CURRENT_TIMESTAMP),
    ('d4e5f6a7-b8c9-0123-defa-234567890123', 'Mouse', 150000.00, 200, CURRENT_TIMESTAMP),
    ('e5f6a7b8-c9d0-1234-efab-345678901234', 'Keyboard', 350000.00, 150, CURRENT_TIMESTAMP),
    ('f6a7b8c9-d0e1-2345-fabc-456789012345', 'Monitor', 2500000.00, 30, CURRENT_TIMESTAMP);
