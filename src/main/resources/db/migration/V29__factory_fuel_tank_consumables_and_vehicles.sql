-- V29: Factory Fuel Tank, Consumables Warehouse, and Diesel Vehicles (Forklift & Trucks)

-- 1. Fabrika Depo Lokasyonları
INSERT INTO stock_locations (code, name, business_unit, location_type, active, add_user_id, update_user_id)
VALUES ('FB-MZ-01', 'Fabrika Mazot Deposu', 'FACTORY', 'FACTORY_FUEL_TANK', true, 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

INSERT INTO stock_locations (code, name, business_unit, location_type, active, add_user_id, update_user_id)
VALUES ('FB-SRF-01', 'Fabrika Sarf Malzeme Deposu', 'FACTORY', 'FACTORY_CONSUMABLES_WAREHOUSE', true, 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

-- 2. Fabrika Mazot Deposu Stok Kalemi
INSERT INTO stock_items (item_code, product_type, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'FABRIKA-MAZOT-DEPO', 'FUEL', 'Fabrika Mazot Deposu (Ana Tank)', 0, 'litre', 0, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'FB-MZ-01'
ON CONFLICT (item_code) DO NOTHING;

-- 3. Fabrika Dizel Araçları & Makineleri (Forklift ve Tırlar)
INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('FCT-FRK-01', 'Forklift 1 (Dizel)', 'FACTORY', 'FORKLIFT', 'Fabrika saha yükleme forklifti', 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('FCT-FRK-02', 'Forklift 2 (Dizel)', 'FACTORY', 'FORKLIFT', 'Fabrika depo forklifti', 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('FCT-TIR-01', 'Tır 1 (Mercedes Actros)', 'FACTORY', 'TRUCK', 'Fabrika blok ve ebatlı sevkiyat tırı', 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('FCT-TIR-02', 'Tır 2 (Scania R500)', 'FACTORY', 'TRUCK', 'Fabrika nakliye ve liman sevkiyat tırı', 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;

-- 4. Fabrika Sarf Malzeme Başlangıç Stok Kartları
INSERT INTO stock_items (item_code, product_type, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'FB-SRF-SEG-01', 'CONSUMABLE', 'Katrak Elmas Segman', 120, 'adet', 85.00, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'FB-SRF-01'
ON CONFLICT (item_code) DO NOTHING;

INSERT INTO stock_items (item_code, product_type, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'FB-SRF-TEL-01', 'CONSUMABLE', 'Elmas Kesme Teli (Plaka & Blok)', 350, 'm.t.', 140.00, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'FB-SRF-01'
ON CONFLICT (item_code) DO NOTHING;

INSERT INTO stock_items (item_code, product_type, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'FB-SRF-EPX-01', 'CONSUMABLE', 'Mermer Epoksi Reçine & Dolgu', 45, 'kg', 220.00, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'FB-SRF-01'
ON CONFLICT (item_code) DO NOTHING;

INSERT INTO stock_items (item_code, product_type, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'FB-SRF-CIL-01', 'CONSUMABLE', 'Cila Keçesi & Parlatma Taşı', 80, 'adet', 175.00, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'FB-SRF-01'
ON CONFLICT (item_code) DO NOTHING;
