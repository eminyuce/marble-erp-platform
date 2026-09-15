-- Özerler Mermer ERP Aşama 1–7: ortak altyapı, ocak/fabrika/atölye/şantiye genişletmesi.
-- Mevcut V1–V10 dosyaları değiştirilmez. Yeni kolonlar önce nullable, sonra backfill edilir.

CREATE TABLE data_migration_warnings
(
    id             BIGSERIAL PRIMARY KEY,
    source_table   VARCHAR(80)  NOT NULL,
    source_id      BIGINT,
    field_name     VARCHAR(80)  NOT NULL,
    original_value TEXT,
    message        TEXT         NOT NULL,
    created_date   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE machines
(
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(40)  NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    business_unit VARCHAR(30)  NOT NULL,
    machine_type  VARCHAR(40)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    notes         TEXT,
    created_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id   VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE TABLE stock_locations
(
    id            BIGSERIAL PRIMARY KEY,
    code          VARCHAR(40)  NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    business_unit VARCHAR(30)  NOT NULL,
    location_type VARCHAR(40)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id   VARCHAR(128),
    update_user_id VARCHAR(128)
);

INSERT INTO stock_locations (code, name, business_unit, location_type, add_user_id, update_user_id)
VALUES ('OCAK-URETIM', 'Üretim Sahası', 'QUARRY', 'PRODUCTION_YARD', 'system@ozerler.com', 'system@ozerler.com'),
       ('OCAK-SEVK', 'Sevkiyat Sahası', 'QUARRY', 'DISPATCH_YARD', 'system@ozerler.com', 'system@ozerler.com'),
       ('FAB-BLOK', 'Fabrika Blok Sahası', 'FACTORY', 'FACTORY_BLOCK_YARD', 'system@ozerler.com', 'system@ozerler.com'),
       ('FAB-PLAKA', 'Plaka Stok Sahası', 'FACTORY', 'SLAB_STOCK_YARD', 'system@ozerler.com', 'system@ozerler.com'),
       ('FAB-PALET', 'Palet Stok Sahası', 'FACTORY', 'PALLET_STOCK_YARD', 'system@ozerler.com', 'system@ozerler.com'),
       ('ATL-STOK', 'Atölye Stoku', 'WORKSHOP', 'WORKSHOP_STOCK', 'system@ozerler.com', 'system@ozerler.com');

INSERT INTO machines (code, name, business_unit, machine_type, notes, add_user_id, update_user_id)
VALUES ('OCK-MK-01', 'Ocak Ekskavatör 01', 'QUARRY', 'QUARRY_MACHINE', 'Ocak üretim makinesi', 'system@ozerler.com',
        'system@ozerler.com'),
       ('OCK-MK-02', 'Ocak Loader 01', 'QUARRY', 'QUARRY_MACHINE', 'Ocak yükleme makinesi', 'system@ozerler.com',
        'system@ozerler.com'),
       ('FAB-KATRAK-01', 'Katrak-01', 'FACTORY', 'GANGSAW', '80 lamalı katrak', 'system@ozerler.com',
        'system@ozerler.com'),
       ('FAB-ST-01', 'ST-01', 'FACTORY', 'ST', 'ST kesim makinesi', 'system@ozerler.com', 'system@ozerler.com'),
       ('FAB-PSILIM-01', 'Plaka Silim 01', 'FACTORY', 'SLAB_POLISHING', NULL, 'system@ozerler.com', 'system@ozerler.com'),
       ('FAB-BSILIM-01', 'Bant Silim 01', 'FACTORY', 'STRIP_POLISHING', NULL, 'system@ozerler.com', 'system@ozerler.com'),
       ('FAB-KOPRU-01', 'Fabrika Köprü Kesme 01', 'FACTORY', 'FACTORY_BRIDGE_SAW', NULL, 'system@ozerler.com',
        'system@ozerler.com'),
       ('ATL-KOPRU-01', 'Atölye Köprü Kesme 01', 'WORKSHOP', 'WORKSHOP_BRIDGE_SAW', NULL, 'system@ozerler.com',
        'system@ozerler.com'),
       ('ATL-KOPRU-02', 'Atölye Köprü Kesme 02', 'WORKSHOP', 'WORKSHOP_BRIDGE_SAW', NULL, 'system@ozerler.com',
        'system@ozerler.com'),
       ('ATL-KENAR-01', 'Kenar Kesme 01', 'WORKSHOP', 'EDGE_CUTTER', NULL, 'system@ozerler.com', 'system@ozerler.com'),
       ('ATL-PAH-01', 'Pah Makinesi 01', 'WORKSHOP', 'CHAMFER_MACHINE', NULL, 'system@ozerler.com', 'system@ozerler.com');

ALTER TABLE blocks
    ADD COLUMN IF NOT EXISTS current_location_id BIGINT;

ALTER TABLE blocks
    ADD CONSTRAINT fk_blocks_current_location FOREIGN KEY (current_location_id) REFERENCES stock_locations (id);

UPDATE blocks
SET status = CASE status
                 WHEN 'QUARRY' THEN 'PRODUCED'
                 WHEN 'IN_TRANSIT' THEN 'DISPATCHED'
                 WHEN 'FACTORY_STOCK' THEN 'AT_FACTORY'
                 WHEN 'SAWING' THEN 'IN_PROCESS'
                 ELSE status
    END
WHERE status IN ('QUARRY', 'IN_TRANSIT', 'FACTORY_STOCK', 'SAWING');

UPDATE blocks b
SET current_location_id = loc.id
FROM stock_locations loc
WHERE b.current_location_id IS NULL
  AND (
    (b.status IN ('PRODUCED', 'MARKED') AND loc.code = 'OCAK-URETIM')
        OR (b.status = 'DISPATCHED' AND loc.code = 'OCAK-SEVK')
        OR (b.status IN ('AT_FACTORY', 'IN_PROCESS') AND loc.code = 'FAB-BLOK')
    );

CREATE TABLE block_location_movements
(
    id               BIGSERIAL PRIMARY KEY,
    block_id         BIGINT      NOT NULL REFERENCES blocks (id),
    from_location_id BIGINT REFERENCES stock_locations (id),
    to_location_id   BIGINT      NOT NULL REFERENCES stock_locations (id),
    description      VARCHAR(255),
    created_date     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date     TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id      VARCHAR(128),
    update_user_id   VARCHAR(128)
);

CREATE INDEX idx_block_location_movements_block ON block_location_movements (block_id);

CREATE TABLE block_customer_marks
(
    id                     BIGSERIAL PRIMARY KEY,
    block_id               BIGINT         NOT NULL REFERENCES blocks (id),
    customer_id            BIGINT         NOT NULL REFERENCES customers (id),
    marked_at              DATE           NOT NULL,
    valid_until            DATE,
    offer_price            DECIMAL(14, 2) NOT NULL DEFAULT 0,
    currency               VARCHAR(3)     NOT NULL DEFAULT 'TRY',
    status                 VARCHAR(40)    NOT NULL DEFAULT 'ACTIVE',
    stock_location_id      BIGINT REFERENCES stock_locations (id),
    sales_order_item_id    BIGINT REFERENCES sales_order_items (id),
    created_date           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id            VARCHAR(128),
    update_user_id         VARCHAR(128)
);

CREATE UNIQUE INDEX uq_block_customer_marks_active ON block_customer_marks (block_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_block_customer_marks_customer ON block_customer_marks (customer_id);

CREATE TABLE machine_fuel_entries
(
    id             BIGSERIAL PRIMARY KEY,
    machine_id     BIGINT          NOT NULL REFERENCES machines (id),
    entry_date     DATE            NOT NULL,
    litres         DECIMAL(12, 3)  NOT NULL,
    price_per_litre DECIMAL(12, 4) NOT NULL,
    total_amount   DECIMAL(14, 2)  NOT NULL,
    receipt_no     VARCHAR(60),
    issued_by      VARCHAR(128),
    received_by    VARCHAR(128),
    notes          TEXT,
    created_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id    VARCHAR(128),
    update_user_id VARCHAR(128)
);

CREATE INDEX idx_machine_fuel_entries_machine_date ON machine_fuel_entries (machine_id, entry_date);

CREATE TABLE factory_work_orders
(
    id                   BIGSERIAL PRIMARY KEY,
    order_no             VARCHAR(50) NOT NULL UNIQUE,
    block_id             BIGINT      NOT NULL REFERENCES blocks (id),
    accepted_at          DATE,
    stock_location_id    BIGINT REFERENCES stock_locations (id),
    assigned_machine_id  BIGINT REFERENCES machines (id),
    status               VARCHAR(40) NOT NULL DEFAULT 'ACCEPTED',
    responsible_name     VARCHAR(120),
    notes                TEXT,
    created_date         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date         TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id          VARCHAR(128),
    update_user_id       VARCHAR(128)
);

CREATE INDEX idx_factory_work_orders_block ON factory_work_orders (block_id);
CREATE INDEX idx_factory_work_orders_status ON factory_work_orders (status);

CREATE TABLE factory_operations
(
    id                    BIGSERIAL PRIMARY KEY,
    work_order_id         BIGINT         NOT NULL REFERENCES factory_work_orders (id),
    previous_operation_id BIGINT REFERENCES factory_operations (id),
    process_type          VARCHAR(40)    NOT NULL,
    machine_id            BIGINT REFERENCES machines (id),
    operator_name         VARCHAR(120),
    started_at            TIMESTAMP,
    finished_at           TIMESTAMP,
    input_quantity        DECIMAL(14, 4) NOT NULL DEFAULT 0,
    input_unit            VARCHAR(30),
    output_quantity       DECIMAL(14, 4) NOT NULL DEFAULT 0,
    output_unit           VARCHAR(30),
    waste_quantity        DECIMAL(14, 4) NOT NULL DEFAULT 0,
    waste_unit            VARCHAR(30),
    chamfer_status        VARCHAR(30)    NOT NULL DEFAULT 'NOT_APPLICABLE',
    status                VARCHAR(30)    NOT NULL DEFAULT 'PLANNED',
    notes                 TEXT,
    created_date          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id           VARCHAR(128),
    update_user_id        VARCHAR(128)
);

CREATE INDEX idx_factory_operations_work_order ON factory_operations (work_order_id);
CREATE INDEX idx_factory_operations_process ON factory_operations (process_type, status);

ALTER TABLE production_orders
    ADD COLUMN IF NOT EXISTS machine_id BIGINT REFERENCES machines (id);
ALTER TABLE production_orders
    ADD COLUMN IF NOT EXISTS factory_work_order_id BIGINT REFERENCES factory_work_orders (id);
ALTER TABLE production_orders
    ADD COLUMN IF NOT EXISTS factory_operation_id BIGINT REFERENCES factory_operations (id);

CREATE TABLE material_lots
(
    id                            BIGSERIAL PRIMARY KEY,
    lot_code                      VARCHAR(60)    NOT NULL UNIQUE,
    product_form                  VARCHAR(30)    NOT NULL,
    source_operation_id           BIGINT REFERENCES factory_operations (id),
    source_workshop_operation_id  BIGINT,
    source_block_id               BIGINT REFERENCES blocks (id),
    slab_id                       BIGINT REFERENCES slabs (id),
    cut_item_id                   BIGINT REFERENCES cut_items (id),
    stone_type                    VARCHAR(100),
    quality_grade                 VARCHAR(20),
    surface_finish                VARCHAR(30),
    thickness_cm                  DECIMAL(8, 2),
    width_cm                      DECIMAL(10, 2),
    length_cm                     DECIMAL(10, 2),
    quantity                      INT            NOT NULL DEFAULT 1,
    total_area_m2                 DECIMAL(12, 4) NOT NULL DEFAULT 0,
    chamfer_status                VARCHAR(30)    NOT NULL DEFAULT 'NOT_APPLICABLE',
    stock_location_id             BIGINT REFERENCES stock_locations (id),
    status                        VARCHAR(30)    NOT NULL DEFAULT 'AVAILABLE',
    unit_cost                     DECIMAL(14, 2) NOT NULL DEFAULT 0,
    total_cost                    DECIMAL(16, 2) NOT NULL DEFAULT 0,
    created_date                  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date                  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id                   VARCHAR(128),
    update_user_id                VARCHAR(128)
);

CREATE INDEX idx_material_lots_block ON material_lots (source_block_id);
CREATE INDEX idx_material_lots_status ON material_lots (status, product_form);

CREATE TABLE pallet_items
(
    id              BIGSERIAL PRIMARY KEY,
    pallet_id       BIGINT         NOT NULL REFERENCES pallets (id),
    material_lot_id BIGINT         NOT NULL REFERENCES material_lots (id),
    quantity        INT            NOT NULL DEFAULT 1,
    area_m2         DECIMAL(12, 4) NOT NULL DEFAULT 0,
    created_date    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id     VARCHAR(128),
    update_user_id  VARCHAR(128)
);

CREATE TABLE shipment_items
(
    id              BIGSERIAL PRIMARY KEY,
    shipment_id     BIGINT         NOT NULL REFERENCES shipments (id),
    pallet_id       BIGINT REFERENCES pallets (id),
    material_lot_id BIGINT REFERENCES material_lots (id),
    block_id        BIGINT REFERENCES blocks (id),
    quantity        INT            NOT NULL DEFAULT 1,
    area_m2         DECIMAL(12, 4) DEFAULT 0,
    created_date    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date    TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id     VARCHAR(128),
    update_user_id  VARCHAR(128)
);

CREATE TABLE workshop_material_receipts
(
    id                      BIGSERIAL PRIMARY KEY,
    receipt_no              VARCHAR(50)    NOT NULL UNIQUE,
    source                  VARCHAR(40)    NOT NULL,
    supplier_id             BIGINT REFERENCES suppliers (id),
    purchase_order_item_id  BIGINT REFERENCES purchase_order_items (id),
    material_lot_id         BIGINT REFERENCES material_lots (id),
    quantity                DECIMAL(12, 4) NOT NULL,
    area_m2                 DECIMAL(12, 4) NOT NULL DEFAULT 0,
    purchase_cost           DECIMAL(14, 2) NOT NULL DEFAULT 0,
    received_at             DATE           NOT NULL,
    notes                   TEXT,
    created_date            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date            TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id             VARCHAR(128),
    update_user_id          VARCHAR(128)
);

CREATE TABLE workshop_operations
(
    id             BIGSERIAL PRIMARY KEY,
    cut_order_id   BIGINT         NOT NULL REFERENCES cut_orders (id),
    process_type   VARCHAR(40)    NOT NULL,
    machine_id     BIGINT REFERENCES machines (id),
    operator_name  VARCHAR(120),
    labor_hours    DECIMAL(8, 2),
    started_at     TIMESTAMP,
    finished_at    TIMESTAMP,
    input_area_m2  DECIMAL(12, 4) NOT NULL DEFAULT 0,
    output_area_m2 DECIMAL(12, 4) NOT NULL DEFAULT 0,
    waste_area_m2  DECIMAL(12, 4) NOT NULL DEFAULT 0,
    area_unit      VARCHAR(30)    NOT NULL DEFAULT 'SQUARE_METER',
    extra_expense  DECIMAL(14, 2) NOT NULL DEFAULT 0,
    status         VARCHAR(30)    NOT NULL DEFAULT 'COMPLETED',
    notes          TEXT,
    created_date   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id    VARCHAR(128),
    update_user_id VARCHAR(128)
);

ALTER TABLE material_lots
    ADD CONSTRAINT fk_material_lots_workshop_op FOREIGN KEY (source_workshop_operation_id) REFERENCES workshop_operations (id);

ALTER TABLE cut_orders
    ADD COLUMN IF NOT EXISTS machine_id BIGINT REFERENCES machines (id);
ALTER TABLE cut_orders
    ADD COLUMN IF NOT EXISTS purpose VARCHAR(40) DEFAULT 'AFTER_PROCESSING_SALE';
ALTER TABLE cut_orders
    ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES customers (id);

CREATE TABLE construction_site_stone_plans
(
    id               BIGSERIAL PRIMARY KEY,
    project_id       BIGINT         NOT NULL REFERENCES projects (id),
    location_id      BIGINT         NOT NULL REFERENCES project_locations (id),
    stone_type       VARCHAR(120)   NOT NULL,
    surface_finish   VARCHAR(30),
    width_cm         DECIMAL(10, 2),
    length_cm        DECIMAL(10, 2),
    planned_area_m2  DECIMAL(12, 4) NOT NULL,
    scrap_percent    DECIMAL(6, 2)  NOT NULL DEFAULT 0,
    required_area_m2 DECIMAL(12, 4) NOT NULL,
    supply_route     VARCHAR(40)    NOT NULL DEFAULT 'INTERNAL_PRODUCTION',
    notes            TEXT,
    created_date     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id      VARCHAR(128),
    update_user_id   VARCHAR(128)
);

CREATE INDEX idx_site_stone_plans_project ON construction_site_stone_plans (project_id);

CREATE TABLE site_supply_allocations
(
    id                     BIGSERIAL PRIMARY KEY,
    stone_plan_id          BIGINT         NOT NULL REFERENCES construction_site_stone_plans (id),
    factory_work_order_id  BIGINT REFERENCES factory_work_orders (id),
    cut_order_id           BIGINT REFERENCES cut_orders (id),
    purchase_order_item_id BIGINT REFERENCES purchase_order_items (id),
    stock_reservation_id   BIGINT REFERENCES stock_reservations (id),
    allocated_area_m2      DECIMAL(12, 4) NOT NULL DEFAULT 0,
    notes                  TEXT,
    created_date           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date           TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id            VARCHAR(128),
    update_user_id         VARCHAR(128)
);

CREATE TABLE site_installations
(
    id                BIGSERIAL PRIMARY KEY,
    project_id        BIGINT         NOT NULL REFERENCES projects (id),
    location_id       BIGINT         NOT NULL REFERENCES project_locations (id),
    material_lot_id   BIGINT REFERENCES material_lots (id),
    pallet_id         BIGINT REFERENCES pallets (id),
    installed_area_m2 DECIMAL(12, 4) NOT NULL,
    waste_area_m2     DECIMAL(12, 4) NOT NULL DEFAULT 0,
    installed_on      DATE           NOT NULL,
    crew_name         VARCHAR(120),
    notes             TEXT,
    created_date      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id       VARCHAR(128),
    update_user_id    VARCHAR(128)
);

CREATE INDEX idx_site_installations_project ON site_installations (project_id, installed_on);

CREATE TABLE cost_period_closes
(
    id              BIGSERIAL PRIMARY KEY,
    business_unit   VARCHAR(30) NOT NULL,
    expense_period  VARCHAR(7)  NOT NULL,
    closed_at       TIMESTAMP   NOT NULL,
    closed_by       VARCHAR(128),
    notes           TEXT,
    created_date    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id     VARCHAR(128),
    update_user_id  VARCHAR(128),
    CONSTRAINT uq_cost_period_closes UNIQUE (business_unit, expense_period)
);

ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS business_unit VARCHAR(30);
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS expense_category VARCHAR(40);
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS currency VARCHAR(3) DEFAULT 'TRY';
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS document_no VARCHAR(60);
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS invoice_date DATE;
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS entry_date DATE;
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS expense_period VARCHAR(7);
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS posting_period VARCHAR(7);
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS machine_id BIGINT REFERENCES machines (id);
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS factory_operation_id BIGINT REFERENCES factory_operations (id);
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS workshop_operation_id BIGINT REFERENCES workshop_operations (id);
ALTER TABLE cost_transactions
    ADD COLUMN IF NOT EXISTS construction_site_id BIGINT REFERENCES projects (id);

ALTER TABLE cost_centers
    ADD COLUMN IF NOT EXISTS business_unit VARCHAR(30);

ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES customers (id);

ALTER TABLE pallets
    ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES customers (id);
ALTER TABLE pallets
    ADD COLUMN IF NOT EXISTS project_id BIGINT REFERENCES projects (id);

ALTER TABLE shipments
    ADD COLUMN IF NOT EXISTS customer_id BIGINT REFERENCES customers (id);

UPDATE cost_centers
SET name          = CASE code
                        WHEN 'CC-001' THEN 'Ocak'
                        WHEN 'CC-002' THEN 'Fabrika Kesim'
                        WHEN 'CC-003' THEN 'Fabrika Silim'
                        WHEN 'CC-004' THEN 'Atölye'
                        WHEN 'CC-005' THEN 'Şantiyeler'
                        WHEN 'CC-006' THEN 'Nakliye'
                        ELSE name
    END,
    business_unit = CASE code
                        WHEN 'CC-001' THEN 'QUARRY'
                        WHEN 'CC-002' THEN 'FACTORY'
                        WHEN 'CC-003' THEN 'FACTORY'
                        WHEN 'CC-004' THEN 'WORKSHOP'
                        WHEN 'CC-005' THEN 'SITE'
                        WHEN 'CC-006' THEN 'FACTORY'
                        ELSE business_unit
        END;

UPDATE cost_transactions ct
SET expense_period    = to_char(COALESCE(ct.created_date, ct.created_at, CURRENT_TIMESTAMP), 'YYYY-MM'),
    posting_period    = to_char(COALESCE(ct.created_date, ct.created_at, CURRENT_TIMESTAMP), 'YYYY-MM'),
    entry_date        = CAST(COALESCE(ct.created_date, ct.created_at, CURRENT_TIMESTAMP) AS date),
    invoice_date      = CAST(COALESCE(ct.created_date, ct.created_at, CURRENT_TIMESTAMP) AS date),
    currency          = COALESCE(ct.currency, 'TRY'),
    business_unit     = COALESCE(ct.business_unit, cc.business_unit, 'FACTORY'),
    expense_category  = COALESCE(ct.expense_category,
                                 CASE ct.expense_type
                                     WHEN 'ELECTRICITY' THEN 'ELECTRICITY'
                                     WHEN 'DIRECT_LABOR' THEN 'LABOR'
                                     WHEN 'DIESEL' THEN 'DIESEL'
                                     WHEN 'LOGISTICS' THEN 'TRANSPORTATION'
                                     WHEN 'TRANSPORTATION' THEN 'TRANSPORTATION'
                                     WHEN 'TAX' THEN 'TAX'
                                     WHEN 'DIRECT_RAW' THEN 'MATERIAL'
                                     WHEN 'MATERIAL' THEN 'MATERIAL'
                                     WHEN 'CONSUMABLES' THEN 'FIXTURE_CONSUMABLE'
                                     WHEN 'FIXTURE_CONSUMABLE' THEN 'FIXTURE_CONSUMABLE'
                                     ELSE 'OTHER'
                                     END),
    construction_site_id = COALESCE(ct.construction_site_id, ct.project_id)
FROM cost_centers cc
WHERE cc.id = ct.center_id
  AND (ct.expense_period IS NULL OR ct.business_unit IS NULL);

INSERT INTO data_migration_warnings (source_table, source_id, field_name, original_value, message)
SELECT 'purchase_order_items', id, 'item_type', item_type,
       'Bilinmeyen satın alma kalem tipi; CONSUMABLE olarak bırakılmadı, raporlandı.'
FROM purchase_order_items
WHERE item_type NOT IN
      ('CONSUMABLE', 'ADHESIVE', 'GROUT', 'CHEMICAL', 'STONE', 'EQUIPMENT', 'SAND', 'CEMENT');

UPDATE pallets
SET status = CASE status
                 WHEN 'OPEN' THEN 'PREPARING'
                 WHEN 'PACKED' THEN 'READY'
                 ELSE status
    END
WHERE status IN ('OPEN', 'PACKED');

INSERT INTO factory_work_orders (order_no, block_id, accepted_at, stock_location_id, status, responsible_name, notes,
                                 add_user_id, update_user_id)
SELECT 'FWO-' || po.order_no,
       po.block_id,
       CAST(po.start_time AS date),
       (SELECT id FROM stock_locations WHERE code = 'FAB-BLOK'),
       CASE WHEN po.status = 'COMPLETED' THEN 'COMPLETED' ELSE 'IN_PROGRESS' END,
       po.operator_name,
       'Mevcut üretim emrinden aktarıldı',
       'system@ozerler.com',
       'system@ozerler.com'
FROM production_orders po;

INSERT INTO factory_operations (work_order_id, process_type, operator_name, started_at, finished_at, input_quantity,
                                input_unit, output_quantity, output_unit, waste_quantity, waste_unit, status, notes,
                                add_user_id, update_user_id)
SELECT fwo.id,
       CASE po.process_type
           WHEN 'ST' THEN 'ST_CUTTING'
           WHEN 'POLISHING' THEN 'SLAB_POLISHING'
           WHEN 'STRIP_POLISHING' THEN 'STRIP_POLISHING'
           WHEN 'BRIDGE_CUTTING' THEN 'BRIDGE_SAW_SIZING'
           WHEN 'PALLETIZING' THEN 'PALLETIZING'
           ELSE 'GANGSAW_CUTTING'
           END,
       po.operator_name,
       po.start_time,
       po.end_time,
       0, 'TON', 0, 'SQUARE_METER', 0, 'KG',
       CASE po.status WHEN 'COMPLETED' THEN 'COMPLETED' ELSE 'IN_PROGRESS' END,
       'Mevcut üretim emrinden aktarıldı: ' || po.order_no,
       'system@ozerler.com',
       'system@ozerler.com'
FROM production_orders po
         JOIN factory_work_orders fwo ON fwo.order_no = 'FWO-' || po.order_no;

UPDATE production_orders po
SET factory_work_order_id = fwo.id
FROM factory_work_orders fwo
WHERE fwo.order_no = 'FWO-' || po.order_no;

UPDATE production_orders po
SET factory_operation_id = fo.id
FROM factory_operations fo
         JOIN factory_work_orders fwo ON fwo.id = fo.work_order_id
WHERE po.factory_work_order_id = fwo.id;

INSERT INTO material_lots (lot_code, product_form, source_block_id, slab_id, stone_type, quality_grade, surface_finish,
                           thickness_cm, width_cm, length_cm, quantity, total_area_m2, stock_location_id, status,
                           unit_cost, total_cost, add_user_id, update_user_id)
SELECT s.slab_code,
       'SLAB',
       s.block_id,
       s.id,
       b.stone_type,
       s.quality_grade,
       s.surface_finish,
       s.thickness_cm,
       s.width_cm,
       s.length_cm,
       1,
       s.surface_area_m2,
       (SELECT id FROM stock_locations WHERE code = 'FAB-PLAKA'),
       CASE s.status
           WHEN 'RESERVED' THEN 'RESERVED'
           WHEN 'SCRAPPED' THEN 'SCRAPPED'
           WHEN 'INSTALLED' THEN 'INSTALLED'
           ELSE 'AVAILABLE'
           END,
       s.cost_per_m2,
       COALESCE(s.cost_per_m2, 0) * COALESCE(s.surface_area_m2, 0),
       'system@ozerler.com',
       'system@ozerler.com'
FROM slabs s
         LEFT JOIN blocks b ON b.id = s.block_id;

INSERT INTO material_lots (lot_code, product_form, source_block_id, cut_item_id, stone_type, thickness_cm, width_cm,
                           length_cm, quantity, total_area_m2, stock_location_id, status, unit_cost, total_cost,
                           add_user_id, update_user_id)
SELECT ci.item_code,
       'SIZED_PRODUCT',
       sl.block_id,
       ci.id,
       b.stone_type,
       ci.thickness_cm,
       ci.width_cm,
       ci.length_cm,
       1,
       ci.area_m2,
       (SELECT id FROM stock_locations WHERE code = 'ATL-STOK'),
       CASE ci.status
           WHEN 'DELIVERED' THEN 'SHIPPED'
           WHEN 'INSTALLED' THEN 'INSTALLED'
           WHEN 'PACKED' THEN 'PALLETIZED'
           ELSE 'AVAILABLE'
           END,
       ci.unit_cost,
       COALESCE(ci.unit_cost, 0) * COALESCE(ci.area_m2, 0),
       'system@ozerler.com',
       'system@ozerler.com'
FROM cut_items ci
         LEFT JOIN slabs sl ON sl.id = ci.source_slab_id
         LEFT JOIN blocks b ON b.id = sl.block_id;

CREATE UNIQUE INDEX uq_stock_reservations_active_slab
    ON stock_reservations (slab_id)
    WHERE status = 'ACTIVE' AND slab_id IS NOT NULL;

CREATE INDEX idx_blocks_current_location ON blocks (current_location_id);
CREATE INDEX idx_cost_transactions_period ON cost_transactions (business_unit, expense_period);
CREATE INDEX idx_cost_transactions_category ON cost_transactions (expense_category);
CREATE INDEX idx_projects_customer ON projects (customer_id);

INSERT INTO system_settings (setting_key, setting_value, category, description)
SELECT 'quarry.tonnage_basis', 'ACTUAL_SCALE', 'GENERAL',
       'Ocak maliyetinde kullanılacak tonaj: ACTUAL_SCALE (kantar) yoksa APPROXIMATE'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'quarry.tonnage_basis');
