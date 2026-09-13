-- Seed new Admin User: admin@eimece.test / B2u5c8JB
-- Hash: $2a$10$8BFsneTqayq13mMdL6YkSunZo8CRlyNWZXAvbze.oHgRFSxtXMLHq
INSERT INTO users (id, username, email, password, first_name, last_name, enabled, deleted, created_at)
VALUES (4, 'eimece_admin', 'admin@eimece.test', '$2a$10$8BFsneTqayq13mMdL6YkSunZo8CRlyNWZXAvbze.oHgRFSxtXMLHq',
        'Eimece', 'Yönetici', TRUE, FALSE, CURRENT_TIMESTAMP);

-- Assign Admin Roles
INSERT INTO user_roles (user_id, role_id)
VALUES (4, 1), -- ROLE_ADMIN
       (4, 2), -- ROLE_USER
       (4, 11);
-- ROLE_EXECUTIVE

-- System Settings Table
CREATE TABLE system_settings
(
    id            BIGSERIAL PRIMARY KEY,
    setting_key   VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    category      VARCHAR(50)  NOT NULL,
    description   VARCHAR(255),
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_settings_key ON system_settings (setting_key);

-- Seed Initial System Settings
INSERT INTO system_settings (setting_key, setting_value, category, description)
VALUES ('security.2fa.enabled', 'false', 'SECURITY', 'Tüm kullanıcılar için İki Adımlı Doğrulama (2FA) zorunluluğu'),
       ('security.recaptcha.enabled', 'false', 'SECURITY', 'Giriş ekranında Google reCAPTCHA koruması'),
       ('security.recaptcha.site_key', '6LeIx0cqAAAAAP...', 'SECURITY', 'Google reCAPTCHA v2 / v3 Site Key'),
       ('security.recaptcha.secret_key', '6LeIx0cqAAAAAM...', 'SECURITY', 'Google reCAPTCHA Secret Key'),
       ('mail.smtp.host', 'smtp.mailgun.org', 'SMTP', 'Giden E-Posta SMTP Sunucu Adresi'),
       ('mail.smtp.port', '587', 'SMTP', 'SMTP Port Numarası (587 TLS veya 465 SSL)'),
       ('mail.smtp.username', 'postmaster@ozerler.com', 'SMTP', 'SMTP Kullanıcı Adı / Hesap'),
       ('mail.smtp.password', 'smtppassword123', 'SMTP', 'SMTP Şifresi'),
       ('mail.smtp.from', 'noreply@ozerlermermer.com', 'SMTP', 'Gönderen E-Posta Adresi (From Address)'),
       ('mail.smtp.auth', 'true', 'SMTP', 'SMTP Kimlik Doğrulaması (Auth)'),
       ('mail.smtp.starttls', 'true', 'SMTP', 'STARTTLS Şifreli Bağlantı');

-- Email Templates Table
CREATE TABLE email_templates
(
    id           BIGSERIAL PRIMARY KEY,
    template_key VARCHAR(50)  NOT NULL UNIQUE,
    name         VARCHAR(100) NOT NULL,
    subject      VARCHAR(200) NOT NULL,
    body_html    TEXT         NOT NULL,
    placeholders VARCHAR(255),
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed Email Templates
INSERT INTO email_templates (template_key, name, subject, body_html, placeholders)
VALUES ('USER_WELCOME', 'Kullanıcı Hoş Geldiniz Bildirimi', 'Özerler Mermer ERP Sistemine Hoş Geldiniz',
        '<div style="font-family:sans-serif; padding:20px; color:#1e293b;">
          <h2 style="color:#b45309;">Özerler Mermer ERP Sistemine Hoş Geldiniz</h2>
          <p>Sayın <strong>{{fullName}}</strong>,</p>
          <p>Hesabınız başarıyla oluşturulmuştur. Sisteme aşağıdaki kullanıcı adı veya e-posta adresinizle giriş yapabilirsiniz:</p>
          <div style="background:#f1f5f9; padding:15px; border-radius:8px; margin:15px 0;">
            <p style="margin:5px 0;"><strong>Giriş Adresi:</strong> http://localhost:81/account/adminlogin/</p>
            <p style="margin:5px 0;"><strong>E-Posta:</strong> {{email}}</p>
          </div>
          <p>Güvenliğiniz için ilk girişinizde şifrenizi değiştirmenizi öneririz.</p>
        </div>', 'fullName, email'),

       ('PASSWORD_RESET', 'Şifre Sıfırlama Talebi', 'Özerler Mermer ERP - Şifre Sıfırlama Kodunuz',
        '<div style="font-family:sans-serif; padding:20px; color:#1e293b;">
          <h2 style="color:#b45309;">Şifre Sıfırlama Talebi</h2>
          <p>Sayın <strong>{{fullName}}</strong>,</p>
          <p>Hesabınız için geçici şifre tanımlanmıştır:</p>
          <div style="background:#fef3c7; border-left:4px solid #b45309; padding:15px; border-radius:4px; margin:15px 0;">
            <p style="margin:0; font-size:18px; font-weight:bold; font-family:monospace; color:#92400e;">{{temporaryPassword}}</p>
          </div>
          <p>Giriş yaptıktan sonra şifrenizi profil sayfanızdan güncelleyiniz.</p>
        </div>', 'fullName, temporaryPassword'),

       ('DEFECT_QUARANTINE_ALERT', 'Kritik Mermer Çatlak Riski & Karantina',
        '🔴 ACİL ALARM: Blok Çatlak Riski Nedeniyle Karantina Kararı',
        '<div style="font-family:sans-serif; padding:20px; color:#1e293b;">
          <h2 style="color:#dc2626;">Kritik Kalite Uyarısı & Sevkiyat Karantinası</h2>
          <p>Şantiyede tespit edilen gizli kılcal çatlak nedeniyle <strong>{{blockCode}}</strong> kaynaklı tüm plakalar kalite kontrol karantinasına alınmıştır.</p>
          <div style="background:#fee2e2; border-left:4px solid #dc2626; padding:15px; border-radius:4px; margin:15px 0;">
            <p style="margin:5px 0;"><strong>Kaynak Blok:</strong> {{blockCode}} ({{quarryName}})</p>
            <p style="margin:5px 0;"><strong>Etkilenen Kardeş Plakalar:</strong> {{affectedCount}} Adet</p>
            <p style="margin:5px 0;"><strong>Risk Seviyesi:</strong> Kritik / Derin Kılcal Çatlak</p>
          </div>
          <p>İlgili ürünlerin fabrika ambarından çıkışı derhal durdurulmuştur.</p>
        </div>', 'blockCode, quarryName, affectedCount'),

       ('STOCK_SHORTAGE_WARNING', 'Proje Mahal Taş İhtiyaç Uyarısı',
        '🟠 UYARI: Şantiye İçin Taş Üretim Açığı Tespit Edildi',
        '<div style="font-family:sans-serif; padding:20px; color:#1e293b;">
          <h2 style="color:#d97706;">Şantiye Üretim İhtiyacı Bildirimi</h2>
          <p><strong>{{projectName}}</strong> projesinin <strong>{{locationName}}</strong> mahali için planlanan montaj takviminde stok açığı tespit edildi.</p>
          <div style="background:#fffbeb; border-left:4px solid #d97706; padding:15px; border-radius:4px; margin:15px 0;">
            <p style="margin:5px 0;"><strong>Gereken Metraj:</strong> {{plannedArea}} m²</p>
            <p style="margin:5px 0;"><strong>Mevcut Serbest Stok:</strong> {{availableStock}} m²</p>
            <p style="margin:5px 0;"><strong>Açılması Gereken Üretim Emri:</strong> {{shortfall}} m²</p>
          </div>
        </div>', 'projectName, locationName, plannedArea, availableStock, shortfall');

-- Synchronize sequences with inserted IDs
SELECT setval(pg_get_serial_sequence('users', 'id'), COALESCE((SELECT MAX(id) FROM users), 1));
SELECT setval(pg_get_serial_sequence('system_settings', 'id'), COALESCE((SELECT MAX(id) FROM system_settings), 1));
SELECT setval(pg_get_serial_sequence('email_templates', 'id'), COALESCE((SELECT MAX(id) FROM email_templates), 1));
