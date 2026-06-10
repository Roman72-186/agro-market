# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> Язык общения — русский, на «ты». Код, имена и комментарии в коде — на английском (UI-строки на русском).

## Что это

AgroMarket — нативное Android-приложение (доска объявлений для агро-рынка: техника, запчасти, семена, удобрения, корма, животные, земля). Клиент к REST API `https://agroprompis.tw1.ru/api/v1/` (значение `API_BASE_URL` в [app/build.gradle.kts](app/build.gradle.kts), переопределяется через `buildConfigField`).

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

### Тесты

- Unit (JVM, без Android): JUnit4 + MockK + `kotlinx-coroutines-test`. Образец — [AgroRepositoryTest.kt](app/src/test/java/ru/agromarket/data/repository/AgroRepositoryTest.kt): мокаем `AgroMarketApi` и `TokenManager`, гоняем suspend через `runTest`, проверяем маппинг `ApiResult` и сохранение токенов. Сериализацию DTO проверяет [ModelsSerializationTest.kt](app/src/test/java/ru/agromarket/data/model/ModelsSerializationTest.kt).
- Instrumented: [AppContextTest.kt](app/src/androidTest/java/ru/agromarket/AppContextTest.kt), runner — `androidx.test.runner.AndroidJUnitRunner`.

## Архитектура

Один модуль `:app`, package `ru.agromarket`. Чистое разделение data / di / ui:

```
data/
  api/        AgroMarketApi (Retrofit-интерфейс, все эндпоинты), AuthInterceptor, TokenManager
  model/      Models.kt — все DTO (request/response) с @SerializedName
  repository/ AgroRepository + sealed ApiResult<Success|Error>
di/           AppModule — Hilt-провайдеры OkHttp/Retrofit/Api/TokenManager (SingletonComponent)
ui/<feature>/ экран + его ViewModel в одном файле (auth, feed, ad, create, favorites, profile, lands, navigation, theme)
utils/        FileUtils
```

Ключевые соглашения, которые надо знать прежде чем писать код:

- **ViewModel живёт в файле экрана**, а не отдельно. Например `FeedViewModel` объявлен внутри [FeedScreen.kt](app/src/main/java/ru/agromarket/ui/feed/FeedScreen.kt). Помечен `@HiltViewModel`, состояние держит через `var ... by mutableStateOf(...)` (НЕ StateFlow), экран получает его через `hiltViewModel()`. Новый экран — следуй этому же шаблону.
- **Сеть только через `AgroRepository`.** Каждый метод оборачивает вызов API в `safeCall { ... }` и возвращает `ApiResult<T>`. `safeCall` парсит `errorBody` и достаёт поле `detail` (формат ошибок FastAPI). Не вызывай `AgroMarketApi` напрямую из ViewModel — добавляй suspend-метод в репозиторий.
- **Авторизация.** `AuthInterceptor` подставляет `Authorization: Bearer <token>` во все запросы, КРОМЕ путей, содержащих `/auth/`. Токены читаются из `TokenManager` через `runBlocking` внутри интерсептора. После успешного `login`/`setPassword` репозиторий сам сохраняет токены через `tokenManager.saveTokens(...)`.
- **TokenManager** хранит access/refresh токены в DataStore (`auth_prefs`). `isLoggedIn: Flow<Boolean>` — источник истины для стартового экрана: [MainActivity.kt](app/src/main/java/ru/agromarket/MainActivity.kt) собирает его через `collectAsStateWithLifecycle` и передаёт в `MainNavigation`.
- **Навигация** — в [Navigation.kt](app/src/main/java/ru/agromarket/ui/navigation/Navigation.kt). Маршруты в `sealed class Screen`, нижний бар — `BottomNavItem` (Каталог/Избранное/Разместить). Стартовый экран зависит от `isLoggedIn`. Новый экран = новый объект в `Screen` + `composable(...)` в `NavHost`.
- **Refresh-токен описан в API (`auth/refresh`), но автоматического обновления при 401 пока нет** — интерсептор не перевыпускает токен. Учитывай при работе с истёкшими сессиями.
- **Загрузка файлов** (фото объявлений, аватар) — multipart, формируется в репозитории (`asRequestBody("image/*")` + `MultipartBody.Part.createFormData`). Имя part: `files` для фото, `file` для аватара.

## Замечания

- `LandsScreen` существует, но НЕ подключён в `Navigation.kt` — это незавершённый/неактивный экран.
- `usesCleartextTraffic="false"` — API только по HTTPS.
- Логирование HTTP-тел (`HttpLoggingInterceptor.Level.BODY`) включается только в debug-сборке.
