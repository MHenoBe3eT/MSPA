## Implementation Plan

### Phase 0: Cleanup and Foundation

#### 0.1 Remove Card

**Delete files:**
- `domain/src/main/kotlin/domain/card/Card.kt`
- `useCase/src/main/kotlin/card/` (all files)
- `infrastructure/src/main/kotlin/entity/CardEntity.kt`

**Modify:**
- `domain/user/User.kt` — remove `cardId: CardId?`
- `infrastructure/entity/UserEntity.kt` — remove `cardId` column
- `rest/specs/api-docs.yaml` — remove `cardId` from `UserDto`
- `rest/controller/UserController.kt`, `rest/transformer/UserTransformer.kt` — remove Card references

#### 0.2 Update DocumentType

```kotlin
enum class DocumentType {
    LAB_ANALYSIS,
    VISIT_PROTOCOL,
    INSTRUMENTAL_STUDY
}
```

Replace current values: BLOOD_TEST, ULTRASOUND, MRT, DOCTOR_NOTE, ECG, XRAY, OTHER.

#### 0.3 Add User fields for auth

User gets: `email`, `passwordHash`, `createdAt`. UserEntity — accordingly.

#### 0.4 Enable Liquibase + initial migration

- `spring.liquibase.enabled=true`
- Create `application/src/main/resources/db/changelog.xml`
- Create `application/src/main/resources/db/migrations/001-initial-schema.sql`

#### 0.5 Fix dependencies

- Remove Ktor dependencies from `rest/build.gradle.kts`
- Remove javax dependencies (javax.persistence, javax.servlet)
- Remove OAuth2/OIDC from `application/build.gradle.kts`
- Add JWT (jjwt), MinIO, Testcontainers, mockk

---

### Phase 1: Authentication (JWT)

#### Domain
- `User` with fields: email, passwordHash, name, createdAt

#### UseCase
- `auth/RegisterUser.kt` — interface
- `auth/AuthenticateUser.kt` — interface
- `auth/PasswordEncoder.kt` — port
- `auth/TokenProvider.kt` — port
- `auth/RegisterUserUseCase.kt` — check for duplicate email, create user
- `auth/AuthenticateUserUseCase.kt` — verify password, generate token
- `user/GetUserByEmail.kt` — port

#### Infrastructure
- `auth/JwtTokenProvider.kt` — TokenProvider implementation (jjwt)
- `auth/SpringPasswordEncoder.kt` — BCrypt wrapper
- `auth/JwtAuthenticationFilter.kt` — Spring Security filter
- `user/GetUserByEmailFromRepository.kt`
- `UserRepository` — add `findByEmail()`

#### Rest
- `POST /auth/register`, `POST /auth/login`
- `controller/AuthController.kt`

#### Application
- `config/SecurityConfig.kt` — CSRF off, stateless, JWT filter, public endpoints

#### Tests
- Unit: RegisterUserUseCase, AuthenticateUserUseCase
- Integration: full register + login flow

---

### Phase 2: File Upload + MinIO

#### Domain — new entity UploadedFile
```
UploadedFile:
  id: UploadedFileId
  userId: UserId
  originalFileName: String
  contentType: String
  sizeBytes: Long
  checksum: String (SHA-256)
  storageKey: String (MinIO path)
  status: UploadStatus (UPLOADED | PROCESSING | PROCESSED | ERROR)
  uploadedAt: Instant
```

#### UseCase
- `file/FileStorage.kt` — port (store, retrieve, delete)
- `file/SaveUploadedFile.kt`, `file/GetUploadedFile.kt` — ports
- `file/TriggerAiProcessing.kt` — port
- `file/UploadFileUseCase.kt` — validation (size <= 10MB, format), SHA-256 checksum, duplicate check, save to MinIO, trigger processing

#### Infrastructure
- `storage/MinioFileStorage.kt` — FileStorage implementation
- `entity/UploadedFileEntity.kt` + repository
- `file/SaveUploadedFileInRepository.kt`, `file/GetUploadedFileFromRepository.kt`

#### Rest
- `POST /uploaded-files` (multipart) → 202 Accepted
- `GET /uploaded-files/{id}` — status + linked documents
- `GET /uploaded-files/{id}/download` — original file
- `controller/UploadController.kt`

