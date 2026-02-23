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
