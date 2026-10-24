-- V30: Cari Hesap Hareketleri (Muhasebe Ekstresi) ve İş Emri Çoklu Ölçü & İşlem Satırları

-- 1. Cari Hesap Hareketleri (Cari Transactions / Ekstre)
CREATE TABLE IF NOT EXISTS cari_transactions (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL REFERENCES customers (id) ON DELETE CASCADE,
    transaction_date DATE NOT NULL DEFAULT CURRENT_DATE,
    transaction_type VARCHAR(40) NOT NULL,
    document_no VARCHAR(60),
    description VARCHAR(255),
    debit_amount DECIMAL(16, 2) NOT NULL DEFAULT 0.00,
    credit_amount DECIMAL(16, 2) NOT NULL DEFAULT 0.00,
    invoice_id BIGINT REFERENCES invoices (id) ON DELETE SET NULL,
    collection_id BIGINT REFERENCES collections (id) ON DELETE SET NULL,
    notes TEXT,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_cari_tx_customer ON cari_transactions (customer_id);
CREATE INDEX IF NOT EXISTS idx_cari_tx_date ON cari_transactions (transaction_date);
CREATE INDEX IF NOT EXISTS idx_cari_tx_type ON cari_transactions (transaction_type);
CREATE INDEX IF NOT EXISTS idx_cari_tx_invoice ON cari_transactions (invoice_id);
CREATE INDEX IF NOT EXISTS idx_cari_tx_collection ON cari_transactions (collection_id);

-- 2. İş Emri Satırları (Operation Work Order Measurement Items)
CREATE TABLE IF NOT EXISTS operation_work_order_items (
    id BIGSERIAL PRIMARY KEY,
    work_order_id BIGINT NOT NULL REFERENCES operation_work_orders (id) ON DELETE CASCADE,
    thickness_cm DECIMAL(8, 2),
    width_cm DECIMAL(10, 2),
    length_cm DECIMAL(10, 2),
    piece_count INT NOT NULL DEFAULT 1,
    calculated_m2 DECIMAL(10, 4),
    calculated_mt DECIMAL(10, 4),
    surface_operation VARCHAR(80),
    surface_unit VARCHAR(20) DEFAULT 'm2',
    edge_operation VARCHAR(80),
    edge_unit VARCHAR(20) DEFAULT 'm.t.',
    notes VARCHAR(255),
    item_order INT DEFAULT 0,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_op_wo_items_order ON operation_work_order_items (work_order_id);

-- 3. Mevcut Satış & Alış Faturalarını Cari Hareketlere Aktar (Geçmiş Veri Senkronizasyonu)
INSERT INTO cari_transactions (customer_id, transaction_date, transaction_type, document_no, description, debit_amount, credit_amount, invoice_id, notes, created_date, updated_date)
SELECT i.customer_id, i.invoice_date,
       CASE WHEN i.invoice_type = 'SALES' THEN 'SATIS_FATURASI' ELSE 'ALIS_FATURASI' END,
       i.invoice_no,
       CONCAT(CASE WHEN i.invoice_type = 'SALES' THEN 'Satış Faturası: ' ELSE 'Alış Faturası: ' END, i.invoice_no),
       CASE WHEN i.invoice_type = 'SALES' THEN COALESCE(i.total_amount, 0.00) ELSE 0.00 END,
       CASE WHEN i.invoice_type = 'PURCHASE' THEN COALESCE(i.total_amount, 0.00) ELSE 0.00 END,
       i.id,
       i.notes,
       i.created_date,
       i.updated_date
FROM invoices i
WHERE i.customer_id IS NOT NULL AND i.status != 'CANCELLED';

-- 4. Mevcut Tahsilat Kayıtlarını Cari Hareketlere Aktar
INSERT INTO cari_transactions (customer_id, transaction_date, transaction_type, document_no, description, debit_amount, credit_amount, collection_id, notes, created_date, updated_date)
SELECT c.customer_id, c.collection_date,
       'TAHSILAT',
       c.collection_no,
       CONCAT('Tahsilat (', c.collection_method, '): ', c.collection_no),
       0.00,
       COALESCE(c.amount, 0.00),
       c.id,
       c.notes,
       c.created_date,
       c.updated_date
FROM collections c
WHERE c.customer_id IS NOT NULL;
