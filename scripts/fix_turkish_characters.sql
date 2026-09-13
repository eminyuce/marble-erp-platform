-- Repair Turkish characters stored with incorrect encoding
-- Apply with: psql -U marbleuser -d marble_erp -f scripts/fix_turkish_characters.sql
SET client_encoding = 'UTF8';

-- Roles (V3 seed)
UPDATE roles SET description = 'Sistem Yöneticisi - Tam Yetki' WHERE id = 1;
UPDATE roles SET description = 'Standart Kullanıcı' WHERE id = 2;
UPDATE roles SET description = 'Ocak Şefi / Formeni' WHERE id = 3;
UPDATE roles SET description = 'Fabrika Müdürü' WHERE id = 4;
UPDATE roles SET description = 'Makine Operatörü' WHERE id = 5;
UPDATE roles SET description = 'Atölye Şefi' WHERE id = 6;
UPDATE roles SET description = 'Şantiye Şefi / Metraj Mühendisi' WHERE id = 7;
UPDATE roles SET description = 'Maliyet & Finans Uzmanı' WHERE id = 8;
UPDATE roles SET description = 'Satış & İhracat Sorumlusu' WHERE id = 9;
UPDATE roles SET description = 'Kalite Kontrol Uzmanı' WHERE id = 10;
UPDATE roles SET description = 'Genel Müdür / Şirket Ortağı' WHERE id = 11;

-- Users
UPDATE users SET first_name = 'Sistem', last_name = 'Yöneticisi' WHERE id = 1;
UPDATE users SET first_name = 'Ahmet', last_name = 'Kaya' WHERE id = 2;
UPDATE users SET first_name = 'Mehmet', last_name = 'Demir' WHERE id = 3;
UPDATE users SET first_name = 'Osman', last_name = 'Güler' WHERE id = 4;
UPDATE users SET first_name = 'Ali', last_name = 'Yıldız' WHERE id = 5;
UPDATE users SET first_name = 'Serkan', last_name = 'Özkan' WHERE id = 6;
UPDATE users SET first_name = 'Burcu', last_name = 'Çelik' WHERE id = 7;
UPDATE users SET first_name = 'Deniz', last_name = 'Arslan' WHERE id = 8;

-- System settings descriptions (V4 seed)
UPDATE system_settings SET description = 'Tüm kullanıcılar için İki Adımlı Doğrulama (2FA) zorunluluğu' WHERE setting_key = 'security.2fa.enabled';
UPDATE system_settings SET description = 'Giriş ekranında Google reCAPTCHA koruması' WHERE setting_key = 'security.recaptcha.enabled';
UPDATE system_settings SET description = 'SMTP Port Numarası (587 TLS veya 465 SSL)' WHERE setting_key = 'mail.smtp.port';
UPDATE system_settings SET description = 'SMTP Kullanıcı Adı / Hesap' WHERE setting_key = 'mail.smtp.username';
UPDATE system_settings SET description = 'SMTP Şifresi' WHERE setting_key = 'mail.smtp.password';
UPDATE system_settings SET description = 'Gönderen E-Posta Adresi (From Address)' WHERE setting_key = 'mail.smtp.from';
UPDATE system_settings SET description = 'SMTP Kimlik Doğrulaması (Auth)' WHERE setting_key = 'mail.smtp.auth';
UPDATE system_settings SET description = 'STARTTLS Şifreli Bağlantı' WHERE setting_key = 'mail.smtp.starttls';

-- Email templates (V4 seed titles)
UPDATE email_templates
SET name = 'Kullanıcı Hoş Geldiniz Bildirimi',
    subject = 'Özerler Mermer ERP Sistemine Hoş Geldiniz',
    body_html = '<div style="font-family:sans-serif; padding:20px; color:#1e293b;">
  <h2 style="color:#b45309;">Özerler Mermer ERP Sistemine Hoş Geldiniz</h2>
  <p>Sayın <strong>{{fullName}}</strong>,</p>
  <p>Hesabınız başarıyla oluşturulmuştur. Sisteme aşağıdaki kullanıcı adı veya e-posta adresinizle giriş yapabilirsiniz:</p>
  <div style="background:#f1f5f9; padding:15px; border-radius:8px; margin:15px 0;">
    <p style="margin:5px 0;"><strong>Giriş Adresi:</strong> http://localhost:81/account/adminlogin/</p>
    <p style="margin:5px 0;"><strong>E-Posta:</strong> {{email}}</p>
  </div>
  <p>Güvenliğiniz için ilk girişinizde şifrenizi değiştirmenizi öneririz.</p>
