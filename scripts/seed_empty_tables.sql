-- Özerler Mermer ERP - Seed Dummy Data for Empty Tables
-- Populates:
-- 1. stock_reservations
-- 2. block_customer_marks
-- 3. block_location_movements
-- 4. machine_fuel_entries
-- 5. pallet_items
-- 6. shipment_items
-- 7. workshop_operations
-- 8. workshop_material_receipts
-- 9. construction_site_stone_plans
-- 10. site_supply_allocations
-- 11. site_installations
-- 12. cost_period_closes
-- 13. data_migration_warnings

BEGIN;

-- 1. stock_reservations
INSERT INTO stock_reservations (slab_id, block_id, project_id, reserved_area_m2, reserved_until, status, created_at)
VALUES 
    (1, 1, 1, 4.25, CURRENT_TIMESTAMP + INTERVAL '30 days', 'ACTIVE', CURRENT_TIMESTAMP),
    (4, 2, 2, 3.80, CURRENT_TIMESTAMP + INTERVAL '15 days', 'ACTIVE', CURRENT_TIMESTAMP),
    (NULL, 3, 1, 15.00, CURRENT_TIMESTAMP + INTERVAL '20 days', 'ACTIVE', CURRENT_TIMESTAMP);

-- 2. block_customer_marks
INSERT INTO block_customer_marks (block_id, customer_id, marked_at, valid_until, offer_price, currency, status, stock_location_id, add_user_id, update_user_id)
VALUES 
    (3, 1, CURRENT_DATE - 5, CURRENT_DATE + 25, 145000.00, 'TRY', 'ACTIVE', 1, 'admin@eimece.test', 'admin@eimece.test'),
    (4, 2, CURRENT_DATE - 10, CURRENT_DATE + 10, 185000.00, 'TRY', 'ACTIVE', 3, 'admin@eimece.test', 'admin@eimece.test'),
    (6, 3, CURRENT_DATE - 2, CURRENT_DATE + 28, 210000.00, 'TRY', 'ACTIVE', 3, 'admin@eimece.test', 'admin@eimece.test');

-- 3. block_location_movements
INSERT INTO block_location_movements (block_id, from_location_id, to_location_id, description, add_user_id, update_user_id)
VALUES 
    (1, 1, 2, 'Ocak üretim sahasından sevk rampasına nakil', 'admin@eimece.test', 'admin@eimece.test'),
    (1, 2, 3, 'Fabrika blok sahasına varış ve stok kabulü', 'admin@eimece.test', 'admin@eimece.test'),
    (2, 1, 3, 'Doğrudan fabrika blok stoğuna sevk', 'admin@eimece.test', 'admin@eimece.test'),
    (4, 1, 3, 'Fabrika blok sahasına kabul ve yerleşim', 'admin@eimece.test', 'admin@eimece.test');

-- 4. machine_fuel_entries
INSERT INTO machine_fuel_entries (machine_id, entry_date, litres, price_per_litre, total_amount, receipt_no, issued_by, received_by, notes, add_user_id, update_user_id)
VALUES 
    (1, CURRENT_DATE - 7, 350.000, 44.5000, 15575.00, 'YKT-2026-001', 'Petrol Ofisi', 'Ahmet Usta', 'Ekskavatör haftalık yakıt ikmali', 'admin@eimece.test', 'admin@eimece.test'),
    (2, CURRENT_DATE - 6, 280.000, 44.5000, 12460.00, 'YKT-2026-002', 'Petrol Ofisi', 'Mehmet Kaya', 'Loader yakıt ikmali', 'admin@eimece.test', 'admin@eimece.test'),
    (3, CURRENT_DATE - 3, 120.000, 44.7500, 5370.00, 'YKT-2026-003', 'OPET', 'Ali Demir', 'Jeneratör ve fabrika destek ikmali', 'admin@eimece.test', 'admin@eimece.test');

