# Session Handoff

**Updated:** 2026-06-30 +0500
**Agent:** Claude Code
**Workspace:** C:\Users\User\Desktop\Project
**Active project:** C:\Users\User\Desktop\Project\agromarket-android.tar_1\agromarket-android

> Предыдущая задача (монетизация: boost/ЮKassa + Pro-мок) ЗАВЕРШЕНА и зафиксирована в
> `memory/monetization-direction.md`, `memory/backend-monetization-reality.md`. Её детали см. там.
> Текущая активная задача — **миграция приложения на iOS через Compose Multiplatform**.

## Задача

Подготовить AgroMarket к iOS. Стратегия выбрана владельцем: **Compose Multiplatform (CMP)** —
один Kotlin-кодовый базис переиспользует и бизнес-логику, и UI (приложение уже 100% на Compose).
Android должен оставаться рабочим на каждом шаге; iOS добавляется как target.

**Полный план по фазам:** `C:\Users\User\.claude\plans\cryptic-prancing-peacock.md` —
ЧИТАТЬ ЦЕЛИКОМ перед продолжением. Там вход/выход/команда проверки для каждой фазы.

## Как организована работа (важно соблюдать)

- **Каждую фазу выполняет ОТДЕЛЬНЫЙ агент в изолированном git-worktree** (`isolation: worktree`),
  чтобы рабочая ветка Android не ломалась. Тип агента — `general-purpose`.
- **Владелец ревьюит результат каждой фазы ПЕРЕД запуском следующей.** Не запускать следующую
  фазу автоматически — дождаться, пока человек подтвердит зелёный гейт и вольёт изменения.
- Версии библиотек на каждом шаге сверять через **Context7** (resolve-library-id → query-docs),
  не из памяти: Kotlin ↔ Compose Multiplatform ↔ AGP ↔ Ktor/Koin/Coil быстро меняются.
- Промпт агенту делать самодостаточным (он «холодный», без контекста): дать ссылку на план,
  путь к проекту (`agromarket-android/` — Gradle-проект), среду (Windows, `gradlew.bat`, JDK 17 /
  JBR `C:\Program Files\Android\Android Studio\jbr`), и явный Definition of Done с командой проверки.

## Ключевой инвариант среды

- Разработка на **Windows**. Kotlin/Native компилирует iOS-таргет ТОЛЬКО на macOS.
- **Фазы 0–2 делаются и проверяются на Windows** через Android-сборку
  (`gradlew.bat assembleDebug` + `testDebugUnitTest`) — общий код `commonMain` верифицируется
  Android-компиляцией. iOS-таргеты на Windows НЕ собирать (упадут — это ожидаемо).
- **Фазы 3+ требуют macOS + Xcode.** Дефолт: GitHub Actions macOS-раннеры для сборки; облачный
  Mac на время отладки. Локальная Windows-разработка кода продолжается.

## Фазы (статус)

| Фаза | Что | Среда | Статус |
|---|---|---|---|
| 0 | Каркас CMP + bump Kotlin 1.9→2.0, KMP+Compose MP плагины, source sets | Windows | **ГОТОВА** (закоммичено `d6a1657`, гейт зелёный) |
| 1a | Gson → kotlinx.serialization | Windows | **ГОТОВА** (`4777d6a`, гейт зелёный) |
| 1b | Retrofit/OkHttp → Ktor | Windows | **ГОТОВА** (`50942ff`, гейт зелёный, на ревью владельца) |
| 1c | Hilt → Koin | Windows | **СЛЕДУЮЩАЯ** |
| 1d | Coil 2→3, выпил Accompanist | Windows | не начата |
| 1e | DataStore → multiplatform | Windows | не начата |
| 2 | expect/actual платформенного слоя (Android actual) | Windows | не начата |
| 3 | iOS-приложение: Xcode-проект + iosMain actual + запуск | macOS | не начата |
| 4 | iOS-интеграции: APNs/push, deep links, оплата | macOS | не начата |
| 5 | Релиз: CI macOS, подпись, TestFlight | macOS/CI | не начата |
| 6 | Монетизация iOS через StoreKit IAP (поздняя) | macOS | не начата |

