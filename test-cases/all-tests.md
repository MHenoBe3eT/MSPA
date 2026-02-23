# Все реализованные тест-кейсы

---

## Phase 1 — Аутентификация (JWT)

### Unit-тесты

#### RegisterUserUseCase (`RegisterUserUseCaseTest.kt`)

1. **Успешная регистрация** — валидный email/password/name → создаётся User с захешированным паролем, возвращается сохранённый объект
2. **Дубликат email** — email уже существует → выбрасывается `EmailAlreadyExistsException`, `CreateUser.create()` не вызывается
3. **Пароль хешируется** — `PasswordEncoder.encode()` вызывается ровно один раз, в User попадает хеш, а не plain text

#### AuthenticateUserUseCase (`AuthenticateUserUseCaseTest.kt`)

4. **Успешный логин** — верный email + пароль → возвращается JWT токен
5. **Неверный пароль** — правильный email, неправильный пароль → выбрасывается `InvalidCredentialsException`
6. **Несуществующий email** — email не найден → та же `InvalidCredentialsException` (не раскрываем наличие аккаунта)

#### JwtTokenProvider (`JwtTokenProviderTest.kt`)

7. **Генерация токена** — по UserId/email генерируется валидный JWT (3 части, проходит `isValid`)
8. **Парсинг токена** — из валидного токена корректно извлекаются userId и email
9. **Просроченный токен** — JWT с отрицательным `expiration` → `isValid` возвращает `false`
10. **Повреждённый токен** — произвольная строка → `isValid` возвращает `false`
11. **Токен с чужой подписью** — JWT, подписанный другим ключом → `isValid` возвращает `false`

---

### Integration-тесты (`AuthIntegrationTest.kt`)

> Инфраструктура: Testcontainers PostgreSQL

#### POST /auth/register

12. **201** — валидный запрос → пользователь создан, в ответе `id`, `email`, `name`; поля `password` и `passwordHash` отсутствуют
13. **409** — повторная регистрация с тем же email → Conflict
14. **400** — невалидный email → Bad Request
15. **400** — пустой пароль → Bad Request
16. **400** — пустое имя → Bad Request

#### POST /auth/login

17. **200** — верные credentials → в ответе JWT токен
18. **401** — неверный пароль → Unauthorized
19. **401** — несуществующий email → Unauthorized

#### Защищённые эндпоинты

20. **401** — запрос без токена к защищённому эндпоинту → Unauthorized
21. **401** — запрос с невалидным/просроченным токеном → Unauthorized
22. **200** — запрос с валидным JWT → доступ разрешён (не 401/403)

#### Полный flow (e2e)

23. **Register → Login → Access** — регистрация, логин, использование полученного токена для доступа к защищённому эндпоинту

---

## Phase 2 — File Upload + MinIO

### Unit-тесты (`UploadFileUseCaseTest.kt`)

24. **Превышение размера** — файл > 10 MB → выбрасывается `FileTooLargeException`, `FileStorage.store()` и `SaveUploadedFile.save()` не вызываются
25. **Граничный размер** — файл ровно 10 MB → успешная загрузка, `sizeBytes` корректен
26. **Неподдерживаемый формат** — `application/msword` → выбрасывается `UnsupportedFileFormatException`, файл не сохраняется
27. **PDF принимается** — `application/pdf` → статус `UPLOADED`, файл сохранён
28. **JPEG принимается** — `image/jpeg` → статус `UPLOADED`, файл сохранён
29. **PNG принимается** — `image/png` → статус `UPLOADED`, файл сохранён
30. **Дедупликация** — повторная загрузка того же содержимого тем же пользователем → возвращается существующий файл, `store()` и `save()` не вызываются повторно
31. **Нет дедупликации между пользователями** — одинаковый checksum, другой userId → файл сохраняется как новый
32. **SHA-256 checksum** — вычисляется корректно: 64 hex-символа, детерминированный результат
33. **AI processing trigger** — после успешной загрузки `TriggerAiProcessing.trigger()` вызывается ровно один раз
34. **Storage key** — ключ содержит `userId`, `fileId` и оригинальное имя файла

---

### Integration-тесты (`UploadIntegrationTest.kt`)

> Инфраструктура: Testcontainers PostgreSQL + MinIO

#### POST /uploaded-files (multipart)

35. **202** — валидный PDF → Accepted, в ответе `id`, `status: UPLOADED`, `originalFileName`
36. **400** — файл > 10 MB → Bad Request
37. **400** — неподдерживаемый MIME-тип → Bad Request
38. **202 + дедупликация** — повторная загрузка того же файла → возвращается тот же `id`

#### GET /uploaded-files/{id}

39. **200** — существующий файл → статус и метаданные в ответе

#### GET /uploaded-files/{id}/download

40. **200** — скачанный файл байт-в-байт совпадает с оригинально загруженным

#### Авторизация

41. **401** — запрос без токена → Unauthorized
42. **404** — несуществующий `id` → Not Found

---

## Phase 3 — AI Abstraction + Asynchronous Pipeline

### Unit-тесты (`ProcessUploadedFileUseCaseTest.kt`)

