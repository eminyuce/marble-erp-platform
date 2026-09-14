-- Seed demo sales and purchase orders so those Tabulator grids have rows.
-- Customers and suppliers were created in V6/V7 without matching orders.

INSERT INTO sales_orders (order_no, customer_id, order_date, delivery_date, total_amount, paid_amount, status, notes)
SELECT 'SIP-2026-00101', c.id, DATE '2026-08-12', DATE '2026-09-28', 186400.00, 80000.00, 'CONFIRMED',
       'X Residence lobi plaka teslimatı'
FROM customers c
WHERE c.customer_code = 'MUS-001'
  AND NOT EXISTS (SELECT 1 FROM sales_orders so WHERE so.order_no = 'SIP-2026-00101');

INSERT INTO sales_orders (order_no, customer_id, order_date, delivery_date, total_amount, paid_amount, status, notes)
SELECT 'SIP-2026-00102', c.id, DATE '2026-08-25', DATE '2026-10-15', 94250.00, 94250.00, 'INVOICED',
       'Yeşilyurt şantiye basamak ve süpürgelik'
FROM customers c
WHERE c.customer_code = 'MUS-002'
  AND NOT EXISTS (SELECT 1 FROM sales_orders so WHERE so.order_no = 'SIP-2026-00102');

INSERT INTO sales_orders (order_no, customer_id, order_date, delivery_date, total_amount, paid_amount, status, notes)
SELECT 'SIP-2026-00103', c.id, DATE '2026-09-04', DATE '2026-09-30', 41200.00, 0.00, 'DRAFT',
       'Bayi stok siparişi — onay bekliyor'
FROM customers c
WHERE c.customer_code = 'MUS-003'
  AND NOT EXISTS (SELECT 1 FROM sales_orders so WHERE so.order_no = 'SIP-2026-00103');

INSERT INTO sales_order_items (sales_order_id, description, quantity, unit, unit_price, line_total)
SELECT so.id, 'Afyon Şeker cilalı plaka 2 cm', 48.00, 'm2', 1850.00, 88800.00
FROM sales_orders so
WHERE so.order_no = 'SIP-2026-00101'
  AND NOT EXISTS (SELECT 1 FROM sales_order_items i WHERE i.sales_order_id = so.id);

INSERT INTO sales_order_items (sales_order_id, description, quantity, unit, unit_price, line_total)
SELECT so.id, 'Denizli traverten basamak 3 cm', 25.00, 'm2', 3770.00, 94250.00
FROM sales_orders so
WHERE so.order_no = 'SIP-2026-00102'
  AND NOT EXISTS (SELECT 1 FROM sales_order_items i WHERE i.sales_order_id = so.id);

INSERT INTO sales_order_items (sales_order_id, description, quantity, unit, unit_price, line_total)
SELECT so.id, 'Muğla Beyaz honlu plaka', 20.00, 'm2', 2060.00, 41200.00
FROM sales_orders so
WHERE so.order_no = 'SIP-2026-00103'
  AND NOT EXISTS (SELECT 1 FROM sales_order_items i WHERE i.sales_order_id = so.id);

INSERT INTO purchase_orders (po_number, supplier_id, project_id, order_date, expected_delivery, total_amount, status, notes)
SELECT 'PO-2026-00041', s.id, p.id, DATE '2026-08-18', DATE '2026-09-05', 28600.00, 'DELIVERED',
       'X Residence kimyasal ve sarf malzeme'
FROM suppliers s
         JOIN projects p ON p.project_code = 'PROJ-2026-0042'
WHERE s.supplier_code = 'TED-002'
  AND NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.po_number = 'PO-2026-00041');

INSERT INTO purchase_orders (po_number, supplier_id, project_id, order_date, expected_delivery, total_amount, status, notes)
SELECT 'PO-2026-00042', s.id, p.id, DATE '2026-09-01', DATE '2026-09-20', 154800.00, 'CONFIRMED',
       'Marina yalıları elmas tel ve aparat'
FROM suppliers s
         JOIN projects p ON p.project_code = 'PROJ-2026-0043'
WHERE s.supplier_code = 'TED-003'
  AND NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.po_number = 'PO-2026-00042');

INSERT INTO purchase_orders (po_number, supplier_id, project_id, order_date, expected_delivery, total_amount, status, notes)
SELECT 'PO-2026-00043', s.id, NULL, DATE '2026-09-08', DATE '2026-09-22', 18750.00, 'SENT',
       'Fabrika genel sarf siparişi'
FROM suppliers s
WHERE s.supplier_code = 'TED-001'
  AND NOT EXISTS (SELECT 1 FROM purchase_orders po WHERE po.po_number = 'PO-2026-00043');

INSERT INTO purchase_order_items (purchase_order_id, item_name, item_type, quantity, unit, unit_price, line_total, delivery_status)
SELECT po.id, 'Epoksi dolgu reçinesi', 'CHEMICAL', 40.00, 'KG', 715.00, 28600.00, 'DELIVERED'
FROM purchase_orders po
WHERE po.po_number = 'PO-2026-00041'
  AND NOT EXISTS (SELECT 1 FROM purchase_order_items i WHERE i.purchase_order_id = po.id);

INSERT INTO purchase_order_items (purchase_order_id, item_name, item_type, quantity, unit, unit_price, line_total, delivery_status)
SELECT po.id, 'Elmas tel 11 mm', 'EQUIPMENT', 12.00, 'ADET', 12900.00, 154800.00, 'PENDING'
FROM purchase_orders po
WHERE po.po_number = 'PO-2026-00042'
  AND NOT EXISTS (SELECT 1 FROM purchase_order_items i WHERE i.purchase_order_id = po.id);

INSERT INTO purchase_order_items (purchase_order_id, item_name, item_type, quantity, unit, unit_price, line_total, delivery_status)
SELECT po.id, 'Portland çimento 50 kg', 'CONSUMABLE', 75.00, 'TORBA', 250.00, 18750.00, 'PENDING'
FROM purchase_orders po
WHERE po.po_number = 'PO-2026-00043'
  AND NOT EXISTS (SELECT 1 FROM purchase_order_items i WHERE i.purchase_order_id = po.id);

SELECT setval(pg_get_serial_sequence('sales_orders', 'id'), COALESCE((SELECT MAX(id) FROM sales_orders), 1));
SELECT setval(pg_get_serial_sequence('sales_order_items', 'id'), COALESCE((SELECT MAX(id) FROM sales_order_items), 1));
SELECT setval(pg_get_serial_sequence('purchase_orders', 'id'), COALESCE((SELECT MAX(id) FROM purchase_orders), 1));
SELECT setval(pg_get_serial_sequence('purchase_order_items', 'id'),
              COALESCE((SELECT MAX(id) FROM purchase_order_items), 1));
