-- Explicit masraf merkezi seçimi satın alma siparişlerinde
ALTER TABLE purchase_orders
    ADD COLUMN cost_center_id BIGINT REFERENCES cost_centers (id);

CREATE INDEX idx_purchase_orders_cost_center ON purchase_orders (cost_center_id);