43. **Переходы статусов при успехе** — файл проходит UPLOADED → PROCESSING → PROCESSED: `UpdateUploadedFileStatus.update()` вызывается ровно дважды в правильном порядке
44. **Создание N документов** — AI возвращает N `ExtractedDocument` → создаётся ровно N `MedicalDocument`, каждый с корректным `documentType`, `status=PROCESSED`, `userId`, `uploadedFileId`
45. **Сохранение структурированных данных** — для документа типа `LAB_ANALYSIS` вызывается `SaveLabAnalysisData.save()` с корректными индикаторами (имя, значение, единица)
46. **Интерпретация для каждого документа** — `SaveAiInterpretation.save()` вызывается ровно N раз (по одному на каждый извлечённый документ)
47. **Переход в ERROR при сбое AI** — `AiDocumentProvider.processDocument()` бросает исключение → статус становится ERROR, `SaveMedicalDocument.save()` не вызывается, исключение пробрасывается наверх
48. **Метаданные передаются в AI** — `AiDocumentProvider.processDocument()` получает правильный `contentType` и `originalFileName` из `UploadedFile`

---

## Phase 4 — Document API

### Integration-тесты (`DocumentIntegrationTest.kt`)

> Инфраструктура: Testcontainers PostgreSQL + MinIO
> Подготовка: загрузка PDF → ожидание статуса `PROCESSED` (StubAiDocumentProvider создаёт 1 документ типа `LAB_ANALYSIS`)

#### GET /documents

49. **200 — список документов** — после загрузки и обработки файла → массив содержит созданный документ с `id` и `documentType: LAB_ANALYSIS`
50. **200 — фильтр по типу** — `?type=LAB_ANALYSIS` → возвращаются только документы нужного типа; `?type=VISIT_PROTOCOL` → пустой массив

#### GET /documents/{id}

51. **200 — детали документа** — ответ содержит поля документа, непустой блок `interpretation` (с `interpretationText` и `disclaimer`), непустой блок `labData` с массивом `indicators`

#### PATCH /documents/{id}

52. **200 — смена типа** — передаём `{"documentType": "VISIT_PROTOCOL"}` → ответ содержит обновлённый `documentType`; повторный `GET /documents/{id}` подтверждает изменение

#### DELETE /documents/{id}

53. **204 — удаление** — документ удалён; повторный `GET /documents/{id}` возвращает 404

#### Авторизация и ошибки

54. **401** — запросы `GET /documents` и `GET /documents/{id}` без токена → Unauthorized
55. **404** — `GET /documents/00000000-0000-0000-0000-000000000000` с валидным токеном → Not Found

---

## Phase 5 — Database Schema (Liquibase)

### Integration-тесты (`LiquibaseMigrationIntegrationTest.kt`)

> Инфраструктура: Testcontainers PostgreSQL
> Метод проверки: запросы к `information_schema` и системным каталогам PostgreSQL

#### Таблицы

56. **Все таблицы существуют** — после применения Liquibase миграций присутствуют все 9 таблиц: `users`, `documents`, `uploaded_files`, `ai_processing_results`, `medical_documents`, `lab_analysis_data`, `visit_protocol_data`, `instrumental_study_data`, `ai_interpretations`

#### Колонки

57. **users** — таблица содержит колонки: `id`, `email`, `password_hash`, `name`, `created_at`
58. **uploaded_files** — таблица содержит колонки: `id`, `user_id`, `original_file_name`, `content_type`, `size_bytes`, `checksum`, `storage_key`, `status`, `uploaded_at`
59. **medical_documents** — таблица содержит колонки: `id`, `uploaded_file_id`, `user_id`, `document_type`, `status`, `title`, `document_date`, `created_at`
60. **ai_processing_results** — таблица содержит колонки: `id`, `uploaded_file_id`, `raw_extracted_text`, `model_version`, `processed_at`
61. **ai_interpretations** — таблица содержит колонки: `id`, `medical_document_id`, `interpretation_text`, `risk_markers`, `disclaimer`, `model_version`
62. **visit_protocol_data** — таблица содержит колонки: `id`, `medical_document_id`, `narrative_text`, `complaints`, `anamnesis`, `diagnosis`, `treatment_plan`
63. **instrumental_study_data** — таблица содержит колонки: `id`, `medical_document_id`, `description`, `findings`

#### Типы JSONB

64. **lab_analysis_data.indicators** — колонка имеет тип `jsonb`
65. **ai_interpretations.risk_markers** — колонка имеет тип `jsonb`
66. **instrumental_study_data.findings** — колонка имеет тип `jsonb`

#### Индексы

67. **Все индексы существуют** — присутствуют индексы: `idx_uploaded_files_user_id`, `idx_uploaded_files_checksum_user_id`, `idx_medical_documents_user_id`, `idx_medical_documents_uploaded_file_id`

#### Ограничения

68. **UNIQUE на users.email** — колонка `email` таблицы `users` имеет ограничение `UNIQUE`

---

### Integration-тесты (`AiProcessingIntegrationTest.kt`)

> Инфраструктура: Testcontainers PostgreSQL + MinIO
> Stub AI-провайдер создаёт 1 документ типа `LAB_ANALYSIS` с 4 индикаторами и интерпретацией

#### Переходы статусов

69. **Статус UPLOADED сразу после загрузки** — `POST /uploaded-files` возвращает 202 со статусом `UPLOADED`
70. **Переход в PROCESSED после обработки** — после async AI-обработки `GET /uploaded-files/{id}` возвращает статус `PROCESSED`

#### Создание документов

71. **Создание MedicalDocument** — после обработки `GET /documents` возвращает массив с как минимум одним документом
72. **Структурированные данные и интерпретация** — `GET /documents/{id}` содержит непустые блоки `labData.indicators` (массив) и `interpretation` с полями `interpretationText` и `disclaimer`

#### Статус файла

73. **GET /uploaded-files/{id} после обработки** — возвращает 200 со статусом `PROCESSED`

#### Изоляция данных

74. **Документы изолированы между пользователями** — документы, созданные при загрузке файла первым пользователем, не видны второму пользователю (`GET /documents` возвращает пустой массив)