</div>'
WHERE template_key = 'USER_WELCOME';

UPDATE email_templates
SET name = 'Şifre Sıfırlama Talebi',
    subject = 'Özerler Mermer ERP - Şifre Sıfırlama Kodunuz',
    body_html = '<div style="font-family:sans-serif; padding:20px; color:#1e293b;">
  <h2 style="color:#b45309;">Şifre Sıfırlama Talebi</h2>
  <p>Sayın <strong>{{fullName}}</strong>,</p>
  <p>Hesabınız için geçici şifre tanımlanmıştır:</p>
  <div style="background:#fef3c7; border-left:4px solid #b45309; padding:15px; border-radius:4px; margin:15px 0;">
    <p style="margin:0; font-size:18px; font-weight:bold; font-family:monospace; color:#92400e;">{{temporaryPassword}}</p>
  </div>
  <p>Giriş yaptıktan sonra şifrenizi profil sayfanızdan güncelleyiniz.</p>
</div>'
WHERE template_key = 'PASSWORD_RESET';

UPDATE email_templates
SET name = 'Kritik Mermer Çatlak Riski & Karantina',
    subject = 'ACİL ALARM: Blok Çatlak Riski Nedeniyle Karantina Kararı',
    body_html = '<div style="font-family:sans-serif; padding:20px; color:#1e293b;">
  <h2 style="color:#dc2626;">Kritik Kalite Uyarısı & Sevkiyat Karantinası</h2>
  <p>Şantiyede tespit edilen gizli kılcal çatlak nedeniyle <strong>{{blockCode}}</strong> kaynaklı tüm plakalar kalite kontrol karantinasına alınmıştır.</p>
  <div style="background:#fee2e2; border-left:4px solid #dc2626; padding:15px; border-radius:4px; margin:15px 0;">
    <p style="margin:5px 0;"><strong>Kaynak Blok:</strong> {{blockCode}} ({{quarryName}})</p>
    <p style="margin:5px 0;"><strong>Etkilenen Kardeş Plakalar:</strong> {{affectedCount}} Adet</p>
    <p style="margin:5px 0;"><strong>Risk Seviyesi:</strong> Kritik / Derin Kılcal Çatlak</p>
  </div>
  <p>İlgili ürünlerin fabrika ambarından çıkışı derhal durdurulmuştur.</p>
</div>'
WHERE template_key = 'DEFECT_QUARANTINE_ALERT';

UPDATE email_templates
SET name = 'Proje Mahal Taş İhtiyaç Uyarısı',
    subject = 'UYARI: Şantiye İçin Taş Üretim Açığı Tespit Edildi',
    body_html = '<div style="font-family:sans-serif; padding:20px; color:#1e293b;">
  <h2 style="color:#d97706;">Şantiye Üretim İhtiyacı Bildirimi</h2>
  <p><strong>{{projectName}}</strong> projesinin <strong>{{locationName}}</strong> mahali için planlanan montaj takviminde stok açığı tespit edildi.</p>
  <div style="background:#fffbeb; border-left:4px solid #d97706; padding:15px; border-radius:4px; margin:15px 0;">
    <p style="margin:5px 0;"><strong>Gereken Metraj:</strong> {{plannedArea}} m²</p>
    <p style="margin:5px 0;"><strong>Mevcut Serbest Stok:</strong> {{availableStock}} m²</p>
    <p style="margin:5px 0;"><strong>Açılması Gereken Üretim Emri:</strong> {{shortfall}} m²</p>
  </div>
</div>'
WHERE template_key = 'STOCK_SHORTAGE_WARNING';

-- Domain demo rows that may have been repaired before; keep them correct.
UPDATE quarries SET name = 'Yatağan Beyaz Ocağı', location = 'Muğla - Yatağan' WHERE id = 1;
UPDATE quarries SET name = 'Afyon Şeker Ocağı', location = 'Afyonkarahisar - İscehisar' WHERE id = 2;
UPDATE quarries SET name = 'Burdur Bej Ocağı', location = 'Burdur - Karamanlı' WHERE id = 3;
UPDATE quarries SET name = 'Denizli Klasik Traverten', location = 'Denizli - Kaklık' WHERE id = 4;
UPDATE quarries SET name = 'Finike Limra Ocağı', location = 'Antalya - Finike' WHERE id = 5;
UPDATE quarries SET name = 'Bilecik Bej & Rozaliya Ocağı', location = 'Bilecik - Söğüt' WHERE id = 6;