#### Tests
- Unit: size validation, format validation, deduplication
- Integration: MinIO store/retrieve, upload endpoint

---

### Phase 3: AI Abstraction + Asynchronous Pipeline

#### Domain — new entities

**AiProcessingResult:**
```
id, uploadedFileId, rawExtractedText, modelVersion, processedAt
```

**MedicalDocument:**
```
id, uploadedFileId, userId, documentType, status, title, documentDate, createdAt
```

**AiInterpretation:**
```
id, medicalDocumentId, interpretationText, riskMarkers (JSON), disclaimer, modelVersion
```

**LabAnalysisData, VisitProtocolData, InstrumentalStudyData** — as defined in section 8.

#### UseCase — key interface

**`ai/AiDocumentProvider.kt`** — main abstraction:
```kotlin
interface AiDocumentProvider {
    fun processDocument(fileContent: ByteArray, contentType: String, fileName: String): AiExtractionResult
    val modelVersion: String
}

data class AiExtractionResult(rawText: String, documents: List<ExtractedDocument>)
// ExtractedDocument contains: type, title, date, structured data (sealed class), interpretation
```

**`ai/ProcessUploadedFileUseCase.kt`** — orchestrator:
1. Retrieve file from MinIO
2. Call AI provider
3. Save raw text (AiProcessingResult)
4. Create N MedicalDocument records
5. For each — save structured data and interpretation
6. Update file status (PROCESSED or ERROR)

#### Infrastructure
- `ai/AsyncAiProcessingTrigger.kt` — Spring @Async, TriggerAiProcessing implementation
- `ai/StubAiDocumentProvider.kt` — stub for tests and development
- JPA entities + repositories for all new entities
- JSONB columns via hypersistence-utils

#### Tests
- Unit: ProcessUploadedFileUseCase with mocked AI
- Verify status transitions
- Verify creation of N documents from a single file

---

### Phase 4: Document API

#### UseCase
- `document/GetMedicalDocument.kt` — port (byId, byUserId with filter, byUploadedFileId)
- `document/UpdateDocumentTypeUseCase.kt` — user changes document type
- `document/GetDocumentDetailsUseCase.kt` — aggregated view (document + data + interpretation)
- `document/DeleteMedicalDocumentUseCase.kt`

#### Rest
- `GET /documents` — list (with filters by type, date)
- `GET /documents/{id}` — details + structured data + interpretation
- `PATCH /documents/{id}` — update type
- `DELETE /documents/{id}`
- `controller/DocumentController.kt`
- `transformer/DocumentTransformer.kt`

#### Tests
- API tests: list, details, type change, deletion

---

### Phase 5: Database Schema (Liquibase)

Single migration file `001-initial-schema.sql` — see section 13 for table definitions.

In practice, migrations are written alongside each phase.

---

### Phase 6: Integration Testing

#### Test Infrastructure
- `IntegrationTestBase.kt` — Testcontainers (PostgreSQL + MinIO), `@SpringBootTest`, `@DynamicPropertySource`

#### Test Cases
1. **Auth**: register, duplicate email, login, wrong password, protected endpoint without token
2. **Upload**: valid PDF → 202, file > 10MB → 400, unsupported format → 400, duplicate → return existing
3. **Processing**: upload → status transitions UPLOADED→PROCESSING→PROCESSED, N MedicalDocuments created, structured data saved, disclaimer present
4. **Documents**: list with filter, details, type change, deletion
5. **Download**: uploaded file is byte-for-byte identical to original

---

### Execution Order

```
Phase 0 (Cleanup)
    ↓
Phase 5 (DB Schema — iterative, alongside each phase)
    ↓
Phase 1 (Auth)
    ↓
Phase 2 (Upload + MinIO)
    ↓
Phase 3 (AI Pipeline)
    ↓
Phase 4 (Document API)
    ↓
Phase 6 (Integration Tests)
```

### Verification

1. `./gradlew build` — project compiles
2. Start PostgreSQL + MinIO via docker-compose
3. `./gradlew bootRun` — application starts
4. Swagger UI at `/api/swagger-ui.html` — all endpoints visible
5. Register → Login → Upload PDF → Poll status → Get documents — full flow
6. `./gradlew test` — all tests green