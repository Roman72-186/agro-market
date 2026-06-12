# Аудит кода AgroMarket

Дата: 2026-06-11 (обновление аудита от 2026-06-09 после коммитов `a9eebfa`…`d3db044`)
Объём: весь модуль `:app` (Kotlin · Jetpack Compose · Hilt · Retrofit/OkHttp · DataStore).

Архитектура чистая: `data / di / ui`, репозиторий с `ApiResult`, Hilt, ViewModel в файле экрана.
С прошлого аудита закрыты все критичные и заметные пункты, плюс выполнена дизайн-модернизация
(см. [plans/2026-06-10-design-modernization.md](plans/2026-06-10-design-modernization.md)).
Ниже — что осталось открытым и новые наблюдения.

---

## ✅ Закрыто с прошлого аудита (2026-06-09)

| Было | Фикс |
|---|---|
| ProGuard не покрывал `AdMyListResponse` (release-краш «Моих объявлений») | `proguard-rules.pro`: `-keep class ru.agromarket.data.** { *; }` + правила для Hilt/Retrofit/Gson |
| Двойной POST `submitAd` при ошибке | `when (val submitResult = repository.submitAd(adId))` — один вызов, результат сохранён в `val` |
| Поиск дёргал API на каждой букве | `snapshotFlow { searchQuery }` + `debounce(400ms)` в `FeedScreen.kt` |
| `isFavorite` всегда стартовал с `false` | при открытии сверяется с `getFavorites()`, optimistic toggle с откатом при ошибке |
| Категория угадывалась по подстроке (`APP_CATEGORIES`) | `CreateAdScreen` строит дерево из `getCategories()`, локальный хардкод-список удалён |
| Район/населённый пункт не выбирались | `getDistricts`/`getLocalities` подключены в шаге формы мастера |
| `allowBackup="true"` | `false` |
| Токены в DataStore без шифрования | новый `CryptoManager` (AES-256/GCM, ключ в AndroidKeystore), `TokenManager` хранит только шифротекст, `decrypt()` возвращает `null` для legacy/повреждённых данных |
| Нет авто-refresh при 401 | новый `TokenAuthenticator`: refresh через `auth/refresh`, синхронизация конкурентных 401 (`synchronized` + повторная проверка токена), лимит попыток, очистка токенов и логаут при невалидном refresh |
| `provideTokenManager` в `AppModule` | убран — Hilt создаёт `TokenManager` сам (`@Singleton @Inject constructor`) |
| `response.body()!!` мог упасть на пустом теле | `safeCall` теперь возвращает `ApiResult.Error("Пустой ответ сервера", ...)` при `body() == null` |
| `Icons.Default.ArrowBack` (deprecated) | заменено на `Icons.AutoMirrored.Filled.ArrowBack` |
| Дизайн: дефолтный Material3-шаблон, эмодзи-иконки, разнобой шапок/карточек | фазы 0–8 завершены: палитра «Глина и Олива», `AdCard`/`StatusBadge`/`EmptyState`/`AppTopBar`/`AuthHero`/`ErrorBanner`/`CategoryIcon`, dark theme, брендинг иконки/сплэша, анимации навигации, `./gradlew lint` чист |

---

## 🟠 Осталось открытым

### 1. ~~Refresh-логика и шифрование токенов не покрыты тестами~~ — закрыто 2026-06-11
Добавлены `TokenAuthenticatorTest` (7 тестов: успешный/неудачный refresh, исключение из
`api.refreshToken`, отсутствие refresh-токена, дедупликация конкурентных 401, лимит попыток,
исключение `/auth/`) и `TokenManagerTest` (5 тестов: round-trip, Flow при пустом хранилище,
`isLoggedIn`, `decrypt() == null`, `clear()`).

`CryptoManager` использует реальный `AndroidKeyStore`-ключ (AES/GCM), который Robolectric не
может полноценно эмулировать — спайк `KeyGenerator.getInstance(AES, "AndroidKeyStore")` падает с
`NoSuchAlgorithmException`. Поэтому:
- `TokenManager` теперь получает `CryptoManager` через конструктор (DI, см. пункт ниже в
  «Мелкие») — в `TokenManagerTest` он замокан через MockK, DataStore работает на реальном
  Robolectric `Context` (`app/src/test`).
