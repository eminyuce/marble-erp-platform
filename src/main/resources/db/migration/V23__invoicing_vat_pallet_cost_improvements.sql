-- V23: Invoicing VAT, internal unit transfers, project cost attribution, pallet item dimensions, and running meter support

-- 1. Invoices: VAT & internal unit transfer & project tracking
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS tax_rate DECIMAL(5, 2) DEFAULT 20.00;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS tax_amount DECIMAL(16, 2) NOT NULL DEFAULT 0.00;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS target_department VARCHAR(40);
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS project_id BIGINT REFERENCES projects (id);

CREATE INDEX IF NOT EXISTS idx_invoices_project ON invoices (project_id);
CREATE INDEX IF NOT EXISTS idx_invoices_target_dept ON invoices (target_department);

-- 2. Invoice Items: slab reference, width (for running meter), and calculated m2
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS slab_id BIGINT REFERENCES slabs (id);
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS width_cm DECIMAL(10, 2);
ALTER TABLE invoice_items ADD COLUMN IF NOT EXISTS calculated_m2 DECIMAL(12, 4);

CREATE INDEX IF NOT EXISTS idx_invoice_items_slab ON invoice_items (slab_id);

-- 3. Pallets: department (FACTORY vs WORKSHOP)
ALTER TABLE pallets ADD COLUMN IF NOT EXISTS department VARCHAR(40) DEFAULT 'FACTORY';

-- 4. Pallet Items: dimensions, product name, stock item / slab links, nullable lot
ALTER TABLE pallet_items ALTER COLUMN material_lot_id DROP NOT NULL;
ALTER TABLE pallet_items ADD COLUMN IF NOT EXISTS product_name VARCHAR(150);
ALTER TABLE pallet_items ADD COLUMN IF NOT EXISTS width_cm DECIMAL(10, 2);
ALTER TABLE pallet_items ADD COLUMN IF NOT EXISTS length_cm DECIMAL(10, 2);
ALTER TABLE pallet_items ADD COLUMN IF NOT EXISTS thickness_cm DECIMAL(8, 2);
ALTER TABLE pallet_items ADD COLUMN IF NOT EXISTS stock_item_id BIGINT REFERENCES stock_items (id);
ALTER TABLE pallet_items ADD COLUMN IF NOT EXISTS slab_id BIGINT REFERENCES slabs (id);

CREATE INDEX IF NOT EXISTS idx_pallet_items_stock_item ON pallet_items (stock_item_id);
CREATE INDEX IF NOT EXISTS idx_pallet_items_slab ON pallet_items (slab_id);
