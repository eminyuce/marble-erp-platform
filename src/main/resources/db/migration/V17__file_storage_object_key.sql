-- ==============================================================================
-- V17: Add MinIO object-storage metadata to file_storage
-- Existing file_path values remain until FileStorageMigrationService rewrites them.
-- ==============================================================================

ALTER TABLE file_storage
    ADD COLUMN IF NOT EXISTS object_key VARCHAR(500),
    ADD COLUMN IF NOT EXISTS bucket_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS checksum VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS idx_file_storage_object_key
    ON file_storage (object_key)
    WHERE object_key IS NOT NULL;
