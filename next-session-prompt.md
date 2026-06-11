# Промт для нового чата — открытые задачи AgroMarket Android

Контекст: см. [audit.md](audit.md), раздел «Осталось открытым» (обновлён 2026-06-11).

## Задачи по приоритету

### 1. ~~Несовпадение `type` между приложением и `GET /feed`~~ — закрыто 2026-06-11
Сверено с `https://agro.assaru.space/openapi.json`: бэкенд самосогласован
(`AdType = "sale"|"rent"|"service"` для `AdCreateRequest.type` и для фильтра `GET /ads/`).
`"land"` в бэкенде не существует — это категория "Земельные участки" (`category_id = 56`).
Поправлено приложение (`FeedScreen`, `CreateAdScreen`, `StatusBadge.adTypeBadge`, `LandsScreen`) —
теперь везде `"sale"/"rent"/"service"` + `LAND_CATEGORY_ID` для фильтра "Земли". Подробности —
`audit.md`, пункт 4. Изменения пока не закоммичены.

### 2. Мёртвый функционал «забыли пароль» / смена пароля
`forgotPassword`, `resetPassword`, `changePassword` реализованы в `AgroMarketApi`/`AgroRepository`,
но не вызываются из UI:
- На `LoginScreen` нет «Забыли пароль?».
- В `ProfileScreen` нет смены пароля.

Решить: добавить экраны/действия, либо явно зафиксировать как backlog (чтобы не путать на
следующем аудите).

## Мелкие (на усмотрение, низкий приоритет)
- `CryptoManagerTest` (`app/src/androidTest`, 5 тестов: round-trip, decrypt на мусоре/пустой
  строке/после ротации ключа) добавлен 2026-06-11, но ещё ни разу не запускался — нужен
  эмулятор/устройство: `./gradlew connectedDebugAndroidTest`. Если упадёт — разобраться, баг в
  тесте или в `CryptoManager`.
- `runBlocking` в `AuthInterceptor`/`TokenAuthenticator` блокирует OkHttp-поток — кандидат на
  `@Volatile`-кэш токена из `Flow`, если профилирование покажет проблему.
- `FileUtils.uriToFile` без сжатия/ресайза фото (отложено до пасса с `ExifInterface`).
- `CreateAdScreen.kt` — 441 строка, кандидат на разбиение по шагам.

## Закрыто 2026-06-11
Тесты на `TokenAuthenticator`/`CryptoManager`/`TokenManager` — `TokenAuthenticatorTest` (7),
`TokenManagerTest` (5, Robolectric), `CryptoManagerTest` (5, androidTest). `TokenManager` теперь
получает `CryptoManager` через конструктор (DI). Подробности — `audit.md`, пункт 1.