- `CryptoManagerTest` (5 тестов: round-trip, decrypt на мусорном/пустом/слишком коротком Base64,
  decrypt после ротации ключа) лежит в `app/src/androidTest`. **Запущен 2026-06-11** на эмуляторе
  `Medium_Phone_API_36.1` через `./gradlew connectedDebugAndroidTest` — все 5 тестов (плюс
  `AppContextTest`, итого 6) `PASSED`.
- В `app/build.gradle.kts` добавлены `org.robolectric:robolectric:4.13` и `androidx.test:core:1.5.0`
  (testImplementation) + `testOptions.unitTests.isIncludeAndroidResources/isReturnDefaultValues`.

### 2. ~~Мёртвый функционал в UI~~ — закрыто 2026-06-11
Добавлены `ForgotPasswordScreen` (2 шага: email → код + новый пароль, как в `RegisterScreen`) и
`ChangePasswordScreen` в профиле (текущий/новый/подтверждение пароля). Подключены в
`Navigation.kt` (`Screen.ForgotPassword`, `Screen.ChangePassword`), точки входа — «Забыли
пароль?» на `LoginScreen` и пункт «Сменить пароль» в `ProfileScreen`.

По пути сверено с `https://agro.assaru.space/openapi.json` и поправлено несоответствие полей:
- `ResetPasswordRequest.password` → `newPassword` (`@SerializedName("new_password")`) — бэкенд
  ждал `new_password`, поле было не замаплено и `resetPassword` вообще отсутствовал в
  `AgroRepository` (добавлен).
- `ChangePasswordRequest.oldPassword` → `currentPassword` (`@SerializedName("current_password")`)
  — бэкенд ждёт `current_password`, не `old_password`.

Валидация нового пароля в новых экранах — минимум 8 символов (бэкенд: 8–128), вынесена в общие
константы `MIN_PASSWORD_LENGTH`/`PASSWORD_LENGTH_ERROR` (`ui/auth/PasswordRules.kt`) и
переиспользуется в `RegisterScreen`/`ForgotPasswordScreen`/`ChangePasswordScreen` —
несогласованность «6 vs 8 символов» закрыта 2026-06-11.

**Живой прогон 2026-06-11** (эмулятор `Medium_Phone_API_36.1`, debug APK):
- `ForgotPasswordScreen`, шаг EMAIL: ввод email → «Получить код» → бэкенд вернул сообщение
  «Если email зарегистрирован, код отправлен» → переход на шаг RESET — работает корректно.
- `ForgotPasswordScreen`, шаг RESET: проверена клиентская валидация — код короче 6 символов →
  «Введите код из письма»; пароль короче 8 символов → «Пароль минимум 8 символов» (новая
  проверка из задачи 1) — отображается корректно.
- Полный успешный сброс пароля (с реальным кодом из письма) и `ChangePasswordScreen` живьём
  **не проверены**: для `ChangePasswordScreen` нужен вход в существующий аккаунт, а регистрация
  нового аккаунта требует кода подтверждения из реального письма — недоступно в этой сессии.
  Обработка 401 при смене пароля (см. ниже) тоже остаётся непроверенной вживую.

**Не проверено**: при смене пароля бэкенд может инвалидировать текущий access-токен — если так,
`TokenAuthenticator` подхватит 401 и разлогинит штатным образом (см. `MainNavigation`), отдельной
обработки в `ChangePasswordScreen` нет.

### 3. ~~`LandsScreen` не подключён~~ — закрыто 2026-06-11
Экран реализован (список объявлений категории "Земельные участки" через
`LandsViewModel`/`AgroRepository.getFeed(categoryId = LAND_CATEGORY_ID)`, карточки `LandCard` в
стиле референса с фото, регионом и ценой) и зарегистрирован в `Navigation.kt` как
`Screen.Lands`. Точка входа — иконка "Земли" в `AppTopBar` на `FeedScreen`.

### 4. ~~Несовпадение значений `type` между приложением и `GET /feed` бэкенда~~ — закрыто 2026-06-11
Сверено с актуальной OpenAPI-схемой бэкенда (`https://agro.assaru.space/openapi.json`):
`AdType = "sale" | "rent" | "service"` — единый enum и для `AdCreateRequest.type`, и для фильтра
`GET /ads/`. Значения `"sell"` и `"land"` в бэкенде не существуют вовсе — `"land"` никогда не было
валидным `type`, это категория ("Земельные участки", `category_id = 56`, подтверждено через
`GET /categories/`).

