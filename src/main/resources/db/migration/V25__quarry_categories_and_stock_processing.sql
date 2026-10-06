-- V25: Quarry Categories, Stock Cards, and Invoice Stock Processing Lifecycle

-- 1. Invoices: stock_processed and direct_expense tracking
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS stock_processed BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS direct_expense BOOLEAN DEFAULT false;

-- Backfill existing issued/paid invoices as already processed
UPDATE invoices SET stock_processed = true WHERE status IN ('ISSUED', 'PAID', 'PARTIALLY_PAID');

-- 2. Stock Items: quarry_category column
ALTER TABLE stock_items ADD COLUMN IF NOT EXISTS quarry_category VARCHAR(30);

-- Backfill quarry categories for existing stock items
UPDATE stock_items SET quarry_category = 'MAZOT' WHERE item_code = 'OCAK-MAZOT-DEPO' OR product_type = 'FUEL';
UPDATE stock_items SET quarry_category = 'SARF_MALZEME' WHERE product_type = 'CONSUMABLE' AND quarry_category IS NULL;

-- 3. Seed standard stock cards for Quarry under OCK-SRF-01 location
INSERT INTO stock_items (item_code, product_type, quarry_category, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'SRF-DISK-350', 'CONSUMABLE', 'SARF_MALZEME', 'Kesici Disk 350mm', 20, 'adet', 350.00, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'OCK-SRF-01'
ON CONFLICT (item_code) DO NOTHING;

INSERT INTO stock_items (item_code, product_type, quarry_category, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'SRF-FLT-01', 'CONSUMABLE', 'SARF_MALZEME', 'Hava & Yağ Filtresi', 15, 'adet', 450.00, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'OCK-SRF-01'
ON CONFLICT (item_code) DO NOTHING;

INSERT INTO stock_items (item_code, product_type, quarry_category, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'SRF-YAG-15W40', 'CONSUMABLE', 'SARF_MALZEME', 'Motor Yağı 15W-40', 100, 'litre', 120.00, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'OCK-SRF-01'
ON CONFLICT (item_code) DO NOTHING;

INSERT INTO stock_items (item_code, product_type, quarry_category, description, quantity, unit, unit_price, status, stock_location_id, add_user_id, update_user_id)
SELECT 'DGR-MLZ-01', 'OTHER', 'DIGER', 'Çeşitli Sarf / Hırdavat', 50, 'adet', 75.00, 'AVAILABLE', id, 'system@ozerler.com', 'system@ozerler.com'
FROM stock_locations WHERE code = 'OCK-SRF-01'
ON CONFLICT (item_code) DO NOTHING;
