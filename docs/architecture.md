# Архитектура AgroMarket

> On-demand справочник. Читать, когда задача касается структуры проекта, навигации, авторизации или тестов.

## Структура папок

```
data/
  api/        AgroMarketApi (Retrofit-интерфейс, все эндпоинты), AuthInterceptor, TokenManager
  model/      Models.kt — все DTO (request/response) с @SerializedName
  repository/ AgroRepository + sealed ApiResult<Success|Error>
di/           AppModule — Hilt-провайдеры OkHttp/Retrofit/Api/TokenManager (SingletonComponent)
ui/<feature>/ экран + его ViewModel в одном файле (auth, feed, ad, create, favorites, profile, navigation, theme)
ui/components/ переиспользуемые composables (AdCard, AppTopBar, AuthHero, CategoryIcon, EmptyState, ErrorBanner, ErrorState, FavoriteRow, LoadingState, StatusBadge)
utils/        FileUtils
```

## Конвенции — детали

- **ViewModel живёт в файле экрана**, а не отдельно. Например `FeedViewModel` объявлен внутри [FeedScreen.kt](../app/src/main/java/ru/agromarket/ui/feed/FeedScreen.kt). Помечен `@HiltViewModel`, состояние держит через `var ... by mutableStateOf(...)` (НЕ StateFlow), экран получает его через `hiltViewModel()`. Новый экран — следуй этому же шаблону.
- **Сеть только через `AgroRepository`.** Каждый метод оборачивает вызов API в `safeCall { ... }` и возвращает `ApiResult<T>`. `safeCall` парсит `errorBody` и достаёт поле `detail` (формат ошибок FastAPI). Не вызывай `AgroMarketApi` напрямую из ViewModel — добавляй suspend-метод в репозиторий.
- **Авторизация.** `AuthInterceptor` подставляет `Authorization: Bearer <token>` во все запросы, КРОМЕ путей, содержащих `/auth/`. Токены читаются из `TokenManager` через `runBlocking` внутри интерсептора. После успешного `login`/`setPassword` репозиторий сам сохраняет токены через `tokenManager.saveTokens(...)`.
- **TokenManager** хранит access/refresh токены в DataStore (`auth_prefs`). `isLoggedIn: Flow<Boolean>` — источник истины для стартового экрана: [MainActivity.kt](../app/src/main/java/ru/agromarket/MainActivity.kt) собирает его через `collectAsStateWithLifecycle` и передаёт в `MainNavigation`.
- **Навигация** — в [Navigation.kt](../app/src/main/java/ru/agromarket/ui/navigation/Navigation.kt). Маршруты в `sealed class Screen`, нижний бар — `BottomNavItem` (Каталог/Избранное/Разместить). Стартовый экран зависит от `isLoggedIn`. Новый экран = новый объект в `Screen` + `composable(...)` в `NavHost`.
- **Refresh-токен (`auth/refresh`) обновляется автоматически при 401** через `TokenAuthenticator`: синхронизация конкурентных 401 (`synchronized` + повторная проверка токена), лимит попыток, при невалидном refresh — очистка токенов и логаут.
- **Загрузка файлов** (фото объявлений, аватар) — multipart, формируется в репозитории (`asRequestBody("image/*")` + `MultipartBody.Part.createFormData`). Имя part: `files` для фото, `file` для аватара.

## Тесты

- Unit (JVM, без Android): JUnit4 + MockK + `kotlinx-coroutines-test`. Образец — [AgroRepositoryTest.kt](../app/src/test/java/ru/agromarket/data/repository/AgroRepositoryTest.kt): мокаем `AgroMarketApi` и `TokenManager`, гоняем suspend через `runTest`, проверяем маппинг `ApiResult` и сохранение токенов. Сериализацию DTO проверяет [ModelsSerializationTest.kt](../app/src/test/java/ru/agromarket/data/model/ModelsSerializationTest.kt).
- Instrumented: [AppContextTest.kt](../app/src/androidTest/java/ru/agromarket/AppContextTest.kt), runner — `androidx.test.runner.AndroidJUnitRunner`.
