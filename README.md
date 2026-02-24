# MSPA — Medical Storage and Predictive Analysis

Сервис для загрузки медицинских документов (PDF/изображения), их структурированного хранения и AI-анализа через DeepSeek.

---

## Содержание

- [Архитектура](#архитектура)
- [Технологический стек](#технологический-стек)
- [Быстрый старт (Docker)](#быстрый-старт-docker)
- [Локальный запуск](#локальный-запуск)
- [Переменные окружения](#переменные-окружения)
- [API](#api)
- [Аутентификация](#аутентификация)
- [AI-провайдеры](#ai-провайдеры)
- [Структура базы данных](#структура-базы-данных)
- [Тесты](#тесты)
- [Структура проекта](#структура-проекта)

---

## Архитектура

Проект построен по принципам **Domain-Driven Design** и **Ports & Adapters** (гексагональная архитектура).
Разделён на 5 Gradle-модулей с однонаправленными зависимостями:

```
application  ──▶  rest  ──▶  useCase  ──▶  domain
     │                  ▶  infrastructure  ──▶  domain
     │                                          ▲
     └────────────────────────────────────────────
```

| Модуль           | Назначение                                                    |
|------------------|---------------------------------------------------------------|
| `domain`         | Доменные сущности и типы. Нет зависимостей.                   |
| `useCase`        | Бизнес-логика. Интерфейсы-порты (репозитории, хранилище, AI). |
| `rest`           | REST-контроллеры. Кодогенерация по OpenAPI-спеку.             |
| `infrastructure` | JPA-сущности, MinIO, JWT, DeepSeek, реализация портов.        |
| `application`    | Точка входа Spring Boot. Сборка всех бинов, конфигурация.     |

### Основной поток обработки документа

```
POST /uploaded-files (multipart)
  │
  ├─ Валидация (тип, размер ≤10 MB)
  ├─ SHA-256 дедупликация
  ├─ Сохранение в MinIO
  ├─ Запись в БД со статусом UPLOADED
  └─ Асинхронный запуск AI-обработки (@Async)
       │
       ├─ Скачивание файла из MinIO
       ├─ Отправка в AI-провайдер (DeepSeek / Stub)
       ├─ Парсинг JSON-ответа
       ├─ Сохранение AiProcessingResult (сырой текст)
       ├─ Создание MedicalDocument(s) с типом и датой
       ├─ Сохранение структурированных данных (Lab / Visit / Instrumental)
       ├─ Сохранение AiInterpretation (текст + маркеры риска)
       └─ Обновление статуса: PROCESSED (или ERROR)
```

---

## Технологический стек

| Компонент           | Технология                      |
|---------------------|---------------------------------|
| Язык                | Kotlin 2.1.21, JVM 21           |
| Фреймворк           | Spring Boot 3.4.3               |
| База данных         | PostgreSQL 16                   |
| Миграции            | Liquibase 4.25.0                |
| Объектное хранилище | MinIO                           |
| Аутентификация      | JWT (jjwt 0.12.6) + BCrypt      |
| AI-интеграция       | Spring AI 1.0.0 + DeepSeek Chat |
| ORM                 | Spring Data JPA + Hibernate     |
| Сборка              | Gradle 8.x (multi-module)       |
| Тесты               | JUnit 5, MockK, Testcontainers  |

---

## Быстрый старт (Docker)

Запускает PostgreSQL, MinIO и само приложение. AI-провайдер по умолчанию — **stub** (без внешних вызовов).

```bash
docker-compose up --build
```

После запуска:

| Сервис        | URL                                             |
|---------------|-------------------------------------------------|
| API           | http://localhost:8080/api                       |
| Swagger UI    | http://localhost:8080/api/swagger-ui.html       |
| MinIO Console | http://localhost:9001 (minioadmin / minioadmin) |

Остановить и удалить тома:

```bash
docker-compose down -v
```

### Включение DeepSeek в Docker

Отредактируйте `docker-compose.yml`, добавив переменные в секцию `app.environment`:

```yaml
MSPA_AI_PROVIDER: deepseek
SPRING_AI_MODEL_CHAT: deepseek
SPRING_AI_DEEPSEEK_API_KEY: sk-ваш_ключ
```

---

## Локальный запуск

Требования: Java 21, запущенные PostgreSQL и MinIO.

### 1. Поднять инфраструктуру

```bash
# Только PostgreSQL и MinIO без сборки приложения
docker-compose up postgres minio
```

### 2. Собрать проект

```bash
./gradlew :application:bootJar
```

### 3. Запустить приложение

```bash
java -jar application/build/libs/application-*.jar \
  --spring.config.location=application.properties
```

Или через Gradle:

```bash
./gradlew :application:bootRun
```

Приложение стартует на `http://localhost:8080/api`.

---

## API

Базовый путь: `/api`

Полная интерактивная документация доступна в Swagger UI после запуска:
`http://localhost:8080/api/swagger-ui.html`

### Аутентификация

| Метод    | Эндпоинт           | Описание                          |
|----------|--------------------|-----------------------------------|
| `POST`   | `/auth/register`   | Регистрация нового пользователя   |
| `POST`   | `/auth/login`      | Вход и получение JWT-токена       |

**Регистрация:**
```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret123",
  "name": "Андрей Петров"
}
```

**Вход:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret123"
}
```

Ответ: `{ "token": "<JWT>" }`

### Загрузка файлов

Все запросы требуют заголовка `Authorization: Bearer <token>`.

| Метод | Эндпоинт                          | Описание                                          |
|-------|-----------------------------------|---------------------------------------------------|
| `POST` | `/uploaded-files`                 | Загрузить файл (multipart/form-data, поле `file`) |
| `GET` | `/uploaded-files/{id}`            | Статус загрузки + список документов               |
| `GET` | `/uploaded-files/{id}/download`   | Скачать исходный файл                             |

**Допустимые форматы:** `application/pdf`, `image/jpeg`, `image/png`
**Максимальный размер:** 10 MB

**Загрузка:**
```http
POST /api/uploaded-files
Authorization: Bearer <token>
Content-Type: multipart/form-data

file=@analysis.pdf
```

Ответ: `202 Accepted` + объект с полем `status: "UPLOADED"`. Обработка идёт асинхронно — статус меняется на `PROCESSING` → `PROCESSED` (или `ERROR`).

**Статусы:**

| Статус         | Описание                        |
|----------------|---------------------------------|
| `UPLOADED`     | Файл получен, ожидает обработки |
| `PROCESSING`   | AI-обработка в процессе         |
| `PROCESSED`    | Обработан, документы созданы    |
| `ERROR`        | Ошибка при обработке            |

### Документы

| Метод    | Эндпоинт          | Описание                          |
|----------|-------------------|-----------------------------------|
| `GET`    | `/documents`      | Список документов пользователя    |
| `GET`    | `/documents/{id}` | Детали: данные + AI-интерпретация |
| `PATCH`  | `/documents/{id}` | Изменить тип документа            |
| `DELETE` | `/documents/{id}` | Удалить документ                  |

**Фильтрация списка:**
```
GET /api/documents?type=LAB_ANALYSIS&startDate=2024-01-01&endDate=2024-12-31
```

**Типы документов:**

| Тип                  | Описание                                          |
|----------------------|---------------------------------------------------|
| `LAB_ANALYSIS`       | Лабораторный анализ (кровь, моча и т.д.)          |
| `VISIT_PROTOCOL`     | Протокол приёма врача                             |
| `INSTRUMENTAL_STUDY` | Инструментальное исследование (УЗИ, МРТ, рентген) |

---

## Аутентификация

Приложение использует **JWT Bearer Token**.

### Авторизация в Swagger UI

1. Откройте `http://localhost:8080/api/swagger-ui.html`
2. Выполните `POST /auth/register` или `POST /auth/login`
3. Скопируйте значение поля `token` из ответа
4. Нажмите кнопку **Authorize** (в правом верхнем углу)
5. Введите `Bearer <ваш_токен>` и нажмите **Authorize**

### Авторизация в curl / Postman

```bash
# Получить токен
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secret123"}' \
  | jq -r '.token')

# Использовать токен
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/documents
```

**Параметры токена:**
- Срок действия: 1 час (3 600 000 мс)

---

## AI-провайдеры

Провайдер выбирается через `mspa.ai.provider`.

### Stub (по умолчанию)

```properties
mspa.ai.provider=stub
spring.ai.model.chat=none
```

Возвращает фиктивные данные анализа крови (ОАК). Используется для разработки и тестов — не требует API-ключа и интернета.

### DeepSeek

```properties
mspa.ai.provider=deepseek
spring.ai.model.chat=deepseek
spring.ai.deepseek.api-key=sk-...
```

Отправляет содержимое файла в DeepSeek Chat (multimodal). Системный промпт извлекает из документа:
- Тип документа и дату
- Структурированные данные (показатели, диагноз, заключение)
- Интерпретацию с маркерами риска
- Дисклеймер (рекомендации по лечению не включаются)

### Запуск интеграционных тестов DeepSeek (с реальным API)

```bash
DEEPSEEK_API_KEY=sk-xxx ./gradlew :application:test --tests "ai.DeepSeekRealFilesIntegrationTest"
```

---

## Структура базы данных

Миграции применяются автоматически при старте через Liquibase (`db/changelog.xml`).

```
users
├── id (UUID PK)
├── email (UNIQUE)
├── password_hash
├── name
└── created_at

uploaded_files
├── id (UUID PK)
├── user_id → users(id)
├── original_file_name
├── content_type
├── size_bytes
├── checksum (SHA-256)         ◄─ UNIQUE(checksum, user_id) — дедупликация
├── storage_key (MinIO path)
├── status
└── uploaded_at

medical_documents
├── id (UUID PK)
├── uploaded_file_id → uploaded_files(id)
├── user_id → users(id)
├── document_type
├── status
├── title
├── document_date
└── created_at

lab_analysis_data
├── id, medical_document_id
└── indicators (JSONB)         ◄─ [{name, code, value, unit, referenceRange}]

visit_protocol_data
├── id, medical_document_id
├── narrative_text
├── complaints, anamnesis, diagnosis, treatment_plan

instrumental_study_data
├── id, medical_document_id
├── description
└── findings (JSONB)

ai_processing_results
├── id, uploaded_file_id
├── raw_extracted_text
├── model_version
└── processed_at

ai_interpretations
├── id, medical_document_id
├── interpretation_text
├── risk_markers (JSONB)       ◄─ ["Гемоглобин ниже нормы", ...]
├── disclaimer
└── model_version
```

---

## Тесты

```bash
# Все тесты (кроме DeepSeek-интеграционных)
./gradlew :application:test

# Конкретный класс
./gradlew :application:test --tests "auth.AuthIntegrationTest"

# DeepSeek с реальным API
DEEPSEEK_API_KEY=sk-xxx ./gradlew :application:test --tests "ai.DeepSeekRealFilesIntegrationTest"
```

Интеграционные тесты автоматически поднимают PostgreSQL и MinIO через **Testcontainers** — Docker должен быть запущен.

| Тест                                | Тип           | Покрытие                               |
|-------------------------------------|---------------|----------------------------------------|
| `AuthIntegrationTest`               | Integration   | Регистрация, вход, JWT                 |
| `AuthenticateUserUseCaseTest`       | Unit          | Аутентификация, неверный пароль        |
| `RegisterUserUseCaseTest`           | Unit          | Регистрация, дублирование email        |
| `JwtTokenProviderTest`              | Unit          | Генерация, валидация, истечение токена |
| `UploadIntegrationTest`             | Integration   | Загрузка файла, статус, MinIO          |
| `UploadFileUseCaseTest`             | Unit          | Валидация размера, типа, дедупликация  |
| `DocumentIntegrationTest`           | Integration   | CRUD документов                        |
| `ProcessUploadedFileUseCaseTest`    | Unit          | AI-оркестрация                         |
| `AiProcessingIntegrationTest`       | Integration   | Загрузка → обработка → статус          |
| `DeepSeekRealFilesIntegrationTest`  | E2E           | Реальный вызов DeepSeek API            |
| `LiquibaseMigrationIntegrationTest` | Integration   | Применение миграций                    |

---

## Структура проекта

```
MSPA/
├── domain/src/main/kotlin/domain/
│   ├── user/User.kt
│   ├── file/UploadedFile.kt
│   ├── document/
│   │   ├── MedicalDocument.kt
│   │   ├── LabAnalysisData.kt
│   │   ├── VisitProtocolData.kt
│   │   ├── InstrumentalStudyData.kt
│   │   └── AiInterpretation.kt
│   ├── ai/AiProcessingResult.kt
│   └── DocumentType.kt
│
├── useCase/src/main/kotlin/
│   ├── auth/RegisterUserUseCase.kt, AuthenticateUserUseCase.kt
│   ├── file/UploadFileUseCase.kt
│   ├── ai/ProcessUploadedFileUseCase.kt, AiDocumentProvider.kt
│   └── document/GetDocumentDetailsUseCase.kt, DeleteMedicalDocumentUseCase.kt, ...
│
├── rest/
│   ├── src/main/kotlin/controller/
│   │   ├── AuthController.kt
│   │   ├── UploadController.kt
│   │   └── DocumentController.kt
│   └── specs/api-docs.yaml           ← OpenAPI-спек для кодогенерации
│
├── infrastructure/src/main/kotlin/
│   ├── entity/                       ← JPA-сущности
│   ├── repository/                   ← Spring Data репозитории
│   ├── storage/MinioFileStorage.kt
│   ├── ai/StubAiDocumentProvider.kt, DeepSeekAiDocumentProvider.kt
│   └── auth/JwtTokenProvider.kt, JwtAuthenticationFilter.kt
│
├── application/
│   ├── src/main/kotlin/
│   │   ├── MSPAApplication.kt
│   │   └── config/
│   │       ├── SecurityConfig.kt
│   │       ├── MinioConfig.kt
│   │       ├── AsyncConfig.kt
│   │       └── ...
│   ├── src/main/resources/
│   │   └── db/migrations/001-003.sql ← Liquibase-миграции
│   └── src/test/kotlin/              ← Все тесты
│
├── Dockerfile                        ← Multi-stage build
├── docker-compose.yml                ← PostgreSQL + MinIO + App
└── application.properties            ← Конфиг для локального запуска
```
