-- ============================================================================
-- V28: Add business_unit to customers and suppliers (Cari Hesaplar Modülü)
-- ============================================================================

ALTER TABLE customers ADD COLUMN IF NOT EXISTS business_unit VARCHAR(30);
ALTER TABLE suppliers ADD COLUMN IF NOT EXISTS business_unit VARCHAR(30);

CREATE INDEX IF NOT EXISTS idx_customers_business_unit ON customers (business_unit);
CREATE INDEX IF NOT EXISTS idx_suppliers_business_unit ON suppliers (business_unit);