-- 5. pallet_items
INSERT INTO pallet_items (pallet_id, material_lot_id, quantity, area_m2, add_user_id, update_user_id)
VALUES 
    (1, 1, 10, 14.5000, 'admin@eimece.test', 'admin@eimece.test'),
    (1, 2, 8, 11.2000, 'admin@eimece.test', 'admin@eimece.test'),
    (2, 3, 15, 21.0000, 'admin@eimece.test', 'admin@eimece.test'),
    (3, 4, 12, 16.8000, 'admin@eimece.test', 'admin@eimece.test'),
    (5, 5, 14, 18.2000, 'admin@eimece.test', 'admin@eimece.test');

-- 6. shipment_items
INSERT INTO shipment_items (shipment_id, pallet_id, material_lot_id, block_id, quantity, area_m2, add_user_id, update_user_id)
VALUES 
    (1, 1, NULL, NULL, 1, 25.7000, 'admin@eimece.test', 'admin@eimece.test'),
    (1, 2, NULL, NULL, 1, 21.0000, 'admin@eimece.test', 'admin@eimece.test'),
    (2, 3, NULL, NULL, 1, 16.8000, 'admin@eimece.test', 'admin@eimece.test'),
    (3, NULL, NULL, 1, 1, 0.0000, 'admin@eimece.test', 'admin@eimece.test');

