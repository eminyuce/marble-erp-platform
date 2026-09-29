-- V21: Entegre Operasyon, Stok, Fatura, Tahsilat ve İş Emri Modülleri

-- 1. Blok tablosu genişletmeleri
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS estimated_tonnage DECIMAL(10, 3);
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS actual_tonnage DECIMAL(10, 3);
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS target_destination VARCHAR(40) DEFAULT 'FACTORY';
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS arrival_date DATE;
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS assigned_customer_id BIGINT REFERENCES customers (id);
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS dispatch_invoice_id BIGINT;

-- Mevcut blokların tahmini ve gerçek tonajlarını backfill et
UPDATE blocks
SET estimated_tonnage = ROUND(theoretical_weight_kg / 1000.0, 3)
WHERE estimated_tonnage IS NULL AND theoretical_weight_kg IS NOT NULL;

UPDATE blocks
SET actual_tonnage = ROUND(actual_weight_kg / 1000.0, 3)
WHERE actual_tonnage IS NULL AND actual_weight_kg IS NOT NULL AND actual_weight_kg > 0;

-- 2. Yeni Ebatlı Stok Sahası lokasyonu
INSERT INTO stock_locations (code, name, business_unit, location_type, active, add_user_id, update_user_id)
SELECT 'FAB-EBATLI', 'Fabrika Ebatlı Stok Sahası', 'FACTORY', 'SIZED_STOCK_YARD', true, 'system@ozerler.com', 'system@ozerler.com'
WHERE NOT EXISTS (SELECT 1 FROM stock_locations WHERE code = 'FAB-EBATLI');

-- 3. Plaka tablosu müşteri ataması genişletmesi
ALTER TABLE slabs ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES customers (id);

