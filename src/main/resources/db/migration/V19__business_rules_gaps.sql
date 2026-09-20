-- P1-3 quarry section, P0-2 operation costs, P1-6 pallet stock location

ALTER TABLE blocks
    ADD COLUMN IF NOT EXISTS quarry_section VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_blocks_quarry_section ON blocks (quarry_section);

ALTER TABLE factory_operations
    ADD COLUMN IF NOT EXISTS labor_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS electricity_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS consumable_cost NUMERIC(14, 2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_operation_cost NUMERIC(14, 2) NOT NULL DEFAULT 0;

ALTER TABLE pallets
    ADD COLUMN IF NOT EXISTS current_location_id BIGINT;

ALTER TABLE pallets
    DROP CONSTRAINT IF EXISTS fk_pallets_current_location;

ALTER TABLE pallets
    ADD CONSTRAINT fk_pallets_current_location
        FOREIGN KEY (current_location_id) REFERENCES stock_locations (id);

UPDATE pallets
SET current_location_id = (
    SELECT sl.id
    FROM stock_locations sl
    WHERE sl.location_type = 'PALLET_STOCK_YARD'
      AND sl.active = TRUE
    ORDER BY sl.id
    LIMIT 1
)
WHERE current_location_id IS NULL;

CREATE TABLE IF NOT EXISTS pallet_location_movements (
    id                BIGSERIAL PRIMARY KEY,
    pallet_id         BIGINT       NOT NULL REFERENCES pallets (id),
    from_location_id  BIGINT REFERENCES stock_locations (id),
    to_location_id    BIGINT       NOT NULL REFERENCES stock_locations (id),
    description       VARCHAR(255),
    created_date      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id       VARCHAR(128),
    update_user_id    VARCHAR(128)
);

CREATE INDEX IF NOT EXISTS idx_pallet_movements_pallet ON pallet_location_movements (pallet_id);
