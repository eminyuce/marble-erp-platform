-- Modül 1: Ocaklar ve Bloklar
CREATE TABLE quarries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(150) NOT NULL,
    specific_gravity DECIMAL(5,2) NOT NULL DEFAULT 2.70,
    license_no VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE blocks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    quarry_id BIGINT NOT NULL,
    block_code VARCHAR(50) NOT NULL UNIQUE,
    extraction_date DATE NOT NULL,
    width_cm INT NOT NULL,
    length_cm INT NOT NULL,
    height_cm INT NOT NULL,
    volume_m3 DECIMAL(10,3) NOT NULL,
    theoretical_weight_kg DECIMAL(12,2) NOT NULL,
    actual_weight_kg DECIMAL(12,2) NOT NULL,
    weight_deviation_pct DECIMAL(6,2) NOT NULL,
    stone_type VARCHAR(100) NOT NULL,
    color_tone VARCHAR(100),
    quality_grade VARCHAR(20) NOT NULL DEFAULT 'A',
    crack_level INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'QUARRY',
    extraction_cost DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    transport_cost DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    total_cost DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    notes TEXT,
    photo_urls TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_blocks_quarry FOREIGN KEY (quarry_id) REFERENCES quarries (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_blocks_code ON blocks(block_code);
CREATE INDEX idx_blocks_status ON blocks(status);

-- Modül 2: Paletler ve Plakalar
CREATE TABLE pallets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pallet_code VARCHAR(50) NOT NULL UNIQUE,
    warehouse_location VARCHAR(100),
    packaging_type VARCHAR(50) NOT NULL DEFAULT 'A_FRAME',
    qr_code_hash VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    gross_weight_kg DECIMAL(12,2) DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Modül 2: Fabrika Üretim Emirleri
CREATE TABLE production_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    block_id BIGINT NOT NULL,
    machine_name VARCHAR(100) NOT NULL,
    process_type VARCHAR(50) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME,
    duration_hours DECIMAL(6,2),
    electricity_kwh DECIMAL(10,2) DEFAULT 0.00,
    blade_wear_mm DECIMAL(6,2) DEFAULT 0.00,
    operator_name VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prod_orders_block FOREIGN KEY (block_id) REFERENCES blocks (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE slabs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slab_code VARCHAR(60) NOT NULL UNIQUE,
    order_id BIGINT NOT NULL,
    block_id BIGINT NOT NULL,
    pallet_id BIGINT,
    thickness_cm DECIMAL(5,2) NOT NULL,
    width_cm DECIMAL(8,2) NOT NULL,
    length_cm DECIMAL(8,2) NOT NULL,
    surface_area_m2 DECIMAL(10,4) NOT NULL,
    surface_finish VARCHAR(50) NOT NULL DEFAULT 'RAW',
    quality_grade VARCHAR(20) NOT NULL DEFAULT 'A',
    gloss_level INT DEFAULT 0,
    cost_per_m2 DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_slabs_order FOREIGN KEY (order_id) REFERENCES production_orders (id),
    CONSTRAINT fk_slabs_block FOREIGN KEY (block_id) REFERENCES blocks (id),
    CONSTRAINT fk_slabs_pallet FOREIGN KEY (pallet_id) REFERENCES pallets (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_slabs_code ON slabs(slab_code);
CREATE INDEX idx_slabs_status ON slabs(status);

-- Modül 4: Projeler & Şantiyeler
CREATE TABLE projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    contract_value DECIMAL(16,2) NOT NULL DEFAULT 0.00,
    estimated_cost DECIMAL(16,2) NOT NULL DEFAULT 0.00,
    actual_cost DECIMAL(16,2) NOT NULL DEFAULT 0.00,
    start_date DATE,
    delivery_date DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE project_locations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    parent_id BIGINT,
    location_name VARCHAR(150) NOT NULL,
    floor_level VARCHAR(50),
    stone_spec VARCHAR(150),
    planned_area_m2 DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    installed_area_m2 DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(30) NOT NULL DEFAULT 'PLANNED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_locations_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_locations_parent FOREIGN KEY (parent_id) REFERENCES project_locations (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Modül 3: Atölye & Ebatlama İmalatı
CREATE TABLE cut_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    cut_order_no VARCHAR(50) NOT NULL UNIQUE,
    project_id BIGINT,
    location_id BIGINT,
    machine_name VARCHAR(100) NOT NULL,
    operator_name VARCHAR(100),
    planned_start DATE,
    status VARCHAR(30) NOT NULL DEFAULT 'COMPLETED',
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cut_orders_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE SET NULL,
    CONSTRAINT fk_cut_orders_location FOREIGN KEY (location_id) REFERENCES project_locations (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cut_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_code VARCHAR(60) NOT NULL UNIQUE,
    cut_order_id BIGINT NOT NULL,
    source_slab_id BIGINT NOT NULL,
    width_cm DECIMAL(8,2) NOT NULL,
    length_cm DECIMAL(8,2) NOT NULL,
    thickness_cm DECIMAL(5,2) NOT NULL,
    area_m2 DECIMAL(10,4) NOT NULL,
    edge_finish VARCHAR(100),
    unit_cost DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    target_location VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'READY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cut_items_order FOREIGN KEY (cut_order_id) REFERENCES cut_orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_cut_items_slab FOREIGN KEY (source_slab_id) REFERENCES slabs (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Fire Takibi (10 Neden Kodlu)
CREATE TABLE scrap_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scrap_code VARCHAR(50) NOT NULL UNIQUE,
    order_id BIGINT,
    cut_order_id BIGINT,
    block_id BIGINT,
    slab_id BIGINT,
    reason_code VARCHAR(20) NOT NULL,
    scrap_weight_kg DECIMAL(10,2) DEFAULT 0.00,
    scrap_area_m2 DECIMAL(10,4) DEFAULT 0.00,
    cost_impact DECIMAL(14,2) NOT NULL DEFAULT 0.00,
    description TEXT,
    logged_by VARCHAR(100),
    logged_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_scrap_order FOREIGN KEY (order_id) REFERENCES production_orders (id) ON DELETE SET NULL,
    CONSTRAINT fk_scrap_cut_order FOREIGN KEY (cut_order_id) REFERENCES cut_orders (id) ON DELETE SET NULL,
    CONSTRAINT fk_scrap_block FOREIGN KEY (block_id) REFERENCES blocks (id) ON DELETE SET NULL,
    CONSTRAINT fk_scrap_slab FOREIGN KEY (slab_id) REFERENCES slabs (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Şantiye Tüketimleri & İşçilik Puantajı
CREATE TABLE site_consumptions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    location_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    consumption_type VARCHAR(50) NOT NULL,
    item_name VARCHAR(150) NOT NULL,
    quantity DECIMAL(12,2) NOT NULL,
    unit VARCHAR(30) NOT NULL,
    unit_cost DECIMAL(12,2) NOT NULL,
    total_cost DECIMAL(14,2) NOT NULL,
    notes TEXT,
    recorded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_site_cons_location FOREIGN KEY (location_id) REFERENCES project_locations (id) ON DELETE CASCADE,
    CONSTRAINT fk_site_cons_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Masraf Merkezleri & Aktivite Tabanlı Maliyetleme (ABC)
CREATE TABLE cost_centers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    monthly_budget DECIMAL(16,2) NOT NULL DEFAULT 0.00,
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cost_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    center_id BIGINT NOT NULL,
    block_id BIGINT,
    slab_id BIGINT,
    project_id BIGINT,
    expense_type VARCHAR(50) NOT NULL,
    amount DECIMAL(14,2) NOT NULL,
    allocation_key VARCHAR(100),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_cost_trans_center FOREIGN KEY (center_id) REFERENCES cost_centers (id),
    CONSTRAINT fk_cost_trans_block FOREIGN KEY (block_id) REFERENCES blocks (id) ON DELETE SET NULL,
    CONSTRAINT fk_cost_trans_slab FOREIGN KEY (slab_id) REFERENCES slabs (id) ON DELETE SET NULL,
    CONSTRAINT fk_cost_trans_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Stok Rezervasyonları
CREATE TABLE stock_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    slab_id BIGINT,
    block_id BIGINT,
    project_id BIGINT NOT NULL,
    reserved_area_m2 DECIMAL(10,2) NOT NULL,
    reserved_until DATETIME,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_res_slab FOREIGN KEY (slab_id) REFERENCES slabs (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_res_block FOREIGN KEY (block_id) REFERENCES blocks (id) ON DELETE CASCADE,
    CONSTRAINT fk_stock_res_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Sevkiyat & Lojistik
CREATE TABLE shipments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    waybill_no VARCHAR(50) NOT NULL UNIQUE,
    project_id BIGINT,
    vehicle_plate VARCHAR(30) NOT NULL,
    driver_name VARCHAR(100) NOT NULL,
    departure_time DATETIME NOT NULL,
    distance_km INT NOT NULL DEFAULT 0,
    freight_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'IN_TRANSIT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shipments_project FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