-- 4. Stok Kalemleri (Stock Items - Ebatlı, Plaka, vb.)
CREATE TABLE IF NOT EXISTS stock_items (
    id BIGSERIAL PRIMARY KEY,
    item_code VARCHAR(60) NOT NULL UNIQUE,
    product_type VARCHAR(30) NOT NULL,
    source_block_id BIGINT REFERENCES blocks (id),
    stone_type VARCHAR(100),
    description VARCHAR(255),
    thickness_cm DECIMAL(8, 2),
    width_cm DECIMAL(10, 2),
    length_cm DECIMAL(10, 2),
    quantity DECIMAL(12, 4) NOT NULL DEFAULT 0,
    unit VARCHAR(20) DEFAULT 'm2',
    piece_count INT NOT NULL DEFAULT 1,
    actual_produced_quantity DECIMAL(12, 4),
    customer_id BIGINT REFERENCES customers (id),
    stock_location_id BIGINT REFERENCES stock_locations (id),
    quality_grade VARCHAR(20) DEFAULT 'A',
    surface_finish VARCHAR(50) DEFAULT 'RAW',
    edge_finish VARCHAR(50) DEFAULT 'DUZ',
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    production_date DATE NOT NULL DEFAULT CURRENT_DATE,
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_stock_items_type_status ON stock_items (product_type, status);
CREATE INDEX IF NOT EXISTS idx_stock_items_customer ON stock_items (customer_id);
CREATE INDEX IF NOT EXISTS idx_stock_items_block ON stock_items (source_block_id);

-- 5. Faturalar (Invoices - KDV'siz Ara Toplam = Genel Toplam)
CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_no VARCHAR(60) NOT NULL UNIQUE,
    invoice_date DATE NOT NULL,
    due_date DATE,
    invoice_type VARCHAR(30) NOT NULL,
    department VARCHAR(40) NOT NULL,
    customer_id BIGINT REFERENCES customers (id),
    supplier_id BIGINT REFERENCES suppliers (id),
    party_name VARCHAR(150),
    subtotal_amount DECIMAL(16, 2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(16, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'ISSUED',
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_invoices_type_dept ON invoices (invoice_type, department);
CREATE INDEX IF NOT EXISTS idx_invoices_customer ON invoices (customer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_date ON invoices (invoice_date);

-- 6. Fatura Kalemleri (Invoice Items - KDV kesinlikle yok)
CREATE TABLE IF NOT EXISTS invoice_items (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES invoices (id) ON DELETE CASCADE,
    product_name VARCHAR(150) NOT NULL,
    description VARCHAR(255),
    quantity DECIMAL(14, 4) NOT NULL DEFAULT 1.0000,
    unit VARCHAR(30) NOT NULL DEFAULT 'm2',
    unit_price DECIMAL(14, 2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(16, 2) NOT NULL DEFAULT 0.00,
    block_id BIGINT REFERENCES blocks (id),
    stock_item_id BIGINT REFERENCES stock_items (id),
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_invoice_items_invoice ON invoice_items (invoice_id);

-- 7. Tahsilatlar (Collections)
CREATE TABLE IF NOT EXISTS collections (
    id BIGSERIAL PRIMARY KEY,
    collection_no VARCHAR(60) NOT NULL UNIQUE,
    collection_date DATE NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers (id),
    invoice_id BIGINT REFERENCES invoices (id),
    collection_method VARCHAR(30) NOT NULL,
    amount DECIMAL(16, 2) NOT NULL,
    bank_name VARCHAR(100),
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_collections_customer ON collections (customer_id);
CREATE INDEX IF NOT EXISTS idx_collections_date ON collections (collection_date);

-- 8. Çek Takibi (Checks)
CREATE TABLE IF NOT EXISTS checks (
    id BIGSERIAL PRIMARY KEY,
    collection_id BIGINT REFERENCES collections (id) ON DELETE SET NULL,
    check_no VARCHAR(60) NOT NULL,
    check_date DATE NOT NULL,
    due_date DATE NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    customer_id BIGINT NOT NULL REFERENCES customers (id),
    amount DECIMAL(16, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PORTFOLIO',
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_checks_due_date ON checks (due_date);
CREATE INDEX IF NOT EXISTS idx_checks_status ON checks (status);
CREATE INDEX IF NOT EXISTS idx_checks_customer ON checks (customer_id);

-- 9. Operasyon İş Emirleri (Operation Work Orders)
CREATE TABLE IF NOT EXISTS operation_work_orders (
    id BIGSERIAL PRIMARY KEY,
    order_no VARCHAR(60) NOT NULL UNIQUE,
    customer_id BIGINT REFERENCES customers (id),
    order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    due_date DATE,
    department VARCHAR(40) NOT NULL,
    responsible_person VARCHAR(120),
    stone_type VARCHAR(100),
    color_quality VARCHAR(100),
    thickness_cm DECIMAL(8, 2),
    width_cm DECIMAL(10, 2),
    length_cm DECIMAL(10, 2),
    quantity DECIMAL(12, 4),
    quantity_unit VARCHAR(20) DEFAULT 'm2',
    surface_operation VARCHAR(60),
    edge_operation VARCHAR(60),
    status VARCHAR(30) NOT NULL DEFAULT 'NEW',
    source_stock_item_id BIGINT REFERENCES stock_items (id),
    source_slab_id BIGINT REFERENCES slabs (id),
    source_block_id BIGINT REFERENCES blocks (id),
    used_quantity DECIMAL(12, 4),
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_op_work_orders_dept_status ON operation_work_orders (department, status);
CREATE INDEX IF NOT EXISTS idx_op_work_orders_customer ON operation_work_orders (customer_id);

-- 10. İş Emri Durum Tarihçesi (Work Order Status History)
CREATE TABLE IF NOT EXISTS work_order_status_history (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES operation_work_orders (id) ON DELETE CASCADE,
    previous_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by VARCHAR(128),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_work_order_history_order ON work_order_status_history (work_order_id);

-- 11. Entegre Stok Hareketleri (Stock Movements Ledger)
CREATE TABLE IF NOT EXISTS stock_movements (
    id BIGSERIAL PRIMARY KEY,
    movement_code VARCHAR(60) NOT NULL UNIQUE,
    movement_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    movement_type VARCHAR(40) NOT NULL,
    source_department VARCHAR(40),
    target_department VARCHAR(40),
    target_destination VARCHAR(40),
    from_location_id BIGINT REFERENCES stock_locations (id),
    to_location_id BIGINT REFERENCES stock_locations (id),
    block_id BIGINT REFERENCES blocks (id),
    slab_id BIGINT REFERENCES slabs (id),
    stock_item_id BIGINT REFERENCES stock_items (id),
    item_description VARCHAR(255),
    quantity DECIMAL(14, 4) DEFAULT 1.0000,
    quantity_unit VARCHAR(30) DEFAULT 'm2',
    tonnage DECIMAL(10, 3),
    customer_id BIGINT REFERENCES customers (id),
    work_order_id BIGINT REFERENCES operation_work_orders (id),
    invoice_id BIGINT REFERENCES invoices (id),
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_date ON stock_movements (movement_date);
CREATE INDEX IF NOT EXISTS idx_stock_movements_block ON stock_movements (block_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_item ON stock_movements (stock_item_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_type ON stock_movements (movement_type);

-- 12. Tanımlar (Operation Definitions: Yüzey, Kenar, Birim)
CREATE TABLE IF NOT EXISTS operation_definitions (
    id BIGSERIAL PRIMARY KEY,
    category VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

-- Başlangıç tanımları
INSERT INTO operation_definitions (category, code, name, display_order, active, add_user_id, update_user_id)
VALUES
    ('SURFACE_OPERATION', 'HONLU', 'Honlu', 1, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('SURFACE_OPERATION', 'CILALI', 'Cilalı', 2, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('SURFACE_OPERATION', 'FIRCALI', 'Fırçalı', 3, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('SURFACE_OPERATION', 'KUMLAMA', 'Kumlama', 4, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('SURFACE_OPERATION', 'LEATHER', 'Leather / Patinato', 5, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('SURFACE_OPERATION', 'DIGER', 'Diğer', 6, true, 'system@ozerler.com', 'system@ozerler.com'),

    ('EDGE_OPERATION', 'DUZ', 'Düz', 1, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('EDGE_OPERATION', 'PAH', 'Pah', 2, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('EDGE_OPERATION', 'PROFIL', 'Profil', 3, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('EDGE_OPERATION', 'YUVARLATMA', 'Yuvarlatma', 4, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('EDGE_OPERATION', 'DIGER', 'Diğer', 5, true, 'system@ozerler.com', 'system@ozerler.com'),

    ('UNIT', 'MT', 'm.t.', 1, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('UNIT', 'M2', 'm²', 2, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('UNIT', 'M3', 'm³', 3, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('UNIT', 'TON', 'ton', 4, true, 'system@ozerler.com', 'system@ozerler.com'),
    ('UNIT', 'ADET', 'adet', 5, true, 'system@ozerler.com', 'system@ozerler.com')
ON CONFLICT (code) DO NOTHING;
