-- Insert 1000 products into PostgreSQL database
-- Usage: psql -U nurvan -d sales_db -f insert-1000-products.sql

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create temporary function to generate random numbers
DO $$
DECLARE
    i INTEGER;
    v_id UUID;
    v_name TEXT;
    v_price NUMERIC(15,2);
    v_stock INTEGER;
    v_created_at TIMESTAMP;
    v_updated_at TIMESTAMP;
    name_parts TEXT[] := ARRAY['Laptop', 'Phone', 'Tablet', 'Monitor', 'Keyboard', 'Mouse', 
                                'Headphones', 'Speaker', 'Camera', 'Printer', 'Scanner', 'Router',
                                'SSD', 'RAM', 'CPU', 'GPU', 'Motherboard', 'Power Supply', 
                                'Case', 'Cooling Fan', 'Webcam', 'Microphone', 'Projector', 
                                'UPS', 'Cable', 'Adapter', 'Battery', 'Charger', 'Smartwatch', 
                                'Earbuds', 'Switch', 'Hub', 'Dock', 'Stand', 'Backpack', 
                                'Bag', 'Sleeve', 'Screen Protector', 'Cleaning Kit'];
    prefixes TEXT[] := ARRAY['Pro', 'Plus', 'Max', 'Ultra', 'Lite', 'Air', 'Mini', 'Elite'];
BEGIN
    FOR i IN 1..1000 LOOP
        -- Generate UUID v7-like ID
        v_id := uuid_generate_v4();
        
        -- Generate product name
        v_name := name_parts[1 + floor(random() * array_length(name_parts, 1))] || ' ' ||
                  prefixes[1 + floor(random() * array_length(prefixes, 1))] || ' ' ||
                  (100 + floor(random() * 900))::TEXT || ' - ' || i::TEXT;
        
        -- Generate random price (100,000 to 10,000,000)
        v_price := (100000 + random() * 9900000)::NUMERIC(15,2);
        
        -- Generate random stock (10 to 500)
        v_stock := 10 + floor(random() * 491)::INTEGER;
        
        -- Set timestamps
        v_created_at := NOW() - (random() * INTERVAL '30 days');
        v_updated_at := v_created_at;
        
        -- Insert product
        INSERT INTO products (id, name, price, stock, created_at, updated_at, created_by, updated_by)
        VALUES (v_id, v_name, v_price, v_stock, v_created_at, v_updated_at, NULL, NULL);
        
        -- Progress indicator every 100 rows
        IF i % 100 = 0 THEN
            RAISE NOTICE 'Inserted % products...', i;
        END IF;
    END LOOP;
    
    RAISE NOTICE '========================================';
    RAISE NOTICE ' Successfully inserted 1000 products!';
    RAISE NOTICE '========================================';
END $$;

-- Verify insertion
SELECT COUNT(*) as total_products FROM products;
SELECT id, name, price, stock, created_at FROM products LIMIT 5;
