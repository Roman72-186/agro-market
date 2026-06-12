# Промт для нового чата — добить мелкие пункты AgroMarket Android (всё за один прогон)

Контекст: см. [audit.md](audit.md). Задачи #1, #2, #3, #4 закрыты 2026-06-11. Ниже —
6 мелких пунктов из раздела «Мелкие/на усмотрение» — все закрыты 2026-06-11.

## Закрыто — 2026-06-11

### ~~1. RegisterScreen: валидация пароля 6 → 8 символов~~ — закрыто 2026-06-11
В `app/src/main/java/ru/agromarket/ui/auth/RegisterScreen.kt`, метод `setPassword` (или
аналог) валидирует пароль как «минимум 6 символов». Бэкенд требует 8–128 (см. audit.md,
пункт 2; уже используется в `ForgotPasswordScreen`/`ChangePasswordScreen` — взять оттуда
текст ошибки и константу для консистентности).

Сделано: вынесены общие `MIN_PASSWORD_LENGTH`/`PASSWORD_LENGTH_ERROR` в новый файл
`ui/auth/PasswordRules.kt`, переиспользованы в `RegisterScreen`/`ForgotPasswordScreen`/
`ChangePasswordScreen`.

### ~~2. Живой прогон ForgotPasswordScreen / ChangePasswordScreen~~ — закрыто 2026-06-11
`ForgotPasswordScreen`/`ChangePasswordScreen` (добавлены 2026-06-11) проверены только
`assembleDebug`/`lint`/`testDebugUnitTest`, не запускались на эмуляторе/устройстве.
Если эмулятор/устройство доступны — прогнать сценарии: запрос кода восстановления,
сброс пароля по коду, смена пароля из профиля (включая обработку 401, если бэкенд
инвалидирует токен после смены). Если эмулятора нет — явно зафиксировать в audit.md,
что пункт остаётся непроверенным, и не делать вид, что проверено.

Сделано: прогнано на эмуляторе `Medium_Phone_API_36.1` — шаг EMAIL (запрос кода) и
клиентская валидация шага RESET (код < 6 символов, пароль < 8 символов) работают
корректно. Полный сброс по реальному коду из письма и `ChangePasswordScreen` живьём
не проверены (нужен реальный email/аккаунт) — детали и обоснование в audit.md, пункт 2.

### ~~3. Запустить CryptoManagerTest (androidTest)~~ — закрыто 2026-06-11
`CryptoManagerTest` (`app/src/androidTest`, 5 тестов: round-trip, decrypt на мусоре/
пустой строке/после ротации ключа) добавлен 2026-06-11, но ни разу не запускался.
Если есть эмулятор/устройство: `./gradlew connectedDebugAndroidTest`. Если упадёт —
разобраться, баг в тесте или в `CryptoManager`, и пофиксить. Если эмулятора нет —
зафиксировать как остающийся открытым пункт.

Сделано: запущен `connectedDebugAndroidTest` на `Medium_Phone_API_36.1` — все 5 тестов
`CryptoManagerTest` (+ `AppContextTest`, итого 6) `PASSED`.

### ~~4. runBlocking в AuthInterceptor / TokenAuthenticator~~ — закрыто 2026-06-11
Оставлено сознательно (комментарии в коде про синхронизацию конкурентных refresh).
Это не баг — просто перепроверить: остаётся ли решение оправданным, нет ли новых
сигналов (профилирование, жалобы на задержки), которые делают `@Volatile`-кэш токена
из `Flow` оправданным сейчас. Если сигналов нет — оставить как есть и зафиксировать
это явно в audit.md (не трогать код без причины).

Сделано: новых сигналов нет, решение остаётся оправданным, код не менялся.

### ~~5. FileUtils.uriToFile — сжатие/ресайз фото~~ — закрыто 2026-06-11
Сейчас без сжатия/ресайза, только защита от роста `cacheDir` (чистка `upload_*`
старше часа). Добавить пасс с `ExifInterface`: правильный поворот по EXIF-ориентации
и ресайз/сжатие перед отправкой (например, downscale до разумного максимума по
большей стороне + JPEG quality). Сохранить существующую логику очистки cacheDir.

Сделано: добавлены `ExifInterface`-поворот, downscale до 1600px по большей стороне +
JPEG quality 85, логика очистки `cacheDir` сохранена. Зависимость
`androidx.exifinterface:exifinterface:1.3.7` добавлена в `app/build.gradle.kts`.

### ~~6. CreateAdScreen.kt — разбить на шаги~~ — закрыто 2026-06-11
441 строка, мастер из 3-4 шагов в одном файле — самый крупный файл в проекте.
Разбить по шагам на отдельные composable-функции (по аналогии с тем, как организован
`RegisterScreen`/`ForgotPasswordScreen` — степ-енам + отдельные composable на шаг,
ViewModel остаётся в одном файле экрана). Не менять поведение, только структуру.

Сделано: разбит на `CreateAdTypeStep`/`CreateAdCategoryStep`/`CreateAdSubCategoryStep`/
`CreateAdFormStep` (по одному файлу на шаг), `CreateAdScreen.kt` сократился до ~280 строк,
поведение не изменилось (`assembleDebug`/`lint`/`testDebugUnitTest` зелёные).

## Конвенции (важно)

- ViewModel — в файле экрана, `@HiltViewModel`, состояние через `var ... by mutableStateOf(...)`.
- Сеть только через `AgroRepository`, `safeCall { ... }` → `ApiResult<T>`.
- Сборка: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"` (PowerShell,
  не Bash — Bash ломает синтаксис `$env:`), затем `.\gradlew.bat assembleDebug -q`,
  `.\gradlew.bat lint -q`, `.\gradlew.bat :app:testDebugUnitTest -q`.
- В конце — обновить `audit.md` и `next-session-prompt.md` по конвенции проекта
  (закрытые пункты помечать `~~...~~ — закрыто <дата>`).
- Отвечать пользователю по-русски, на «ты».
