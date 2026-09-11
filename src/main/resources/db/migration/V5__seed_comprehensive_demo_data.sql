-- ==============================================================================
-- Flyway Migration V5: Seed Comprehensive Demo Data
-- Populates extensive domain dataset for operational testing & demonstration
-- ==============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. KULLANICILAR & ROLLER
INSERT IGNORE INTO users (id, username, email, password, first_name, last_name, enabled, deleted, created_at) VALUES
(4, 'ocak_sefi', 'ocak@ozerler.com', '$2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC', 'Osman', 'Güler', TRUE, FALSE, CURRENT_TIMESTAMP),
(5, 'operator_ali', 'ali.kaya@ozerler.com', '$2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC', 'Ali', 'Yıldız', TRUE, FALSE, CURRENT_TIMESTAMP),
(6, 'atolye_sefi', 'atolye@ozerler.com', '$2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC', 'Serkan', 'Özkan', TRUE, FALSE, CURRENT_TIMESTAMP),
(7, 'muhasebe', 'finans@ozerler.com', '$2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC', 'Burcu', 'Çelik', TRUE, FALSE, CURRENT_TIMESTAMP),
(8, 'kalite_uzmani', 'kalite@ozerler.com', '$2a$10$sdoBblM.8QEwHawmFd6qpuiLOPKNIG7se5maBAwcnQrcZGyM/1vsC', 'Deniz', 'Arslan', TRUE, FALSE, CURRENT_TIMESTAMP);

INSERT IGNORE INTO user_roles (user_id, role_id) VALUES
(4, 3), (4, 2),
(5, 5), (5, 2),
(6, 6), (6, 2),
(7, 8), (7, 2),
(8, 10), (8, 2);

-- 2. EK OCAKLAR
INSERT IGNORE INTO quarries (id, code, name, location, specific_gravity, license_no) VALUES
(4, 'Q-DNZ-01', 'Denizli Klasik Traverten', 'Denizli - Kaklık', 2.50, 'DNZ-2021-912'),
(5, 'Q-ANT-01', 'Finike Lymra Ocağı', 'Antalya - Finike', 2.45, 'ANT-2024-118'),
(6, 'Q-BIL-01', 'Bilecik Bej & Rozaliya Ocağı', 'Bilecik - Söğüt', 2.71, 'BIL-2023-304');

-- 3. EK BLOKLAR (Çeşitli taş türleri, ebatlar ve kantar sapmaları)
INSERT IGNORE INTO blocks (id, quarry_id, block_code, extraction_date, width_cm, length_cm, height_cm, volume_m3, theoretical_weight_kg, actual_weight_kg, weight_deviation_pct, stone_type, color_tone, quality_grade, crack_level, status, extraction_cost, transport_cost, total_cost, notes) VALUES
(4, 4, 'BLK-2026-00128', '2026-09-02', 175, 300, 145, 7.612, 19030.00, 18800.00, -1.21, 'Denizli Traverten', 'Açık Ceviz Damar Kesim', 'A', 0, 'FACTORY_STOCK', 31000.00, 5400.00, 36400.00, 'Gözenek yapısı homojen, dolgulu imalat için uygun.'),
(5, 1, 'BLK-2026-00129', '2026-09-03', 165, 275, 135, 6.126, 16540.00, 17550.00, 6.10, 'Muğla Beyaz', 'Gri Gölgeli Bulutlu', 'B', 2, 'FACTORY_STOCK', 34000.00, 6800.00, 40800.00, 'Kantar sapması %6.10 (>%5). Ayna nemli tartılmış olabilir.'),
(6, 2, 'BLK-2026-00130', '2026-09-04', 195, 320, 155, 9.672, 26307.00, 26100.00, -0.79, 'Afyon Bal', 'Koyu Bal Sarısı', 'EXTRA', 0, 'FACTORY_STOCK', 58000.00, 8900.00, 66900.00, 'Birinci sınıf lüks otel lobisi için rezerve edildi.'),
(7, 5, 'BLK-2026-00131', '2026-09-05', 185, 295, 150, 8.186, 20055.00, 19900.00, -0.77, 'Finike Limra', 'Beyaz Fosilli Homojen', 'A', 0, 'QUARRY', 28000.00, 0.00, 28000.00, 'Dış cephe mekanik montaj için uygun hafif taş.'),
(8, 6, 'BLK-2026-00132', '2026-09-06', 170, 285, 140, 6.783, 18381.00, 18150.00, -1.26, 'Bilecik Rozaliya', 'Pembe Alevli Damarlı', 'B', 1, 'FACTORY_STOCK', 36000.00, 7100.00, 43100.00, 'Klasik rozaliya deseni; katrak kesimine alındı.'),
(9, 3, 'BLK-2026-00133', '2026-09-07', 180, 305, 150, 8.235, 22069.00, 21900.00, -0.77, 'Burdur Bej', 'Açık Krem İnci Tane', 'A', 0, 'SAWING', 44000.00, 7500.00, 51500.00, 'Katrak 02 üzerinde kesim aşamasında.'),
(10, 1, 'BLK-2026-00134', '2026-09-08', 160, 260, 130, 5.408, 14601.00, 14200.00, -2.75, 'Muğla Beyaz', 'Koyu Gri Çizgili', 'C', 3, 'SAWING', 24000.00, 5100.00, 29100.00, 'Yüksek kırık seviyesi; atölyede süpürgelik ve bordür ebatlamaya ayrıldı.');

