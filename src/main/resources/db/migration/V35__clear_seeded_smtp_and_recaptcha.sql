-- Clear placeholder secrets seeded by V4. Rows stay so operators can set real
-- values. Rollback: restore the previous setting_value from a backup. Do not
-- put the old placeholder secrets back into a migration.

UPDATE system_settings
SET setting_value = ''
WHERE setting_key = 'mail.smtp.password'
  AND setting_value = 'smtppassword123';

UPDATE system_settings
SET setting_value = ''
WHERE setting_key IN ('security.recaptcha.site_key', 'security.recaptcha.secret_key')
  AND setting_value LIKE '6LeIx0cqAAAAA%';
