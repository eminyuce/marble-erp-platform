-- V32: Business Unit Separation for Checks and Collections (4 Company Model: Factory, Quarry, Workshop, Site)

ALTER TABLE checks ADD COLUMN IF NOT EXISTS business_unit VARCHAR(30);
CREATE INDEX IF NOT EXISTS idx_checks_business_unit ON checks (business_unit);

UPDATE checks c
SET business_unit = cu.business_unit
FROM customers cu
WHERE c.customer_id = cu.id AND c.business_unit IS NULL;

UPDATE checks
SET business_unit = 'FACTORY'
WHERE business_unit IS NULL;

ALTER TABLE collections ADD COLUMN IF NOT EXISTS business_unit VARCHAR(30);
CREATE INDEX IF NOT EXISTS idx_collections_business_unit ON collections (business_unit);

UPDATE collections c
SET business_unit = cu.business_unit
FROM customers cu
WHERE c.customer_id = cu.id AND c.business_unit IS NULL;

UPDATE collections
SET business_unit = 'FACTORY'
WHERE business_unit IS NULL;