-- 4. EK PALETLER
INSERT IGNORE INTO pallets (id, pallet_code, warehouse_location, packaging_type, qr_code_hash, status, gross_weight_kg) VALUES
(3, 'PAL-2026-00147', 'Ambar-A / Sehpa-08', 'EXPORT_CRATE', 'HASH-PAL-2026-00147-QR', 'SHIPPED', 5200.00),
(4, 'PAL-2026-00148', 'Ambar-C / Sundurma-02', 'A_FRAME', 'HASH-PAL-2026-00148-QR', 'OPEN', 3450.00),
(5, 'PAL-2026-00149', 'Ambar-B / Sehpa-15', 'BUNDLE', 'HASH-PAL-2026-00149-QR', 'PACKED', 6100.00);

-- 5. EK FABRİKA ÜRETİM EMİRLERİ
INSERT IGNORE INTO production_orders (id, order_no, block_id, machine_name, process_type, start_time, end_time, duration_hours, electricity_kwh, blade_wear_mm, operator_name, status, notes) VALUES
(2, 'PRD-2026-00453', 2, 'Katrak-02 (100 Lamalı)', 'GANGSAW', '2026-09-04 07:30:00', '2026-09-04 17:00:00', 9.50, 490.00, 1.45, 'Ali Yıldız', 'COMPLETED', 'Afyon Şeker bloğundan 54 plaka elde edildi.'),
(3, 'PRD-2026-00454', 4, 'ST Blok Kesme (Elmas Testere)', 'BLOCK_CUTTER', '2026-09-05 08:00:00', '2026-09-05 14:00:00', 6.00, 260.00, 0.85, 'Mehmet Can', 'COMPLETED', 'Denizli traverten kalın basamak şeritleri kesildi.'),
(4, 'PRD-2026-00455', 8, 'Katrak-01 (80 Lamalı)', 'GANGSAW', '2026-09-07 08:30:00', '2026-09-07 18:00:00', 9.50, 480.00, 1.35, 'Ahmet Kaya', 'COMPLETED', 'Rozaliya bloğu lamalara sarıldı; 42 plaka çıktı.'),
(5, 'PRD-2026-00456', 9, 'Katrak-02 (100 Lamalı)', 'GANGSAW', '2026-09-09 08:00:00', NULL, NULL, 180.00, 0.50, 'Ali Yıldız', 'IN_PROGRESS', 'Kesim devam ediyor; saatte 18 cm ilerleme.');

-- 6. EK PLAKALAR
INSERT IGNORE INTO slabs (id, slab_code, order_id, block_id, pallet_id, thickness_cm, width_cm, length_cm, surface_area_m2, surface_finish, quality_grade, gloss_level, cost_per_m2, status) VALUES
(4, 'SLB-2026-00854', 1, 1, 1, 2.00, 175.00, 285.00, 4.9875, 'POLISHED', 'EXTRA', 92, 1340.20, 'RESERVED'),
(5, 'SLB-2026-00855', 2, 2, 2, 2.00, 165.00, 275.00, 4.5375, 'HONED', 'A', 60, 1220.00, 'AVAILABLE'),
(6, 'SLB-2026-00856', 2, 2, 2, 2.00, 165.00, 275.00, 4.5375, 'HONED', 'A', 62, 1220.00, 'AVAILABLE'),
(7, 'SLB-2026-00857', 2, 2, NULL, 2.00, 165.00, 275.00, 4.5375, 'RAW', 'B', 0, 1060.00, 'AVAILABLE'),
(8, 'SLB-2026-00858', 3, 4, 4, 3.00, 170.00, 290.00, 4.9300, 'BRUSHED', 'A', 40, 890.00, 'AVAILABLE'),
(9, 'SLB-2026-00859', 3, 4, 4, 3.00, 170.00, 290.00, 4.9300, 'BRUSHED', 'B', 38, 770.00, 'RESERVED'),
(10, 'SLB-2026-00860', 4, 8, 5, 2.00, 165.00, 280.00, 4.6200, 'POLISHED', 'A', 86, 1290.00, 'AVAILABLE'),
(11, 'SLB-2026-00861', 4, 8, 5, 2.00, 165.00, 280.00, 4.6200, 'POLISHED', 'B', 82, 1120.00, 'AVAILABLE'),
(12, 'SLB-2026-00862', 4, 8, NULL, 2.00, 165.00, 280.00, 4.6200, 'RAW', 'C', 0, 730.00, 'AVAILABLE');

