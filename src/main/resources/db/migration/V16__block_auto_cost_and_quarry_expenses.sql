-- Blok otomatik maliyet, satış ve piyasa değeri alanları
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS sale_price NUMERIC(14, 2);
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS sale_date DATE;
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS sale_notes TEXT;
ALTER TABLE blocks ADD COLUMN IF NOT EXISTS unit_market_value_per_ton NUMERIC(14, 2);

ALTER TABLE blocks ALTER COLUMN stone_type DROP NOT NULL;

-- Ocak bazlı gider takibi
ALTER TABLE cost_transactions ADD COLUMN IF NOT EXISTS quarry_id BIGINT REFERENCES quarries(id);

CREATE INDEX IF NOT EXISTS idx_cost_tx_quarry_period ON cost_transactions(quarry_id, expense_period);
CREATE INDEX IF NOT EXISTS idx_cost_tx_business_unit_period ON cost_transactions(business_unit, expense_period);
