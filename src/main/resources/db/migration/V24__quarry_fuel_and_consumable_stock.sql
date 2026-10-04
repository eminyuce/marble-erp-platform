-- V24: Quarry Fuel Tank, Consumables Warehouse, and Machine Enhancements

ALTER TABLE stock_items ADD COLUMN IF NOT EXISTS unit_price DECIMAL(14, 2) DEFAULT 0;

-- 1. Ocak Depo Lokasyonları
INSERT INTO stock_locations (code, name, business_unit, location_type, active, add_user_id, update_user_id)
VALUES ('OCK-MZ-01', 'Ocak Mazot Deposu', 'QUARRY', 'QUARRY_FUEL_TANK', true, 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

INSERT INTO stock_locations (code, name, business_unit, location_type, active, add_user_id, update_user_id)
VALUES ('OCK-SRF-01', 'Ocak Sarf Malzeme Deposu', 'QUARRY', 'QUARRY_CONSUMABLES_WAREHOUSE', true, 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

-- 2. Standart Ocak Makineleri
INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('OCK-EKS-01', 'Ekskavatör 1', 'QUARRY', 'QUARRY_MACHINE', 'Ocak ana üretim ekskavatörü', 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('OCK-EKS-02', 'Ekskavatör 2', 'QUARRY', 'QUARRY_MACHINE', 'Ocak yedek/kademe ekskavatörü', 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('OCK-LDR-01', 'Loader 1', 'QUARRY', 'QUARRY_MACHINE', 'Ocak yükleyici loder', 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('OCK-KMY-01', 'Kamyon 1', 'QUARRY', 'QUARRY_MACHINE', 'Ocak hafriyat/nakliye kamyonu', 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

-- 3. Ocak Mazot Deposu Stok Kalemi
INSERT INTO stock_items (item_code, product_type, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'OCAK-MAZOT-DEPO', 'FUEL', 'Ocak Mazot Deposu (Ana Tank)', 0, 'litre', 0, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'OCK-MZ-01'
ON CONFLICT (item_code) DO NOTHING;
