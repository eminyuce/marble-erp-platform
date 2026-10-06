-- V26: Quarry Invoicing Category and Stock Card Direct Expense Tracking

-- 1. Add quarry_category to invoices for tracking fixed purchase categories (MAZOT, SARF_MALZEME, ELEKTRIK, DIGER)
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS quarry_category VARCHAR(30);

-- 2. Add direct_expense flag to stock_items to distinguish direct operational expenses from inventory stock items
ALTER TABLE stock_items ADD COLUMN IF NOT EXISTS direct_expense BOOLEAN DEFAULT false;
