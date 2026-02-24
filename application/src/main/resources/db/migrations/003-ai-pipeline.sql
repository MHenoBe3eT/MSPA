--liquibase formatted sql

--changeset mspa:003-ai-pipeline

CREATE TABLE IF NOT EXISTS ai_processing_results (
    id UUID PRIMARY KEY,
    uploaded_file_id UUID NOT NULL REFERENCES uploaded_files(id),
    raw_extracted_text TEXT NOT NULL,
    model_version VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS medical_documents (
    id UUID PRIMARY KEY,
    uploaded_file_id UUID NOT NULL REFERENCES uploaded_files(id),
    user_id UUID NOT NULL REFERENCES users(id),
    document_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    document_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_medical_documents_user_id ON medical_documents(user_id);
CREATE INDEX IF NOT EXISTS idx_medical_documents_uploaded_file_id ON medical_documents(uploaded_file_id);

CREATE TABLE IF NOT EXISTS lab_analysis_data (
    id UUID PRIMARY KEY,
    medical_document_id UUID NOT NULL REFERENCES medical_documents(id) ON DELETE CASCADE,
    indicators JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS visit_protocol_data (
    id UUID PRIMARY KEY,
    medical_document_id UUID NOT NULL REFERENCES medical_documents(id) ON DELETE CASCADE,
    narrative_text TEXT NOT NULL,
    complaints TEXT,
    anamnesis TEXT,
    diagnosis TEXT,
    treatment_plan TEXT
);

CREATE TABLE IF NOT EXISTS instrumental_study_data (
    id UUID PRIMARY KEY,
    medical_document_id UUID NOT NULL REFERENCES medical_documents(id) ON DELETE CASCADE,
    description TEXT NOT NULL,
    findings JSONB
);

CREATE TABLE IF NOT EXISTS ai_interpretations (
    id UUID PRIMARY KEY,
    medical_document_id UUID NOT NULL REFERENCES medical_documents(id) ON DELETE CASCADE,
    interpretation_text TEXT NOT NULL,
    risk_markers JSONB NOT NULL,
    disclaimer TEXT NOT NULL,
    model_version VARCHAR(100) NOT NULL
);
