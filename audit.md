# Аудит кода AgroMarket

Дата: 2026-06-09
Объём: весь модуль `:app` (Kotlin · Jetpack Compose · Hilt · Retrofit/OkHttp · DataStore).

Архитектура чистая: `data / di / ui`, репозиторий с `ApiResult`, Hilt, ViewModel в файле экрана.
База здоровая. Проблемы — в деталях ниже, по убыванию важности.

---

## 🔴 Критичное (ломает работу)

### 1. ProGuard не покрывает `AdMyListResponse` → «Мои объявления» сломаются в release
Класс объявлен в `app/src/main/java/ru/agromarket/data/api/AgroMarketApi.kt:145` в пакете
`ru.agromarket.data.api`, а правило хранит только модели:
```
-keep class ru.agromarket.data.model.** { *; }
```
В release включён `isMinifyEnabled = true`. Поля без `@SerializedName` (`id`, `type`, `title`,
`status`) будут переименованы R8 → Gson вернёт их `null` → краш/пустой экран профиля.
В debug не воспроизводится, поэтому легко уехать в прод.

**Фикс:** расширить правило до `ru.agromarket.data.**` (или перенести класс в `data.model`).
Заодно добавить стандартные keep-правила для Hilt/Retrofit — текущий `proguard-rules.pro` их не
содержит вообще.

### 2. Двойная отправка объявления на модерацию
`app/src/main/java/ru/agromarket/ui/create/CreateAdScreen.kt:119-122`
```kotlin
when (repository.submitAd(adId)) {
    is ApiResult.Success -> onSuccess()
    is ApiResult.Error -> error = (repository.submitAd(adId) as? ApiResult.Error)?.message
}
```
`submitAd` вызывается для `when`, а в ветке ошибки — ещё раз. Два POST-запроса на submit.
Надо сохранить результат в `val`:
```kotlin
when (val res = repository.submitAd(adId)) {
    is ApiResult.Success -> onSuccess()
    is ApiResult.Error -> error = res.message
}
```

---

## 🟠 Заметные баги

### 3. Поиск дёргает API на каждой букве
`app/src/main/java/ru/agromarket/ui/feed/FeedScreen.kt:112`
`onValueChange = { viewModel.search(it) }` → каждый символ запускает полный сетевой
`loadFeed(refresh)`. Нет debounce, мигание списка, нагрузка на сервер.
Нужно отделить ввод текста от запроса (debounce ~400 мс через `snapshotFlow`/`LaunchedEffect`),
а не звать `search()` в `onValueChange`.

### 4. `isFavorite` всегда стартует с `false`
`app/src/main/java/ru/agromarket/ui/ad/AdDetailScreen.kt:40,54`
При открытии уже добавленного в избранное объявления показывается пустое сердце. Нажатие
повторно шлёт `addFavorite`. Нет синхронизации с реальным состоянием (ни поля в
`AdDetailResponse`, ни сверки с `getFavorites()`).

### 5. Категория угадывается по подстроке, иначе — первая попавшаяся
`app/src/main/java/ru/agromarket/ui/create/CreateAdScreen.kt:95-96`
```kotlin
selectedCategoryId = allCats.firstOrNull { it.name.contains(sub, ignoreCase = true) }?.id
    ?: allCats.firstOrNull()?.id
```
Локальные хардкод-списки (`APP_CATEGORIES`) не связаны с серверными категориями. Если совпадения
нет — объявление молча уходит в произвольную категорию. Нужно строить шаги категорий из ответа
`getCategories()`, а не из захардкоженных списков.

### 6. Район и населённый пункт нигде не выбираются
В `AdCreateRequest` есть `districtId`/`localityId`, эндпоинты `getDistricts/getLocalities/searchLocality`
реализованы, но UI собирает только регион. Фича недоделана.

---

## 🟡 Безопасность / конфиг

### 7. `allowBackup="true"`
`app/src/main/AndroidManifest.xml:12`
Auth-токены лежат в DataStore (`auth_prefs`) и попадают в Android auto-backup → их можно вытащить
из бэкапа. Для маркетплейса с учётками поставить `allowBackup="false"` либо исключить `auth_prefs`
через `fullBackupContent`/`dataExtractionRules`.

### 8. Токены хранятся в открытом виде
Обычный `DataStore Preferences`, без шифрования. Желательно EncryptedSharedPreferences /
шифрованный слой.

### 9. Нет авто-refresh при 401
(Отмечено и в `CLAUDE.md`.) `auth/refresh` есть в API, но `AuthInterceptor` токен не перевыпускает
и не разлогинивает. Истёкшая сессия = поток ошибок без выхода на логин. Самый крупный
архитектурный пробел в auth.

---

## 🟢 Качество / починить заодно

- **`runBlocking` в интерсепторе** (`AuthInterceptor.kt:21`) — блокирует OkHttp-поток и читает весь
  DataStore на каждый запрос. Лучше держать токен в `@Volatile`-кэше, обновляемом из flow.
- **`uriToFile`** (`utils/FileUtils.kt`) — копирует фото в кэш без сжатия, всегда имя `.jpg`, без
  валидации размера/типа, кэш не чистится. Большие фото = тяжёлые загрузки и распухание `cacheDir`.
- **Тихое проглатывание ошибок** в `ProfileViewModel.load()` (`ProfileScreen.kt:61,65`) — пустые
  `is ApiResult.Error -> {}`. Профиль не загрузился → пустой экран без сообщения.
- **Мёртвый/недоступный функционал:** `forgotPassword`, `resetPassword`, `changePassword`,
  `refreshToken`, `getSubcategories`, `getLocalities` реализованы в API/репозитории, но не
  вызываются из UI. «Забыли пароль?» с экрана логина недоступен.
- **Дублирование категорий:** `QUICK_CATEGORIES` (Feed) и `APP_CATEGORIES` (Create) — два
  расходящихся хардкод-списка.
- **`response.body()!!`** (`AgroRepository.kt:107`) — упадёт на пустом теле (например 204), ошибка
  превратится в маскирующее «Ошибка соединения».
- **`provideTokenManager`** (`di/AppModule.kt:24`) лишний — `TokenManager` уже
  `@Singleton @Inject constructor`, Hilt создаст сам.
- **`Icons.Default.ArrowBack`** — deprecated, нужен `Icons.AutoMirrored.Filled.ArrowBack`
  (RTL + lint).
- **Нет тестов и нет Gradle wrapper** — отмечено в `CLAUDE.md`; для CI/воспроизводимости стоит
  добавить.

---

## Что чинить в первую очередь
1. ProGuard (#1) и двойной submit (#2) — иначе release-баги.
2. Debounce поиска (#3) — бьёт по серверу прямо сейчас.
3. `allowBackup` / refresh-токен (#7, #9) — безопасность сессий.