## Current State

- **Фаза 1b ГОТОВА и закоммичена (`50942ff`). HEAD зелёный** — гейт прогнан независимо:
  `compileCommonMainKotlinMetadata` + `assembleDebug` + `testDebugUnitTest` BUILD SUCCESSFUL,
  35 тестов (XML: 0 failures/errors; `AuthRefreshTest`=5, `AgroRepositoryTest`=4). **Ждёт ревью
  владельца `git diff 8ec19ec..50942ff` перед запуском Фазы 1c.**
- **Фаза 0 ГОТОВА и закоммичена** (`d6a1657`). Стек: Kotlin 2.0.21, Compose MP 1.7.3,
  AGP 8.2.2 (не повышать — с AGP 9.0 связка KMP + `com.android.application` ломается),
  **Ktor 3.0.3** (последняя 3.0.x на Kotlin 2.0.x; 3.1+ требует Kotlin 2.1), coroutines 1.9.0.
- **Git-базлайн наведён двумя коммитами** (репо `agromarket-android` — отдельный, НЕ запушен):
  - `c659fa5` — незакоммиченная работа прошлых сессий (монетизация, доки, FCM).
  - `d6a1657` — Фаза 0 (KMP + Compose MP каркас).
- **Секреты под .gitignore:** `google-services.json`, `*-adminsdk-*.json`, `delivery/`,
  `tools/`, `docs/*.pdf`, `__pycache__`. НИКОГДА не коммитить Admin SDK ключ.
- **Бэкап рабочего дерева (до базлайна):** в scratchpad сессии,
  `agromarket-backup-prePhase1a-*.tar.gz` (507 МБ, incl untracked).
- Изоляция worktree для агента НЕ сработала (вложенный git-репо) — агент Фазы 0 писал прямо
  в реальный проект. **Для следующих фаз:** либо отдельная git-ветка под фазу, либо агент
  пишет в основной репо, а ревью идёт по `git diff` коммита фазы (текущий рабочий вариант).

## Открытые решения владельца (заложены дефолтами, не блокируют фазы 0–2)

- **Mac-окружение** — НЕ определено. Нужно к Фазе 3 (минимум — CI macOS-раннер).
- **Apple In-App Purchase** — Apple (Guideline 3.1.1) почти наверняка потребует Boost/Pro через
  StoreKit (комиссия 30%), а не ЮKassa. **Дефолт: в первом релизе iOS платные функции скрыты**;
  монетизация iOS — поздняя Фаза 6.
- **Apple Developer Program ($99/год)** — нужен к фазам 4–5 (push/публикация). Оформить заранее.

## Next Steps

1. **Владельцу: отревьюить `git diff 8ec19ec..50942ff` (Фаза 1b).** Точки внимания, которые
   флагнул агент (см. «Заметки по фазам → Фаза 1b»): кэш токенов Ktor Auth + `clearTokenCache`;
   `expectSuccess`+Auth-retry порядок; дрейф семантики пустого/малформленного тела; параметр
   `baseUrl` в фабрике клиента.
2. **После ревью — запустить Фазу 1c** (Hilt → Koin) отдельным `general-purpose` агентом прямо
   в репо. DoD = `assembleDebug` + `testDebugUnitTest` + `compileCommonMainKotlinMetadata`
   зелёные; DI (`AppModule`, `PaymentModule`) в `commonMain`; ViewModel'и на multiplatform
   `lifecycle` + `koinViewModel()`; **полностью убрать kapt** (Hilt — единственный потребитель);
   шов `PaymentGateway` сохранить (биндинг в Koin-модуле); версии Koin через Context7; отдельный
   коммит фазы. Цикл (ревью → следующая фаза) до Фазы 2.
3. К Фазе 3 — решить вопрос Mac-окружения и Apple Developer Program.

