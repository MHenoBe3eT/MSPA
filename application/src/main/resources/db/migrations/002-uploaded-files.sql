--liquibase formatted sql

--changeset mspa:002-uploaded-files

CREATE TABLE IF NOT EXISTS uploaded_files (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    original_file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    storage_key VARCHAR(1024) NOT NULL,
    status VARCHAR(50) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_uploaded_files_user_id ON uploaded_files(user_id);
CREATE INDEX IF NOT EXISTS idx_uploaded_files_checksum_user_id ON uploaded_files(checksum, user_id);
