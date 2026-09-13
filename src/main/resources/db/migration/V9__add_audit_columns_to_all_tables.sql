-- ==============================================================================
-- V9: Standardized Audit Columns Across All Tables
-- Columns:
--   created_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--   updated_date   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
--   add_user_id    VARCHAR(128) NULL
--   update_user_id VARCHAR(128) NULL
-- ==============================================================================

DO
$$
DECLARE
tbl text;
    tables
text[] := ARRAY[
        'blocks',
        'cost_centers',
        'cost_transactions',
        'customers',
        'cut_items',
        'cut_orders',
        'email_templates',
        'pallets',
        'production_orders',
        'project_locations',
        'projects',
        'purchase_order_items',
        'purchase_orders',
        'quarries',
        'roles',
        'sales_order_items',
        'sales_orders',
        'scrap_logs',
        'shipments',
        'site_consumptions',
        'slabs',
        'stock_reservations',
        'suppliers',
        'system_settings',
        'users',
        'user_roles'
    ];
BEGIN
    FOREACH
tbl IN ARRAY tables LOOP
        -- 1. Add columns if not existing
        EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP', tbl);
EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS updated_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP',
               tbl);
EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS add_user_id VARCHAR(128)', tbl);
EXECUTE format('ALTER TABLE %I ADD COLUMN IF NOT EXISTS update_user_id VARCHAR(128)', tbl);

-- 2. Backfill created_date and updated_date from existing created_at / updated_at if present
BEGIN
EXECUTE format(
        'UPDATE %I SET created_date = COALESCE(created_at, CURRENT_TIMESTAMP), updated_date = COALESCE(created_at, CURRENT_TIMESTAMP) WHERE created_date IS NULL OR created_date = CURRENT_TIMESTAMP',
        tbl);
EXCEPTION WHEN undefined_column THEN
            -- Table does not have created_at, default is already CURRENT_TIMESTAMP
            NULL;
END;

BEGIN
EXECUTE format('UPDATE %I SET updated_date = updated_at WHERE updated_at IS NOT NULL', tbl);
EXCEPTION WHEN undefined_column THEN
            -- Table does not have updated_at
            NULL;
END;

        -- 3. Set default user for legacy records if null
EXECUTE format('UPDATE %I SET add_user_id = ''system@ozerler.com'' WHERE add_user_id IS NULL', tbl);
EXECUTE format('UPDATE %I SET update_user_id = ''system@ozerler.com'' WHERE update_user_id IS NULL', tbl);
END LOOP;
END $$;
