## Medical Document Processing MVP (MSPA)

### 1. Goal

Backend MVP that allows users (patients) to upload medical documents (PDF/images),
process them asynchronously using AI (OCR + extraction), and store structured medical data for later use.

Primary scenario:
> Upload medical document → 202 Accepted → background AI processing → structured data + interpretation available via API.

---

### 2. Scope

#### Included
- User registration and login (JWT)
- Upload medical documents (≤ 10 MB)
- Asynchronous AI-based OCR + extraction (single pass)
- One uploaded file may produce N medical documents (AI splits them)
- Document categorization (AI-determined, user-correctable)
- Structured storage of extracted data
- AI interpretation (text + risk markers, with disclaimers)
- Original file download

#### Explicitly Excluded
- Medical recommendations or prescriptions
- Medication advice
- Reprocessing existing documents (delete and re-upload instead)
- Frontend implementation (API-first design)
- Cross-analysis and dynamics (next iteration)

---

### 3. Document Types

Supported document categories:
- `LAB_ANALYSIS`
- `VISIT_PROTOCOL` (doctor visit notes)
- `INSTRUMENTAL_STUDY` (e.g. ultrasound, MRI)

Supported languages:
- Russian
- English

Supported formats:
- PDF
- JPG / PNG

---

### 4. Architecture

Style:
- Modular monolith
- Domain-Driven Design (DDD)
- Clear separation between:
    - File storage (MinIO)
    - AI processing artifacts
    - Medical domain model

The system must ensure:
- Original files are immutable
- AI processing happens exactly once per file
- AI results are stored and never recomputed automatically
- File upload is asynchronous (202 Accepted → background processing)

---

### 5. Core Pipeline

```
Upload File (multipart)
  → Validate (size ≤ 10 MB, format, SHA-256 checksum)
  → Check for duplicates (by checksum + userId)
  → Store original file in MinIO
  → Create UploadedFile record (status = UPLOADED)
  → Return 202 Accepted
  → [async] Set status = PROCESSING
  → [async] AI Vision Processing (OCR + Extraction)
  → [async] Store raw extracted text (AiProcessingResult)
  → [async] Create N MedicalDocument records with structured data
  → [async] Run AI interpretation for each document
  → [async] Set status = PROCESSED (or ERROR on failure)
```

On AI error: status becomes `ERROR`. No automatic retry. User must delete and re-upload.

---

### 6. File Storage

- MinIO (S3-compatible)
- Stores only original files
- Database stores file metadata and storage key
- User can download the original file at any time
- Duplicate detection via SHA-256 checksum per user

---

### 7. AI Processing Rules

AI acts as:
- OCR (text recognition)
- Structure extraction
- Document splitting (1 file → N documents)
- Interpretation (limited)

AI is accessed through an abstraction (`AiDocumentProvider` interface) with a stub implementation for development/testing. 
Concrete AI model integration comes later.

AI MUST:
- Output raw recognized text
- Output structured data per document
- Output interpretation separately per document
- Determine document type (user can correct afterwards)
- Include disclaimers for all interpretations
- Never give medical treatment recommendations

AI MUST NOT:
- Prescribe drugs
- Suggest dosages
- Replace doctor consultation

---

### 8. Data Model

#### UploadedFile
```
id, userId, originalFileName, contentType, sizeBytes, checksum (SHA-256),
storageKey (MinIO path), status (UPLOADED | PROCESSING | PROCESSED | ERROR), uploadedAt
```

#### AiProcessingResult
```
id, uploadedFileId, rawExtractedText, modelVersion, processedAt
```

#### MedicalDocument
```
id, uploadedFileId, userId, documentType, status, title, documentDate, createdAt
```

#### AiInterpretation
```
id, medicalDocumentId, interpretationText, riskMarkers (JSON), disclaimer, modelVersion
```

#### Structured Data (per document type)

**LabAnalysisData:**
- Strict JSON structure
- Indicators must be machine-readable
- No interpretation inside structured data
```
id, medicalDocumentId, indicators: List<LabIndicator>
  LabIndicator: name, code?, value, unit?, referenceRange?
```

**VisitProtocolData:**
- Hybrid model: original narrative text + optional structured fields
```
id, medicalDocumentId, narrativeText, complaints?, anamnesis?, diagnosis?, treatmentPlan? (text only, no meds)
```

**InstrumentalStudyData:**
- Text + optional structured findings
```
id, medicalDocumentId, description, findings (JSON)?
```

---

### 9. Domain Rules

- UploadedFile and MedicalDocument are immutable after processing
- AI results are versioned implicitly by model version
- Re-uploading the same file (same checksum + user) does NOT create a duplicate — returns existing record
- Changing AI model does NOT affect existing documents

---

### 10. Authentication

- JWT-based (registration + login)
- Multi-user support
- User fields: email (unique), passwordHash, name, createdAt
- Spring Security with stateless session, JWT filter
- BCrypt password encoding

---

### 11. Technology Stack

- Language: Kotlin
- Framework: Spring Boot 4
- JVM: Java 25
- Database: PostgreSQL
- Migrations: Liquibase
- File Storage: MinIO
- Auth: JWT (jjwt)
- Testing: Testcontainers, mockk
- JSONB: hypersistence-utils

---

### 12. API Endpoints

#### Auth
- `POST /auth/register` — create user
- `POST /auth/login` — authenticate, get JWT

#### Files
- `POST /uploaded-files` (multipart) → 202 Accepted
- `GET /uploaded-files/{id}` — status + linked documents
- `GET /uploaded-files/{id}/download` — original file

#### Documents
- `GET /documents` — list (with filters by type, date)
- `GET /documents/{id}` — details + structured data + interpretation
- `PATCH /documents/{id}` — update type (user correction)
- `DELETE /documents/{id}`

All endpoints except auth require JWT token.

---

### 13. Database Schema

| Table                       | Key Fields                                                                                                 |
|-----------------------------|------------------------------------------------------------------------------------------------------------|
| `users`                     | id, email (UNIQUE), password_hash, name, created_at                                                        |
| `uploaded_files`            | id, user_id (FK), original_file_name, content_type, size_bytes, checksum, storage_key, status              |
| `ai_processing_results`     | id, uploaded_file_id (FK), raw_extracted_text, model_version                                               |
| `medical_documents`         | id, uploaded_file_id (FK), user_id (FK), document_type, status, title, document_date                       |
| `lab_analysis_data`         | id, medical_document_id (FK CASCADE), indicators (JSONB)                                                   |
| `visit_protocol_data`       | id, medical_document_id (FK CASCADE), narrative_text, complaints, anamnesis, diagnosis, treatment_plan     |
| `instrumental_study_data`   | id, medical_document_id (FK CASCADE), description, findings (JSONB)                                        |
| `ai_interpretations`        | id, medical_document_id (FK CASCADE), interpretation_text, risk_markers (JSONB), disclaimer, model_version |

Indexes: `uploaded_files(checksum, user_id)` UNIQUE, `medical_documents(user_id)`, `medical_documents(uploaded_file_id)`.

---

### 14. Non-Goals (Strict)

The following must NOT appear in the codebase:
- Medical advice engines
- Drug databases
- Prescription logic
- Automatic reanalysis pipelines
- Automatic retry of failed AI processing

---

### 15. Design Freedom

Implementation is free to:
- Choose internal data models and persistence schema details
- Choose API request/response structures
- Choose async processing mechanism

As long as:
- DDD boundaries are respected
- AI processing is single-pass and asynchronous
- Original documents are immutable
- Medical safety constraints are enforced
- 1 file → N documents relationship is supported

---