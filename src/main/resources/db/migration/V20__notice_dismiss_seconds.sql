INSERT INTO system_settings (setting_key, setting_value, category, description)
SELECT 'ui.notice.dismiss_seconds', '100', 'GENERAL',
       'Tamamlandı, İşlem tamamlanamadı ve Kontrol edin bantlarının ekranda kalma süresi (saniye)'
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE setting_key = 'ui.notice.dismiss_seconds');
