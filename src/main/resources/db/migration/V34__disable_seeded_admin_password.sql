-- Disable the Flyway-seeded admin when it still has the known V4 password hash.
-- The row is kept. No new password is written. An operator must set a new
-- password and re-enable the account. Rollback: set enabled = true and restore
-- the previous password hash from a backup. Do not put that hash back in Git.

UPDATE users
SET enabled = FALSE,
    password = '{locked-reset-required}'
WHERE email = 'admin@eimece.test'
  AND password = '$2a$10$8BFsneTqayq13mMdL6YkSunZo8CRlyNWZXAvbze.oHgRFSxtXMLHq';
