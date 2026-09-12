-- =====================================================
-- Performance Indexes for Özerler Mermer ERP
-- =====================================================

-- Table: blocks
CREATE INDEX idx_blocks_status_created_at ON blocks (status, created_at);
CREATE INDEX idx_blocks_quarry_id ON blocks (quarry_id);
CREATE INDEX idx_blocks_extraction_date ON blocks (extraction_date);

-- Table: production_orders
CREATE INDEX idx_production_orders_block_id_status ON production_orders (block_id, status);
CREATE INDEX idx_production_orders_status_created_at ON production_orders (status, created_at);
CREATE INDEX idx_production_orders_start_time ON production_orders (start_time);

-- Table: slabs
CREATE INDEX idx_slabs_block_id_status ON slabs (block_id, status);
CREATE INDEX idx_slabs_order_id_status ON slabs (order_id, status);
CREATE INDEX idx_slabs_status_surface_area ON slabs (status, surface_area_m2);
CREATE INDEX idx_slabs_status_created_at ON slabs (status, created_at);
CREATE INDEX idx_slabs_pallet_id ON slabs (pallet_id);

-- Table: pallets
CREATE UNIQUE INDEX uq_pallets_qr_code_hash ON pallets (qr_code_hash);
CREATE INDEX idx_pallets_status ON pallets (status);

-- Table: cut_orders
CREATE INDEX idx_cut_orders_project_id_status ON cut_orders (project_id, status);
CREATE INDEX idx_cut_orders_status_created_at ON cut_orders (status, created_at);
CREATE INDEX idx_cut_orders_location_id ON cut_orders (location_id);

-- Table: cut_items
CREATE INDEX idx_cut_items_cut_order_id ON cut_items (cut_order_id);
CREATE INDEX idx_cut_items_source_slab_id ON cut_items (source_slab_id);
CREATE INDEX idx_cut_items_status_created_at ON cut_items (status, created_at);

-- Table: scrap_logs
CREATE INDEX idx_scrap_logs_logged_at ON scrap_logs (logged_at);
CREATE INDEX idx_scrap_logs_reason_code ON scrap_logs (reason_code);
CREATE INDEX idx_scrap_logs_block_id ON scrap_logs (block_id);
CREATE INDEX idx_scrap_logs_order_id ON scrap_logs (order_id);
CREATE INDEX idx_scrap_logs_cut_order_id ON scrap_logs (cut_order_id);

-- Table: projects
CREATE INDEX idx_projects_status_created_at ON projects (status, created_at);
CREATE INDEX idx_projects_name ON projects (name);
CREATE INDEX idx_projects_delivery_date ON projects (delivery_date);

-- Table: project_locations
CREATE INDEX idx_project_locations_project_id ON project_locations (project_id);
CREATE INDEX idx_project_locations_project_parent ON project_locations (project_id, parent_id);

-- Table: site_consumptions
CREATE INDEX idx_site_consumptions_project_total_cost ON site_consumptions (project_id, total_cost);
CREATE INDEX idx_site_consumptions_location_id ON site_consumptions (location_id);
CREATE INDEX idx_site_consumptions_type ON site_consumptions (consumption_type);

-- Table: cost_transactions
CREATE INDEX idx_cost_transactions_center_amount ON cost_transactions (center_id, amount);
CREATE INDEX idx_cost_transactions_type_amount ON cost_transactions (expense_type, amount);
CREATE INDEX idx_cost_transactions_project_id ON cost_transactions (project_id);
CREATE INDEX idx_cost_transactions_block_id ON cost_transactions (block_id);

-- Table: stock_reservations
CREATE INDEX idx_stock_reservations_project_id_status ON stock_reservations (project_id, status);
CREATE INDEX idx_stock_reservations_slab_id ON stock_reservations (slab_id);
CREATE INDEX idx_stock_reservations_status ON stock_reservations (status);

-- Table: shipments
CREATE INDEX idx_shipments_departure_time ON shipments (departure_time);
CREATE INDEX idx_shipments_project_delivery_status ON shipments (project_id, delivery_status);

-- Table: customers
CREATE INDEX idx_customers_company_name ON customers (company_name);

-- Table: sales_orders
CREATE INDEX idx_sales_orders_status_created_at ON sales_orders (status, created_at);
CREATE INDEX idx_sales_orders_customer_id ON sales_orders (customer_id);
CREATE INDEX idx_sales_orders_order_date ON sales_orders (order_date);

-- Table: sales_order_items
CREATE INDEX idx_sales_order_items_sales_order_id ON sales_order_items (sales_order_id);
CREATE INDEX idx_sales_order_items_slab_id ON sales_order_items (slab_id);

-- Table: suppliers
CREATE INDEX idx_suppliers_company_name ON suppliers (company_name);

-- Table: purchase_orders
CREATE INDEX idx_purchase_orders_status_created_at ON purchase_orders (status, created_at);
CREATE INDEX idx_purchase_orders_supplier_id ON purchase_orders (supplier_id);
CREATE INDEX idx_purchase_orders_project_id ON purchase_orders (project_id);

-- Table: purchase_order_items
CREATE INDEX idx_purchase_order_items_po_id ON purchase_order_items (purchase_order_id);

-- Table: users
CREATE INDEX idx_users_deleted_enabled_created ON users (deleted, enabled, created_at);

-- Table: system_settings
CREATE INDEX idx_system_settings_category ON system_settings (category);

-- Table: quarries
CREATE INDEX idx_quarries_name ON quarries (name);

-- Table: cost_centers
CREATE INDEX idx_cost_centers_name ON cost_centers (name);
