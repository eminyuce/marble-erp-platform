-- V18: Makine Mazot Takibi için Çalışma Saati / Kilometre (Saat/Km) alanı ekleme
ALTER TABLE machine_fuel_entries
    ADD COLUMN IF NOT EXISTS working_hours_or_km DECIMAL(12, 2);

CREATE INDEX IF NOT EXISTS idx_machine_fuel_entries_hours ON machine_fuel_entries (working_hours_or_km);