## Заметки по фазам (накопительно)

- **Фаза 1b** (`50942ff`): Ktor **3.0.3** (потолок при Kotlin 2.0.21; 3.1+ требует Kotlin 2.1).
  `AgroMarketApi` теперь класс над `HttpClient` (методы возвращают `HttpResponse`, десериализация
  и ошибки — в `safeCall`, `reified inline`). Сетевой слой + `AgroRepository` (+`ApiResult`/
  `safeCall`/`parseErrorDetail`) + `createHttpClient` + интерфейсы `TokenProvider`/
  `PushTokenProvider` + `MonetizationCatalog` → `commonMain`. В `androidMain` остались actual:
  `AppModule` (Hilt, OkHttp-движок, таймауты 30/30/60), `TokenManagerTokenProvider`,
  `FirebasePushTokenProvider`. `AuthInterceptor`/`TokenAuthenticator` удалены — их забрал Ktor
  `Auth(bearer)` (`loadTokens`/`refreshTokens`, `markAsRefreshTokenRequest` против рекурсии,
  `sendWithoutRequest` исключает `/auth/`, guard «`/auth/` не триггерит рефреш»). Multipart на
  `ByteArray` (`PhotoUpload`), `File→bytes` на call-site (`CreateAdScreen`, `ProfileScreen`).
  Тесты на `MockEngine`: `AgroRepositoryTest` (4), `TokenAuthenticatorTest`→`AuthRefreshTest` (5).
  **4 пункта на ревью владельца:** (1) Ktor Auth кэширует `loadTokens` (вкл. null) — после
  логина/логаута зовём `api.clearTokenCache()` (новый шов, покрыт тестом); (2) `expectSuccess`+
  Auth-retry: ретрай на 401 идёт ДО броска — подтверждено тестом на моках, на проде проверить
  поведение бэкенда; (3) малформленный JSON в 2xx теперь даёт `Error("Пустой ответ сервера")`
  вместо `"Ошибка соединения"` (оба `Error`, для FastAPI недостижимо); (4) в `createHttpClient`
  добавлен параметр `baseUrl` (не был в исходной сигнатуре ТЗ, но нужен).
- **Фаза 1a:** kotlinx-serialization-json `1.7.3`, retrofit2-kotlinx-serialization-converter `1.0.0`
  (Retrofit пока остаётся, конвертер уйдёт в 1b). `ApiJson` (`commonMain/data/ApiJson.kt`) —
  единый `Json { ignoreUnknownKeys; coerceInputValues }`, переиспользовать в Ktor.
  Решение: цена `BigDecimal → Double` (только отображение, целые рубли ≪ 2^53). Латентный риск:
  non-null поля без дефолта бросят `MissingFieldException`, если сервер их не пришлёт (Gson клал
  null) — первый подозреваемый, если в 1b/3 экран перестанет парситься. `encodeDefaults` НЕ включать
  глобально. `CLAUDE.md`/`docs/architecture.md` ещё упоминают Gson — обновить пачкой позже.

## Verification

- Гейт для фаз 0–2: `gradlew.bat assembleDebug` + `gradlew.bat testDebugUnitTest` зелёные;
  ручной прогон сценариев Android (логин → лента → детали → создание объявления с фото →
  избранное → профиль).
- Гейт для Фазы 3+ (на Mac): все ~12 экранов рендерятся/навигируются в iOS-симуляторе.

## Open Risks

- Bump Kotlin 1.9.22 → 2.0 и переход Compose Compiler на отдельный Gradle-плагин — самый
  вероятный источник проблем сборки в Фазе 0.
- App Store IAP (см. открытые решения) — главный риск отклонения; снимается скрытием оплаты в v1.
- iOS невозможно собрать/проверить без Mac — фазы 3+ заблокированы до решения вопроса окружения.
- Текстовый ввод в формах CMP-iOS (мастер CreateAd) — исторически слабое место, проверить рано в Фазе 3.