-- 7. workshop_operations
INSERT INTO workshop_operations (cut_order_id, process_type, machine_id, operator_name, labor_hours, started_at, finished_at, input_area_m2, output_area_m2, waste_area_m2, area_unit, extra_expense, status, notes, add_user_id, update_user_id)
VALUES 
    (1, 'BRIDGE_SAW_SIZING', 8, 'Hasan Usta', 4.50, CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '4 hours', 18.5000, 16.2000, 2.3000, 'SQUARE_METER', 450.00, 'COMPLETED', 'Lobi zemin ebatlama tamamlandı', 'admin@eimece.test', 'admin@eimece.test'),
    (1, 'MACHINE_CHAMFERING', 11, 'Hasan Usta', 2.00, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours', 16.2000, 16.0000, 0.2000, 'SQUARE_METER', 200.00, 'COMPLETED', 'Basamak pah açımı', 'admin@eimece.test', 'admin@eimece.test'),
    (2, 'EDGE_CUTTING', 10, 'Kemal Yılmaz', 3.00, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day' + INTERVAL '3 hours', 12.0000, 11.1000, 0.9000, 'SQUARE_METER', 150.00, 'COMPLETED', 'Koridor bordür kesimi', 'admin@eimece.test', 'admin@eimece.test');

-- 8. workshop_material_receipts
INSERT INTO workshop_material_receipts (receipt_no, source, supplier_id, purchase_order_item_id, material_lot_id, quantity, area_m2, purchase_cost, received_at, notes, add_user_id, update_user_id)
VALUES 
    ('WMR-2026-001', 'INTERNAL_FACTORY', NULL, NULL, 1, 5.0000, 14.2500, 8500.00, CURRENT_DATE - 4, 'Fabrika plaka deposundan atölyeye teslim', 'admin@eimece.test', 'admin@eimece.test'),
    ('WMR-2026-002', 'EXTERNAL_FACTORY', 1, 1, NULL, 8.0000, 22.8000, 14200.00, CURRENT_DATE - 2, 'Tedarikçiden fason plaka kabulü', 'admin@eimece.test', 'admin@eimece.test');

-- 9. construction_site_stone_plans
INSERT INTO construction_site_stone_plans (project_id, location_id, stone_type, surface_finish, width_cm, length_cm, planned_area_m2, scrap_percent, required_area_m2, supply_route, notes, add_user_id, update_user_id)
VALUES 
    (1, 1, 'Mugla Beyazi', 'POLISHED', 60.00, 60.00, 250.0000, 7.00, 267.5000, 'INTERNAL_PRODUCTION', 'Lobi zemin kaplama döşeme planı', 'admin@eimece.test', 'admin@eimece.test'),
    (1, 2, 'Afyon Sekeri', 'HONED', 30.00, 60.00, 420.0000, 8.00, 453.6000, 'INTERNAL_PRODUCTION', 'Koridor zemin kaplama', 'admin@eimece.test', 'admin@eimece.test'),
    (2, 4, 'Mugla Beyazi', 'BRUSHED', 80.00, 80.00, 180.0000, 6.00, 190.8000, 'MIXED', 'Marina rıhtım dış mekan kaplama', 'admin@eimece.test', 'admin@eimece.test');

-- 10. site_supply_allocations
INSERT INTO site_supply_allocations (stone_plan_id, factory_work_order_id, cut_order_id, purchase_order_item_id, stock_reservation_id, allocated_area_m2, notes, add_user_id, update_user_id)
VALUES 
    ((SELECT id FROM construction_site_stone_plans WHERE project_id = 1 AND location_id = 1 LIMIT 1), 1, 1, NULL, 1, 120.0000, 'Fabrika 1. parti kesim tahsisi', 'admin@eimece.test', 'admin@eimece.test'),
    ((SELECT id FROM construction_site_stone_plans WHERE project_id = 1 AND location_id = 2 LIMIT 1), 2, 2, NULL, 2, 200.0000, 'Fabrika 2. parti katrak tahsisi', 'admin@eimece.test', 'admin@eimece.test');

-- 11. site_installations
INSERT INTO site_installations (project_id, location_id, material_lot_id, pallet_id, installed_area_m2, waste_area_m2, installed_on, crew_name, notes, add_user_id, update_user_id)
VALUES 
    (1, 1, 1, 1, 45.0000, 2.5000, CURRENT_DATE - 3, 'Anadolu Montaj Ekibi 1', 'Lobi ana giriş aksı tamamlandı', 'admin@eimece.test', 'admin@eimece.test'),
    (1, 1, 2, 2, 55.0000, 3.2000, CURRENT_DATE - 1, 'Anadolu Montaj Ekibi 1', 'Resepsiyon önü montajı', 'admin@eimece.test', 'admin@eimece.test'),
    (2, 4, 3, 3, 38.0000, 1.8000, CURRENT_DATE - 2, 'Marina Cephe Ekibi', 'Rıhtım basamak montajı', 'admin@eimece.test', 'admin@eimece.test');

-- 12. cost_period_closes
INSERT INTO cost_period_closes (business_unit, expense_period, closed_at, closed_by, notes, add_user_id, update_user_id)
VALUES 
    ('QUARRY', '2026-07', '2026-08-01 10:00:00', 'admin@eimece.test', 'Temmuz 2026 ocak maliyet dönemi kapandı', 'admin@eimece.test', 'admin@eimece.test'),
    ('FACTORY', '2026-07', '2026-08-01 11:30:00', 'admin@eimece.test', 'Temmuz 2026 fabrika maliyet dönemi kapandı', 'admin@eimece.test', 'admin@eimece.test'),
    ('WORKSHOP', '2026-07', '2026-08-01 14:00:00', 'admin@eimece.test', 'Temmuz 2026 atölye maliyet dönemi kapandı', 'admin@eimece.test', 'admin@eimece.test'),
    ('SITE', '2026-07', '2026-08-01 15:00:00', 'admin@eimece.test', 'Temmuz 2026 şantiye maliyet dönemi kapandı', 'admin@eimece.test', 'admin@eimece.test'),
    ('QUARRY', '2026-08', '2026-09-01 09:30:00', 'admin@eimece.test', 'Ağustos 2026 ocak maliyet dönemi kapandı', 'admin@eimece.test', 'admin@eimece.test'),
    ('FACTORY', '2026-08', '2026-09-01 10:45:00', 'admin@eimece.test', 'Ağustos 2026 fabrika maliyet dönemi kapandı', 'admin@eimece.test', 'admin@eimece.test');

-- 13. data_migration_warnings
INSERT INTO data_migration_warnings (source_table, source_id, field_name, original_value, message, created_date)
VALUES 
    ('purchase_order_items', 1, 'item_type', 'OTHER', 'Geçmiş sipariş kalemi tipi otomatik olarak standarda uyarlandı.', CURRENT_TIMESTAMP - INTERVAL '5 days'),
    ('blocks', 3, 'status', 'QUARRY', 'Ocak statüsü V11 operasyonel modeline göre PRODUCED olarak güncellendi.', CURRENT_TIMESTAMP - INTERVAL '5 days');

COMMIT;