Бага в бэкенде нет — он самосогласован. Поправлено приложение:
- `FeedScreen` фильтр "Продажа" → `type = "sale"` (было `"sell"`).
- "Земли" — фильтр по `categoryId = LAND_CATEGORY_ID` (было `type = "land"`).
- `CreateAdScreen`: шаг "Земли СХ назначения" маппится на `type = "sale"` +
  категория "Земельные участки" (`selectType("land")` → `type = "sale"`, авто-выбор категории 56).
- `StatusBadge.adTypeBadge` и `LandsScreen`/`LandCard` используют `"sale"/"rent"/"service"`.

`GET /ads/?type=sale` и `GET /ads/?category_id=56` отдают `200 {"items": [], "total": 0, ...}`
(БД пуста на момент проверки) — запросы больше не возвращают 400. Не проверено вживую с реальными
объявлениями (нет данных в БД), только что запросы валидны.

---

## 🟡 Мелкие/на усмотрение

- **`runBlocking` в `AuthInterceptor` и `TokenAuthenticator`** — оставлено сознательно
  (с комментариями в коде про синхронизацию конкурентных refresh), но по-прежнему блокирует
  OkHttp-поток на каждый запрос и на каждый refresh. Если профилирование покажет проблему —
  кандидат на `@Volatile`-кэш токена, обновляемый из `Flow`.
  **Перепроверено 2026-06-11**: новых сигналов (профилирование, жалобы на задержки) нет —
  решение остаётся оправданным, код не менялся.
- ~~**`TokenManager` создаёт `CryptoManager()` напрямую**~~ — закрыто 2026-06-11:
  `CryptoManager` теперь приходит через конструктор (`@Inject`), Hilt связывает автоматически
  (оба класса `@Singleton @Inject constructor`, без отдельного модуля). Это и сделало
  `TokenManagerTest` возможным — `CryptoManager` в тесте замокан через MockK.
- ~~**`FileUtils.uriToFile`**~~ — закрыто 2026-06-11: добавлены `ExifInterface`-поворот по
  EXIF-ориентации и ресайз/сжатие (downscale до 1600px по большей стороне + JPEG quality 85),
  существующая логика очистки `cacheDir` (`upload_*` старше часа) сохранена. Файлы теперь всегда
  сохраняются как `.jpg` — безопасно, т.к. `uploadPhotos`/`uploadAvatar` используют
  `"image/*".toMediaTypeOrNull()`.
- ~~**`CreateAdScreen.kt` — 441 строка**~~ — закрыто 2026-06-11: разбит на степ-енам `CreateStep`
  + отдельные composable на шаг (`CreateAdTypeStep`, `CreateAdCategoryStep`,
  `CreateAdSubCategoryStep`, `CreateAdFormStep`), `CreateAdViewModel` и общий диалог выбора
  остались в `CreateAdScreen.kt` (~280 строк). Поведение не менялось.
- **`QUICK_CATEGORIES` в `FeedScreen.kt`** — отдельный curated-список для быстрых фильтров на
  главном экране; в отличие от прежнего `APP_CATEGORIES` не дублирует логику выбора категории при
  создании объявления (та теперь полностью на серверных данных), так что дублирования смысла
  больше нет — но стоит иметь в виду при изменении набора категорий на сервере.

---

## Что делать в первую очередь
1. ~~Тесты на `TokenAuthenticator`/`CryptoManager`/`TokenManager` (#1)~~ — закрыто 2026-06-11.
2. ~~Несовпадение `type` между приложением и `GET /feed` (#4)~~ — закрыто 2026-06-11.
3. ~~«Забыли пароль» / смена пароля (#2)~~ — закрыто 2026-06-11.
4. Все 6 пунктов из «Мелкие/на усмотрение» (`next-session-prompt.md`) закрыты 2026-06-11.
5. Остаётся непроверенным вживую: успешный сброс пароля по реальному коду из письма и
   `ChangePasswordScreen` (нужен вход в существующий аккаунт) — см. пункт 2 выше.