-- 7. 10 NEDEN KODLU FİRE KAYITLARININ TAMAMI (FR-01'den FR-10'a)
INSERT IGNORE INTO scrap_logs (id, scrap_code, order_id, block_id, slab_id, reason_code, scrap_weight_kg, scrap_area_m2, cost_impact, description, logged_by) VALUES
(3, 'SCRAP-2026-0003', 2, 2, NULL, 'FR_03', 380.00, 1.8500, 2100.00, 'Katrak hidrolik basınç dalgalanması sebebiyle gönye kaçması.', 'Ali Yıldız'),
(4, 'SCRAP-2026-0004', 1, 1, 3, 'FR_04', 210.00, 1.2000, 950.00, 'Vakumlu vantuz taşıyıcıdan sehpaya bırakırken köşe kırılması.', 'Ali Yıldız'),
(5, 'SCRAP-2026-0005', 3, 4, NULL, 'FR_05', 180.00, 0.9500, 820.00, 'Köprü kesme operatörü cetvel sıfırlama hatası.', 'Mustafa Yılmaz'),
(6, 'SCRAP-2026-0006', 4, 8, 12, 'FR_06', 320.00, 2.1000, 1650.00, 'Plaka gönyesinde 4 mm bombe oluşumu; kalınlık homojenliği bozuldu.', 'Deniz Arslan'),
(7, 'SCRAP-2026-0007', 2, 2, 7, 'FR_07', 150.00, 0.8000, 750.00, 'Cila hattı 4. abrasiv taş çizgi bıraktı; epoksi dolgu gerekti.', 'Serkan Özkan'),
(8, 'SCRAP-2026-0008', 4, 8, NULL, 'FR_08', 410.00, 2.3000, 1920.00, 'Aynı bloktan beklenmeyen koyu kahve damar geçişi; seleksiyon ayrımı yapıldı.', 'Deniz Arslan'),
(9, 'SCRAP-2026-0009', 1, 1, NULL, 'FR_09', 280.00, 1.5000, 1100.00, '80x120 kesimden artan üçgen ve kenar parçaları mozaik stokuna aktarıldı.', 'Mustafa Yılmaz'),
(10, 'SCRAP-2026-0010', NULL, NULL, NULL, 'FR_10', 520.00, 2.8000, 3400.00, 'Şantiye vinç indirmesinde palet çemberi gevşemesi sonucu kırım.', 'Mehmet Demir');

-- 8. EK MİMARİ PROJELER
INSERT IGNORE INTO projects (id, project_code, name, customer_name, contract_value, estimated_cost, actual_cost, start_date, delivery_date, status, notes) VALUES
(2, 'PROJ-2026-0043', 'Bosphorus Waterfront Marina Yalıları', 'Boğaziçi Gayrimenkul Ltd.', 24500000.00, 16200000.00, 15800000.00, '2026-06-15', '2027-02-28', 'ACTIVE', 'Açık bej döşeme, bookmatch şömine kaplamaları ve rıhtım basamakları.'),
(3, 'PROJ-2026-0044', 'Anadolu Finans Kulesi B Blok', 'Kule Yapı Ortaklığı', 18900000.00, 12100000.00, 12650000.00, '2026-07-01', '2027-08-31', 'ACTIVE', '35 kat asansör söveleri ve zemin kat plaza mekanik kaplaması.'),
(4, 'PROJ-2026-0045', 'Kapadokya Cave Resort & Spa', 'Peri Turizm Yatırımları', 6800000.00, 4200000.00, 4100000.00, '2026-05-10', '2026-11-30', 'COMPLETED', 'Traverten antik eskitme havuz kenarı ve spa hamam kurnaları.');

-- Proje Mahal Ağacı (WBS Locations)
INSERT IGNORE INTO project_locations (id, project_id, parent_id, location_name, floor_level, stone_spec, planned_area_m2, installed_area_m2, status) VALUES
(4, 2, NULL, 'Yalı Giriş Holü & Galeri Boşluğu', 'Zemin Kat', 'Afyon Bal Bookmatch 2cm', 650.00, 620.00, 'IN_PROGRESS'),
(5, 2, NULL, 'Rıhtım ve Açık Teras', 'Dış Mekan', 'Denizli Traverten 40x80 Eskitme', 1400.00, 1350.00, 'COMPLETED'),
(6, 3, NULL, 'Plaza Ana Giriş Atrium', 'Giriş', 'Muğla Beyaz & Toros Siyahı Bordür', 2200.00, 600.00, 'IN_PROGRESS');

