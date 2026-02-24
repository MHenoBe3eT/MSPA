--liquibase formatted sql

--changeset mspa:001-initial-schema

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    creation_date DATE NOT NULL,
    visit_date DATE NOT NULL,
    file_path VARCHAR(1024) NOT NULL,
    extracted_text TEXT NOT NULL
);
