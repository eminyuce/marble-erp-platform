-- Seed Roles
INSERT INTO roles (id, name, description)
VALUES (1, 'ROLE_ADMIN', 'Sistem Yöneticisi - Tam Yetki'),
       (2, 'ROLE_USER', 'Standart Kullanıcı'),
       (3, 'ROLE_QUARRY_CHIEF', 'Ocak Şefi / Formeni'),
       (4, 'ROLE_FACTORY_MANAGER', 'Fabrika Müdürü'),
       (5, 'ROLE_OPERATOR', 'Makine Operatörü'),
       (6, 'ROLE_WORKSHOP_CHIEF', 'Atölye Şefi'),
       (7, 'ROLE_SITE_ENGINEER', 'Şantiye Şefi / Metraj Mühendisi'),
       (8, 'ROLE_FINANCE', 'Maliyet & Finans Uzmanı'),
       (9, 'ROLE_SALES', 'Satış & İhracat Sorumlusu'),
       (10, 'ROLE_QC', 'Kalite Kontrol Uzmanı'),
       (11, 'ROLE_EXECUTIVE', 'Genel Müdür / Şirket Ortağı');

-- Seed Default Admin & Sample Users (Password for all: 'changeit')
-- Hash: $2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, deleted, created_at)
VALUES (1, 'admin', 'admin@example.com', '$2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC', 'Sistem',
        'Yöneticisi', TRUE, FALSE, CURRENT_TIMESTAMP),
       (2, 'factory_mgr', 'fabrika@ozerler.com', '$2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC',
        'Ahmet', 'Kaya', TRUE, FALSE, CURRENT_TIMESTAMP),
       (3, 'site_chief', 'santiye@ozerler.com', '$2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC',
        'Mehmet', 'Demir', TRUE, FALSE, CURRENT_TIMESTAMP);

-- Assign User Roles
INSERT INTO user_roles (user_id, role_id)
VALUES (1, 1), -- admin -> ROLE_ADMIN
       (1, 2), -- admin -> ROLE_USER
       (1, 11),-- admin -> ROLE_EXECUTIVE
       (2, 4), -- factory_mgr -> ROLE_FACTORY_MANAGER
       (2, 2),
       (3, 7), -- site_chief -> ROLE_SITE_ENGINEER
       (3, 2);

-- Seed Cost Centers (Aktivite Tabanlı Maliyet Masraf Merkezleri)
INSERT INTO cost_centers (id, code, name, monthly_budget, description)
VALUES (1, 'CC-001', 'Ocak & Çıkarma', 500000.00, 'Blok çıkarma, ayna amortismanı ve ocak direkt işçilik masrafları'),
       (2, 'CC-002', 'Fabrika Katrak & Dilimleme', 450000.00, 'Katrak lamaları, elektrik, su ve kesim işçiliği'),
       (3, 'CC-003', 'Cila & Yüzey İşleme', 350000.00, 'Epoksi reçine, cila abrasiv taşları ve hat enerjisi'),
       (4, 'CC-004', 'Atölye & Ebatlama İmalatı', 300000.00, 'Köprü kesme diskleri, CNC ve ebatlama işçiliği'),
       (5, 'CC-005', 'Şantiye & Montaj', 600000.00, 'Montaj işçilik puantajı, yapıştırıcı ve derz sarfiyatı'),
       (6, 'CC-006', 'Lojistik & Nakliye', 250000.00, 'Ocak-fabrika ve şantiye sevkiyat nakliye akaryakıt giderleri');

-- Seed Quarries
INSERT INTO quarries (id, code, name, location, specific_gravity, license_no)
VALUES (1, 'Q-MUG-01', 'Yatağan Beyaz Ocağı', 'Muğla - Yatağan', 2.70, 'MUG-2024-884'),
       (2, 'Q-AFY-01', 'Afyon Şeker Ocağı', 'Afyonkarahisar - İscehisar', 2.72, 'AFY-2022-102'),
       (3, 'Q-BUR-01', 'Burdur Bej Ocağı', 'Burdur - Karamanlı', 2.68, 'BUR-2023-455');

-- Seed Sample Blocks
INSERT INTO blocks (id, quarry_id, block_code, extraction_date, width_cm, length_cm, height_cm, volume_m3,
                    theoretical_weight_kg, actual_weight_kg, weight_deviation_pct, stone_type, color_tone,
                    quality_grade, crack_level, status, extraction_cost, transport_cost, total_cost, notes)
