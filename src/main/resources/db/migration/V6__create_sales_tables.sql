CREATE TABLE IF NOT EXISTS customers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_code VARCHAR(50) NOT NULL UNIQUE,
    company_name VARCHAR(200) NOT NULL,
    contact_person VARCHAR(150),
    phone VARCHAR(30),
    email VARCHAR(150),
    address TEXT,
    tax_office VARCHAR(100),
    tax_number VARCHAR(20),
    customer_type VARCHAR(30) NOT NULL DEFAULT 'CONSTRUCTION',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    order_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    delivery_date DATE,
    total_amount DECIMAL(16,2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(16,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_sales_customer FOREIGN KEY (customer_id) REFERENCES customers(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sales_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sales_order_id BIGINT NOT NULL,
    slab_id BIGINT,
    block_id BIGINT,
    description VARCHAR(300) NOT NULL,
    quantity DECIMAL(12,2) NOT NULL DEFAULT 1.00,
    unit VARCHAR(30) NOT NULL DEFAULT 'm2',
    unit_price DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    line_total DECIMAL(16,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_soi_order FOREIGN KEY (sales_order_id) REFERENCES sales_orders(id),
    CONSTRAINT fk_soi_slab FOREIGN KEY (slab_id) REFERENCES slabs(id),
    CONSTRAINT fk_soi_block FOREIGN KEY (block_id) REFERENCES blocks(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Seed some demo customers
INSERT INTO customers (customer_code, company_name, contact_person, phone, email, tax_office, tax_number, customer_type) VALUES
('MUS-001', 'Atlas Mermer Uygulama A.Ş.', 'Mehmet Yıldırım', '0532-111-2233', 'info@atlasmermer.com', 'Mecidiyeköy', '1234567890', 'MARBLE_APPLICATION'),
('MUS-002', 'Yeşilyurt İnşaat Ltd. Şti.', 'Ayşe Kara', '0533-444-5566', 'satin@yesilyurt.com', 'Kadıköy', '9876543210', 'CONSTRUCTION'),
('MUS-003', 'Başarı Yapı Malzemeleri', 'Hasan Demir', '0535-777-8899', 'bilgi@basariyapi.com', 'Beyoğlu', '5678901234', 'DEALER');
