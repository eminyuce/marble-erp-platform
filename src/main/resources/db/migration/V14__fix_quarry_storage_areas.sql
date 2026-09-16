-- V14: Ensure all quarry blocks are in the two quarry areas:
-- OCAK-URETIM (Üretim Sahası) or OCAK-SEVK (Stok Sahası)

UPDATE blocks
SET current_location_id = (SELECT id FROM stock_locations WHERE code = 'OCAK-SEVK'),
    status = 'PRODUCED'
WHERE current_location_id = (SELECT id FROM stock_locations WHERE code = 'FAB-BLOK')
   OR status IN ('FACTORY_STOCK', 'AT_FACTORY');
