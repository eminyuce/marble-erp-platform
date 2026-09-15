-- Rename quarry dispatch yard to Stok Sahası, assign sold-block customers,
-- and require purchase orders to belong to a business unit.
-- V1–V11 remain unchanged.

UPDATE stock_locations
SET name = 'Stok Sahası'
WHERE code = 'OCAK-SEVK';

ALTER TABLE blocks
    ADD COLUMN IF NOT EXISTS sold_customer_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_blocks_sold_customer'
    ) THEN
        ALTER TABLE blocks
            ADD CONSTRAINT fk_blocks_sold_customer
                FOREIGN KEY (sold_customer_id) REFERENCES customers (id);
    END IF;
END
$$;

ALTER TABLE purchase_orders
    ADD COLUMN IF NOT EXISTS business_unit VARCHAR(30);

UPDATE purchase_orders
SET business_unit = CASE
                        WHEN project_id IS NOT NULL THEN 'SITE'
                        ELSE 'FACTORY'
                    END
WHERE business_unit IS NULL;