VALUES (1, 1, 'BLK-2026-00125', '2026-09-01', 180, 290, 150, 7.830, 21141.00, 20850.00, -1.37, 'Muğla Beyaz',
        'Ekstra Beyaz Kristalize', 'A', 0, 'FACTORY_STOCK', 42000.00, 7200.00, 49200.00,
        'Homojen kristal yapıda, çatlaksız ayna bloğu.'),
       (2, 2, 'BLK-2026-00126', '2026-09-02', 170, 280, 140, 6.664, 18126.00, 18400.00, 1.51, 'Afyon Şeker',
        'Açık Krem Damarlı', 'A', 1, 'SAWING', 38000.00, 6500.00, 44500.00,
        'Kılcal yüzey çatlağı epoksi hattında telafi edilebilir.'),
       (3, 3, 'BLK-2026-00127', '2026-09-03', 190, 310, 160, 9.424, 25256.00, 25100.00, -0.62, 'Burdur Bej',
        'Homojen Açık Bej', 'EXTRA', 0, 'QUARRY', 52000.00, 0.00, 52000.00, 'Ocak sahasında sevk bekliyor.');

-- Seed Pallets
INSERT INTO pallets (id, pallet_code, warehouse_location, packaging_type, qr_code_hash, status, gross_weight_kg)
VALUES (1, 'PAL-2026-00145', 'Ambar-A / Sehpa-04', 'EXPORT_CRATE', 'HASH-PAL-2026-00145-QR', 'PACKED', 4850.00),
       (2, 'PAL-2026-00146', 'Ambar-B / Sehpa-11', 'A_FRAME', 'HASH-PAL-2026-00146-QR', 'OPEN', 2100.00);

-- Seed Production Order
INSERT INTO production_orders (id, order_no, block_id, machine_name, process_type, start_time, end_time, duration_hours,
                               electricity_kwh, blade_wear_mm, operator_name, status, notes)
VALUES (1, 'PRD-2026-00452', 1, 'Katrak-01 (80 Lamalı)', 'GANGSAW', '2026-09-03 08:00:00', '2026-09-03 16:30:00', 8.5,
        420.00, 1.20, 'Ahmet Kaya', 'COMPLETED', '48 plaka üretildi, kesim firesi FR_01 olarak kaydedildi.');

-- Seed Slabs (Derived from BLK-2026-00125 and PRD-2026-00452)
INSERT INTO slabs (id, slab_code, order_id, block_id, pallet_id, thickness_cm, width_cm, length_cm, surface_area_m2,
                   surface_finish, quality_grade, gloss_level, cost_per_m2, status)
VALUES (1, 'SLB-2026-00851', 1, 1, 1, 2.00, 175.00, 285.00, 4.9875, 'POLISHED', 'A', 88, 1185.57, 'RESERVED'),
       (2, 'SLB-2026-00852', 1, 1, 1, 2.00, 175.00, 285.00, 4.9875, 'POLISHED', 'B', 85, 1030.93, 'AVAILABLE'),
       (3, 'SLB-2026-00853', 1, 1, NULL, 2.00, 175.00, 285.00, 4.9875, 'RAW', 'C', 0, 670.10, 'AVAILABLE');

-- Seed Scrap Logs (10 Neden Kodlu Fire)
INSERT INTO scrap_logs (id, scrap_code, order_id, block_id, slab_id, reason_code, scrap_weight_kg, scrap_area_m2,
                        cost_impact, description, logged_by)
VALUES (1, 'SCRAP-2026-0001', 1, 1, NULL, 'FR_01', 2100.00, 0.0000, 4200.00,
        '80 lama kesim talaş kaybı tozu ve çamuru.', 'Ahmet Kaya'),
       (2, 'SCRAP-2026-0002', 1, 1, NULL, 'FR_02', 450.00, 2.4500, 1850.00,
        'Blok alt ayna gizli kılcal çatlağı nedeniyle 1 plaka kırımı.', 'Ahmet Kaya');

-- Seed Projects
INSERT INTO projects (id, project_code, name, customer_name, contract_value, estimated_cost, actual_cost, start_date,
                      delivery_date, status, notes)
VALUES (1, 'PROJ-2026-0042', 'X Residence Lüks Konut Projesi', 'ABC İnşaat Yatırım A.Ş.', 12400000.00, 7800000.00,
        8150000.00, '2026-08-01', '2027-04-30', 'ACTIVE',
        'Lobi duvar kaplama, koridorlar ve ıslak hacimler doğal taş imalatı.');

-- Seed Project Locations (WBS)
INSERT INTO project_locations (id, project_id, parent_id, location_name, floor_level, stone_spec, planned_area_m2,
                               installed_area_m2, status)