-- 9. EK ATÖLYE EMİRLERİ & PARÇALAR
INSERT IGNORE INTO cut_orders (id, cut_order_no, project_id, location_id, machine_name, operator_name, planned_start, status, notes) VALUES
(2, 'CUT-2026-00982', 1, 2, 'Köprü Kesme - CNC 02', 'Hasan Demir', '2026-09-06', 'COMPLETED', 'Koridorlar için 60x60 kalibre ebatlama.'),
(3, 'CUT-2026-00983', 2, 4, 'Su Jeti (Waterjet) - 01', 'Mustafa Yılmaz', '2026-09-08', 'IN_PROGRESS', 'Özel kavisli galeri basamakları ve süpürgelik kesimi.');

INSERT IGNORE INTO cut_items (id, item_code, cut_order_id, source_slab_id, width_cm, length_cm, thickness_cm, area_m2, edge_finish, unit_cost, target_location, status) VALUES
(4, 'ITM-2026-00104', 2, 2, 60.00, 60.00, 2.00, 0.3600, 'PAHLI_HONLU', 1150.00, 'Kat 3 Koridor Aksı', 'INSTALLED'),
(5, 'ITM-2026-00105', 2, 2, 60.00, 60.00, 2.00, 0.3600, 'PAHLI_HONLU', 1150.00, 'Kat 3 Koridor Aksı', 'INSTALLED'),
(6, 'ITM-2026-00106', 3, 4, 35.00, 140.00, 2.00, 0.4900, 'BALIKSIRTI_CILALI', 1650.00, 'Galeri Basamak #01', 'READY');

-- 10. EK ŞANTİYE TÜKETİMİ & PUANTAJ
INSERT IGNORE INTO site_consumptions (id, location_id, project_id, consumption_type, item_name, quantity, unit, unit_cost, total_cost, notes) VALUES
(3, 2, 1, 'GROUT', 'Antibakteriyel Esnek Derz Dolgusu (Açık Bej)', 150.00, 'TORBA', 180.00, 27000.00, 'Tip kat koridor derz uygulaması'),
(4, 4, 2, 'MECHANICAL_ANCHOR', 'Paslanmaz Çelik Z Ankraj & Pim Takımı', 850.00, 'ADET', 45.00, 38250.00, 'Galeri bookmatch mekanik cephe asma elemanları'),
(5, 5, 2, 'SEALANT', 'Silikon Esaslı Taş Emprenye Yalıtım Sıvısı', 60.00, 'LITRE', 320.00, 19200.00, 'Rıhtım deniz tuzu ve su itici yüzey koruyucu');

-- 11. MALİYET İŞLEMLERİ
INSERT IGNORE INTO cost_transactions (id, center_id, block_id, slab_id, project_id, expense_type, amount, allocation_key, description) VALUES
(1, 1, 1, NULL, NULL, 'RAW_MATERIAL', 42000.00, 'DIRECT_BLOCK', 'BLK-2026-00125 Ocak ayna çıkarma ve kesim bedeli'),
(2, 6, 1, NULL, NULL, 'LOGISTICS', 7200.00, 'FREIGHT_WEIGHT', 'Muğla Ocak - Fabrika tır nakliye bedeli'),
(3, 2, 1, NULL, NULL, 'ENERGY', 4620.00, 'MACHINE_HOURS', 'Katrak 01 kesim elektrik sarfiyatı (8.5 saat)'),
(4, 3, NULL, 1, NULL, 'CHEMICAL', 850.00, 'SURFACE_M2', 'Epoksi file ve kristal cila abrasiv tüketimi'),
(5, 4, NULL, NULL, 1, 'LABOR', 12400.00, 'DIRECT_LABOR', 'Atölye köprü kesme ve lobi özel ebatlama işçiliği'),
(6, 5, NULL, NULL, 1, 'INSTALLATION', 88000.00, 'PROJECT_WBS', 'X Residence Lobi yapıştırıcı ve montaj sarfiyatı');

-- 12. SEVKİYATLAR
INSERT IGNORE INTO shipments (id, waybill_no, project_id, vehicle_plate, driver_name, departure_time, distance_km, freight_cost, delivery_status) VALUES
(1, 'IRS-2026-00441', 1, '48 K 8841', 'Kemal Yurt', '2026-09-05 09:00:00', 680, 16500.00, 'DELIVERED'),
(2, 'IRS-2026-00442', 2, '03 AF 192', 'Süleyman Vural', '2026-09-07 11:30:00', 420, 12800.00, 'DELIVERED'),
(3, 'IRS-2026-00443', 3, '34 BRC 77', 'Murat Eren', '2026-09-09 14:00:00', 710, 18500.00, 'IN_TRANSIT');

SET FOREIGN_KEY_CHECKS = 1;
