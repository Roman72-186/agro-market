# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Язык общения — русский, на «ты». Код, имена и комментарии в коде — на английском (UI-строки на русском).

## Что это

AgroMarket — нативное Android-приложение (доска объявлений для агро-рынка: техника, запчасти, семена, удобрения, корма, животные, земля). Клиент к REST API `https://agro.assaru.space/api/v1/` (значение `API_BASE_URL` в [app/build.gradle.kts](app/build.gradle.kts), переопределяется через `buildConfigField`).

**Репозиторий:** [github.com/Roman72-186/agro-market](https://github.com/Roman72-186/agro-market) — единственный источник правды, локальный код/git/VPS должны быть синхронизированы с ним.

Стек: Kotlin 1.9.22 · Jetpack Compose (BOM 2024.02) + Material3 · Hilt 2.50 (DI) · Retrofit 2.9 + OkHttp 4.12 + Gson · DataStore Preferences (токены) · Coil (картинки) · Navigation Compose · Accompanist.
`minSdk 26`, `targetSdk/compileSdk 34`, JDK 17, AGP 8.2.2.

## Команды

Сборка — через gradle wrapper (Gradle 8.5). Нужен JDK 17; если `JAVA_HOME` не задан, можно указать JBR из Android Studio: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`.

- `./gradlew assembleDebug` — debug APK; `installDebug` — поставить на устройство.
- `./gradlew testDebugUnitTest` — JVM unit-тесты (`app/src/test`).
- `./gradlew :app:testDebugUnitTest --tests "ru.agromarket.data.repository.AgroRepositoryTest"` — один тест-класс.
- `./gradlew connectedDebugAndroidTest` — инструментальные тесты (`app/src/androidTest`, нужен эмулятор/устройство).
- `./gradlew lint` — Android Lint.

`local.properties` (не в VCS) должен содержать `sdk.dir` с путём к Android SDK. На Windows используй `gradlew.bat`.

## Конвенции

Один модуль `:app`, package `ru.agromarket`, разделение `data / di / ui`. Жёсткие правила — нарушение ломает паттерн проекта (детали и полная структура папок — [docs/architecture.md](docs/architecture.md)):

- **ViewModel живёт в файле экрана** (`@HiltViewModel`, состояние через `var ... by mutableStateOf(...)`, НЕ StateFlow), получается через `hiltViewModel()`.
- **Сеть только через `AgroRepository`.** Каждый метод — `safeCall { ... }` → `ApiResult<T>`. Не вызывай `AgroMarketApi` напрямую из ViewModel.
- **Авторизация:** `AuthInterceptor` подставляет `Authorization: Bearer <token>` всем запросам, кроме `/auth/`. `TokenManager` хранит access/refresh в DataStore (`auth_prefs`, шифрование через `CryptoManager`).
- **Загрузка файлов** — multipart из репозитория: part `files` для фото, `file` для аватара.

## Замечания

- `usesCleartextTraffic="false"` — API только по HTTPS.
- Логирование HTTP-тел (`HttpLoggingInterceptor.Level.BODY`) включается только в debug-сборке.
- [archive/](archive/) — мёртвый код вне `app/src/` (не компилируется, не часть сборки); см. [archive/README.md](archive/README.md) перед тем как что-то оттуда возвращать.

## Где что искать

| Тема | Файл |
|---|---|
| Полная структура папок, детали конвенций (Navigation, AuthInterceptor flow, тесты) | [docs/architecture.md](docs/architecture.md) |
| Статус проекта, открытые пункты аудита | [memory/project-state.md](memory/project-state.md) |

Читать соответствующий файл, когда задача его касается.

## Память

Факты проекта — в `memory/` (один факт = один файл, индекс — [memory/MEMORY.md](memory/MEMORY.md)). Перед сохранением нового факта проверять, нет ли уже файла на эту тему — обновлять, не дублировать. Не сохранять то, что выводится из кода/гита/`audit.md`.
