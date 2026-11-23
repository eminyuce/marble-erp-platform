-- D3: line quantities and site areas used a shorter scale than invoice lines.
-- Widen only. Existing values fit in DECIMAL(14, 4). No rows are deleted or rewritten.

ALTER TABLE sales_order_items
    ALTER COLUMN quantity TYPE DECIMAL(14, 4);

ALTER TABLE purchase_order_items
    ALTER COLUMN quantity TYPE DECIMAL(14, 4);

ALTER TABLE project_locations
    ALTER COLUMN planned_area_m2 TYPE DECIMAL(14, 4),
    ALTER COLUMN installed_area_m2 TYPE DECIMAL(14, 4);
