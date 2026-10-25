-- V31: Dahili Malzeme Transferi (Şantiye Depo), Fatura Fire Miktarı ve Atölye Üretimi

-- 1. stock_items tablosuna proje (şantiye) ilişkisi ekle
ALTER TABLE stock_items ADD COLUMN IF NOT EXISTS project_id BIGINT REFERENCES projects(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_stock_items_project ON stock_items(project_id);

-- 2. invoice_items tablosuna fire miktarı (waste_quantity) ekle
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS waste_quantity NUMERIC(14, 4) DEFAULT 0.0000;

-- 3. Şantiye Depo / Stoku lokasyonunu sisteme ekle (yoksa)
INSERT INTO stock_locations (code, name, business_unit, location_type, active, created_date, updated_date)
SELECT 'SNT-STK-01', 'Şantiye Depo / Stoku', 'SITE', 'SITE_STOCK', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM stock_locations WHERE code = 'SNT-STK-01');