VALUES (1, 1, NULL, 'Zemin Kat Lobi & Karşılama', 'Zemin Kat', 'Muğla Beyaz 80x120 Cilalı', 1200.00, 850.00,
        'IN_PROGRESS'),
       (2, 1, NULL, '1-15 Tip Kat Koridorları', 'Kat 1-15', 'Burdur Bej 60x60 Honlu', 3500.00, 1200.00, 'IN_PROGRESS'),
       (3, 1, NULL, 'Islak Hacimler (84 Daire)', 'Daire İçi', 'Afyon Şeker Özel CNC Alınlı', 840.00, 0.00, 'PLANNED');

-- Seed Cut Order & Cut Items
INSERT INTO cut_orders (id, cut_order_no, project_id, location_id, machine_name, operator_name, planned_start, status,
                        notes)
VALUES (1, 'CUT-2026-00981', 1, 1, 'Köprü Kesme - CNC 01', 'Mustafa Yılmaz', '2026-09-04', 'COMPLETED',
        'Lobi duvar kaplama için 80x120 cm ebatlı kesim.');

INSERT INTO cut_items (id, item_code, cut_order_id, source_slab_id, width_cm, length_cm, thickness_cm, area_m2,
                       edge_finish, unit_cost, target_location, status)
VALUES (1, 'ITM-2026-00101', 1, 1, 80.00, 120.00, 2.00, 0.9600, 'PAHLI_CILALI', 1365.00, 'Lobi A Blok Kuzey Duvarı',
        'READY'),
       (2, 'ITM-2026-00102', 1, 1, 80.00, 120.00, 2.00, 0.9600, 'PAHLI_CILALI', 1365.00, 'Lobi A Blok Kuzey Duvarı',
        'READY'),
       (3, 'ITM-2026-00103', 1, 1, 80.00, 120.00, 2.00, 0.9600, 'PAHLI_CILALI', 1365.00, 'Lobi A Blok Güney Duvarı',
        'READY');

-- Seed Site Consumption (Granit yapıştırıcı & Montaj işçilik)
INSERT INTO site_consumptions (id, location_id, project_id, consumption_type, item_name, quantity, unit, unit_cost,
                               total_cost, notes)
VALUES (1, 1, 1, 'ADHESIVE', 'Yüksek Mukavemetli Granit Yapıştırıcı', 400.00, 'TORBA', 220.00, 88000.00,
        'Lobi zemin ve duvar montaj harcı'),
       (2, 1, 1, 'LABOR', '1. Sınıf Mermer Montaj Ustası', 320.00, 'SAAT', 450.00, 144000.00,
        'A Blok Lobi montaj işçilik puantajı');

-- Synchronize sequences with inserted IDs
SELECT setval(pg_get_serial_sequence('roles', 'id'), COALESCE((SELECT MAX(id) FROM roles), 1));
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval(pg_get_serial_sequence('cost_centers', 'id'), COALESCE((SELECT MAX(id) FROM cost_centers), 1));
SELECT setval(pg_get_serial_sequence('quarries', 'id'), COALESCE((SELECT MAX(id) FROM quarries), 1));
SELECT setval(pg_get_serial_sequence('blocks', 'id'), COALESCE((SELECT MAX(id) FROM blocks), 1));
SELECT setval(pg_get_serial_sequence('pallets', 'id'), COALESCE((SELECT MAX(id) FROM pallets), 1));
SELECT setval(pg_get_serial_sequence('production_orders', 'id'), COALESCE((SELECT MAX(id) FROM production_orders), 1));
SELECT setval(pg_get_serial_sequence('slabs', 'id'), COALESCE((SELECT MAX(id) FROM slabs), 1));
SELECT setval(pg_get_serial_sequence('scrap_logs', 'id'), COALESCE((SELECT MAX(id) FROM scrap_logs), 1));
SELECT setval(pg_get_serial_sequence('projects', 'id'), COALESCE((SELECT MAX(id) FROM projects), 1));
SELECT setval(pg_get_serial_sequence('project_locations', 'id'), COALESCE((SELECT MAX(id) FROM project_locations), 1));
SELECT setval(pg_get_serial_sequence('cut_orders', 'id'), COALESCE((SELECT MAX(id) FROM cut_orders), 1));
SELECT setval(pg_get_serial_sequence('cut_items', 'id'), COALESCE((SELECT MAX(id) FROM cut_items), 1));
SELECT setval(pg_get_serial_sequence('site_consumptions', 'id'), COALESCE((SELECT MAX(id) FROM site_consumptions), 1));
