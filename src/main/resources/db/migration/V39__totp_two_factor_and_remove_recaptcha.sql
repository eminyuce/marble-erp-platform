-- 1.0.11: offline TOTP two-factor authentication; Google reCAPTCHA removed.
-- Rollback: drop the new table and columns, and restore system_settings rows from a backup.

-- reCAPTCHA is gone. No table column ever held it; only system_settings rows did.
DELETE FROM system_settings
WHERE setting_key IN ('security.recaptcha.enabled',
                      'security.recaptcha.site_key',
                      'security.recaptcha.secret_key');

-- The old 2FA checkboxes never enforced anything. Mode + enforced replace them.
-- The new mode starts DISABLED, so production login behaviour does not change.
DELETE FROM system_settings
WHERE setting_key IN ('security.2fa.enabled', 'auth.2fa.enabled');

INSERT INTO system_settings (setting_key, setting_value, category, description)
VALUES ('security.2fa.mode', 'DISABLED', 'SECURITY',
        'İki faktörlü doğrulama modu: DISABLED veya AUTHENTICATOR'),
       ('security.2fa.enforced', 'false', 'SECURITY',
        'Tüm kullanıcılar için Authenticator kurulumu zorunlu mu')
ON CONFLICT (setting_key) DO NOTHING;

-- Per-user TOTP state. The secret is AES-256-GCM ciphertext, never plain text.
ALTER TABLE users
    ADD COLUMN totp_secret_encrypted VARCHAR(512),
    ADD COLUMN totp_enabled          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN totp_setup_completed  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN totp_last_used_step   BIGINT,
    ADD COLUMN failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          TIMESTAMP;

-- Single-use backup codes, stored only as BCrypt hashes.
CREATE TABLE user_totp_backup_codes
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(100) NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_totp_backup_codes_user ON user_totp_backup_codes (user_id);
