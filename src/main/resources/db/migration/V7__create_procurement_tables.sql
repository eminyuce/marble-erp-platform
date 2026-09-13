-- Satın Alma / Tedarik Modülü

CREATE TABLE IF NOT EXISTS suppliers
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    supplier_code
    VARCHAR
(
    50
) NOT NULL UNIQUE,
    company_name VARCHAR
(
    200
) NOT NULL,
    contact_person VARCHAR
(
    150
),
    phone VARCHAR
(
    30
),
    email VARCHAR
(
    150
),
    address TEXT,
    tax_office VARCHAR
(
    100
),
    tax_number VARCHAR
(
    20
),
    supplier_type VARCHAR
(
    50
) NOT NULL DEFAULT 'CONSUMABLE',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    );

CREATE TABLE IF NOT EXISTS purchase_orders
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    po_number
    VARCHAR
(
    50
) NOT NULL UNIQUE,
    supplier_id BIGINT NOT NULL,
    project_id BIGINT,
    order_date DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_delivery DATE,
    actual_delivery DATE,
    total_amount DECIMAL
(
    16,
    2
) NOT NULL DEFAULT 0.00,
    status VARCHAR
(
    30
) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_po_supplier FOREIGN KEY
(
    supplier_id
) REFERENCES suppliers
(
    id
),
    CONSTRAINT fk_po_project FOREIGN KEY
(
    project_id
) REFERENCES projects
(
    id
)
    );

CREATE TABLE IF NOT EXISTS purchase_order_items
(
    id
    BIGSERIAL
    PRIMARY
    KEY,
    purchase_order_id
    BIGINT
    NOT
    NULL,
    item_name
    VARCHAR
(
    300
) NOT NULL,
    item_type VARCHAR
(
    50
) NOT NULL DEFAULT 'CONSUMABLE',
    quantity DECIMAL
(
    12,
    2
) NOT NULL DEFAULT 1.00,
    unit VARCHAR
(
    30
) NOT NULL DEFAULT 'ADET',
    unit_price DECIMAL
(
    14,
    2
) NOT NULL DEFAULT 0.00,
    line_total DECIMAL
(
    16,
    2
) NOT NULL DEFAULT 0.00,
    delivery_status VARCHAR
(
    30
) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_poi_order FOREIGN KEY
(
    purchase_order_id
) REFERENCES purchase_orders
(
    id
)
    );

-- Seed demo suppliers
INSERT INTO suppliers (supplier_code, company_name, contact_person, phone, email, supplier_type)
VALUES ('TED-001', 'Akçansa Çimento A.Ş.', 'Ali Vural', '0212-555-1234', 'satis@akcansa.com', 'CONSUMABLE'),
       ('TED-002', 'Betek Yapı Kimyasalları', 'Zeynep Ak', '0216-666-7890', 'siparis@betek.com', 'CHEMICAL'),
       ('TED-003', 'Kale Mermer Aletleri', 'Burak Taş', '0258-333-4567', 'info@kalealet.com',
        'EQUIPMENT') ON CONFLICT (supplier_code) DO NOTHING;

-- Synchronize sequences with inserted IDs
SELECT setval(pg_get_serial_sequence('suppliers', 'id'), COALESCE((SELECT MAX(id) FROM suppliers), 1));
