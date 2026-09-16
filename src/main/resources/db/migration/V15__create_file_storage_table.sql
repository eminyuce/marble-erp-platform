-- ==============================================================================
-- V15: Create File Storage Table for Reusable Multi-File Management
-- Stores metadata for uploaded files (images, PDFs, DOCX, etc.)
-- Designed for generic entity association (entity_type + entity_id)
-- ==============================================================================

CREATE TABLE file_storage
(
    id               BIGSERIAL PRIMARY KEY,
    file_name        VARCHAR(255)  NOT NULL,
    original_name    VARCHAR(255)  NOT NULL,
    mime_type        VARCHAR(100)  NOT NULL,
    file_size        BIGINT        NOT NULL,
    file_path        VARCHAR(500)  NOT NULL,
    entity_type      VARCHAR(50)   NOT NULL,
    entity_id        BIGINT,
    deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
    deleted_at       TIMESTAMP,
    created_date     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    add_user_id      VARCHAR(128),
    update_user_id   VARCHAR(128)
);

CREATE INDEX idx_file_storage_entity ON file_storage (entity_type, entity_id);
CREATE INDEX idx_file_storage_deleted ON file_storage (deleted);
CREATE INDEX idx_file_storage_file_name ON file_storage (file_name);
