---
name: project-state
description: Текущий статус проекта — открытые пункты аудита, статус дизайн-модернизации
metadata:
  type: project
---

Закрыто 2026-06-19 («Ошибка сервера» на входе/регистрации):
- Причина — `asyncpg.exceptions.InternalServerError: (EMAXCONNSESSION) max clients reached in
  session mode - max clients are limited to pool_size: 15`. БД бэкенда (`agro-market`, VPS
  `server-main`) подключена через Supabase **Session Pooler** (порт 5432, лимит 15 одновременных
  клиентов 1:1 с backend-подключениями), а SQLAlchemy-движок (`backend/app/core/database.py`)
  создаёт до 30 подключений (`pool_size=20, max_overflow=10`) **на каждый процесс** — а процессов
  4 (uvicorn `--workers 2` + celery-worker + celery-beat, все импортируют один и тот же модуль).
  Любой всплеск параллельных запросов выбивал лимит — ловилось на `register` (падал на первом же
  `SELECT User`, ещё до отправки email), но реально на любом эндпоинте с обращением к БД.
- Фикс — переход на Supabase **Transaction Pooler** (порт 6543, мультиплексирует клиентов, лимит
  клиентов не привязан 1:1 к backend pool size) + `connect_args={"statement_cache_size": 0}` в
  `create_async_engine` (обязательно для asyncpg под PgBouncer/Supavisor transaction mode, иначе
  `prepared statement ... already exists`). `.env`: `POSTGRES_PORT=6543`. Бэкапы —
  `.env.pre-pooler-fix.bak`, `database.py.pre-pooler-fix.bak`. Передеплоены `backend`,
  `celery-worker`, `celery-beat` (`docker compose build backend && up -d --no-deps ...`).
  Проверено: 20 параллельных запросов (register/login/categories) — без единого 500, без
  `EMAXCONNSESSION`/`prepared statement` в логах. Подробности и ход исследования —
  [plans/2026-06-18-db-connection-pool-fix.md](../plans/2026-06-18-db-connection-pool-fix.md).
- Открыто на будущее: мёртвый `database_url_sync` (`config.py:43`, не используется) и сверка
  лимита backend-подключений Supabase с суммой `pool_size`/`max_overflow` по всем процессам при
  росте нагрузки.

Дизайн-модернизация (фазы 0-8) завершена — палитра «Глина и Олива», новые компоненты (AdCard, StatusBadge, EmptyState, AppTopBar, AuthHero, ErrorBanner, CategoryIcon), dark theme, `./gradlew lint` чист. Подробности — [plans/2026-06-10-design-modernization.md](../plans/2026-06-10-design-modernization.md).

Закрыто 2026-06-15 (доводка лого: splash и шапка каталога):
- Сплэш-экран показывал нечёткую иконку — `windowSplashScreenAnimatedIcon` переиспользовал
  `mipmap/ic_launcher_foreground` (432px у xxxhdpi), который на сплэше отображается крупнее, чем
  на иконке приложения, и масштабировался с потерей качества. Добавлен отдельный
  `app/src/main/res/drawable-nodpi/splash_icon.png` (960×960, контент в той же safe-zone ~65.74%,
  сгенерирован из `logo.png` без апскейла), `values/themes.xml` и `values-night/themes.xml` →
  `windowSplashScreenAnimatedIcon="@drawable/splash_icon"`.
- В шапке каталога (`FeedScreen`) заголовок был `"🌾 АгроМаркет"` — эмодзи-колос заменён на
  реальный логотип: `AppTopBar` получил параметр `showLogo: Boolean = false` (рисует
  `R.drawable.logo` 28dp перед текстом title), `FeedScreen` теперь `title = "АгроМаркет"`,
  `showLogo = true`.
- `assembleDebug`, `lint`, `testDebugUnitTest` зелёные. APK пересобран и задеплоен на
  `https://agro.assaru.space/downloads/agromarket-debug.apk` (Content-Length 20165005 совпадает).
- Изменения не закоммичены.

Закрыто 2026-06-14 (новый логотип):
- Заменён логотип во всём приложении на новый брендированный (трактор/корова/поле/солнце в
  круглом бейдже из листа и колоса). Источник — `logo.png` (1254×1254, корень репо), фон убран,
  векторизован через `vtracer` → `logo.svg` (auto-trace, ~730 КБ, posterized flat-style).
- Иконка приложения: пересобраны все 5 `mipmap-*/ic_launcher_foreground.png` (новый логотип в
  safe zone адаптивной иконки, ~66%, как у старой). Splash screen (`windowSplashScreenAnimatedIcon`)
  переиспользует тот же foreground — обновился автоматически.
- `AuthHero` (шапка Login/Register): заглушка `Icons.Rounded.Eco` заменена на реальный логотип —
  добавлен `app/src/main/res/drawable/logo.png` (512×531, прозрачный), `AuthHero.kt` теперь рендерит
  `Image(painterResource(R.drawable.logo))`.
- `assembleDebug` собран и задеплоен на VPS `server-main` (72.56.77.253,
  `/var/www/agromarket-downloads/agromarket-debug.apk`), доступен по
  `https://agro.assaru.space/downloads/agromarket-debug.apk` (проверено `curl -I`, 200,
  Content-Length совпадает с локальным APK).
- Изменения не закоммичены в git (только в рабочем дереве).

Все пункты [audit.md](../audit.md) (от 2026-06-11) закрыты. Открытые на усмотрение пункты из [audit-2026-06-12.md](../audit-2026-06-12.md) §4 (дубли компонентов, не блокируют): `PasswordTextField` повторён 5 раз в auth-экранах; `ErrorState(message, onRetry)` скопирован в `ProfileScreen`/`CreateAdCategoryStep`; `MyAdRow` в `ProfileScreen` почти повторяет `FavoriteRow`; каскад гео-пикеров в `CreateAdFormStep` просится в `PickerField`.

Закрыто 2026-06-12 (фиксы подачи объявлений):
- **«Нет районов» после выбора региона** — две причины. (а) `DADATA_API_KEY`/`DADATA_SECRET_KEY` в `/root/agromarket/.env` на VPS `server-main` (72.56.77.253) были пустыми с самого начала — владелец завёл аккаунт dadata.ru, ключи вписаны, контейнер пересоздан (`docker compose up -d backend`; важно: `restart` env не перечитывает, нужен `up -d`; первый ключ владельца не работал — «Feature 'SUGGESTIONS' disabled», нужно подключить сервис «Подсказки» в кабинете DaData). (б) DaData Suggestions жёстко ограничивает `count` до 20 — `fias_service.py` просил 100/200, но получал максимум 20 районов/нас. пунктов на регион/район. Переписан `backend/app/services/fias_service.py`: полные списки собираются перебором букв алфавита с привязкой через `locations`+`region_fias_id`/`area_fias_id` (~32 запроса на регион/район, однократно — дальше кэш в БД; бесплатный лимит 10 000/день). Бэкап старой версии — `/root/nginx-backups/fias_service.py.*.bak`. Неполный кэш (56 районов) вычищен из БД (объявления на него не ссылались). Проверено: Краснодарский край 38 районов, Тюменская обл. 22, Адыгея 7, Гиагинский район 49 нас. пунктов. Бэкенд на сервере НЕ под git — источник правды только `/root/agromarket`.
- **«Минимум 2 фото» при загруженных фото** — корневая причина: на VPS в `/etc/nginx/sites-enabled/agro.assaru.space` не было `client_max_body_size` (дефолт 1 МБ), multipart с 2+ сжатыми фото (~200–600 КБ каждое) получал 413 от nginx, а приложение игнорировало результат `uploadPhotos` и шло на `submit`, где сервер отвечал «Загрузите минимум 2 фото». Починено: `client_max_body_size 50M;` добавлен в server-блок (значение из задуманного `nginx/nginx.prod.conf` репо бэкенда), бэкап конфига в `/root/nginx-backups/`, проверено probe-запросами (3–8 МБ → 401 вместо 413). В приложении: `submitAd` теперь конвертирует фото до `createAd` (fail fast), проверяет результат `uploadPhotos` и при ошибке удаляет созданный черновик (`deleteAd`) — без осиротевших draft при ретрае; лимит фото 10 → 5 (`MAX_PHOTOS`/`MIN_PHOTOS` в companion `CreateAdViewModel`, серверный `MAX_PHOTOS_PER_AD=5`). `assembleDebug` + `testDebugUnitTest` зелёные. Изменения не закоммичены.

Закрыто 2026-06-12 (реализация дизайн-ресёрча, см. [design-research-deep-2026-06-12.md](../design-research-deep-2026-06-12.md), секция 9):
- Топ-10 дизайн-изменений реализован: ошибка фида с Retry (общий `ErrorState`, п.3.1 аудита);
  кликабельные телефоны `ACTION_DIAL` + sticky CTA-бар в `AdDetail` (п.3.2); сердечко избранного
  в фиде с optimistic toggle (п.3.3); boost-бейджи `boostLevel` 1/2/3 в `AdCard` (`boostTier()` +
  тест `BoostTierTest`); кебаб «Удалить» в «Моих объявлениях» через готовый `deleteAd` (п.3.4);
  фуллскрин-галерея с pinch/double-tap зумом; избранное — свайп-удаление с Undo-снекбаром,
  приглушение неактивных лотов (alpha 0.6, мёртвые вниз), пустое состояние с CTA «К объявлениям»;
  визард — сегментный степпер (олива=пройдено, глина=текущий) + автосохранение черновика
  в DataStore (`AdDraftManager`, `ad_draft_prefs`, фото не сохраняются — transient URI permissions);
  токены `AgroCream`/`AgroTerracotta` в Color.kt. `assembleDebug`, `testDebugUnitTest`, `lint` — зелёные.
- Сознательно не сделано: чипы применённых фильтров (фильтры и так постоянно видимы как FilterChips —
  дублирование), «расстояние N км» в карточке (нет геолокации в бэке), таблица спеков в `AdDetail`
  (бэк не отдаёт структурированные характеристики), XL-карточка для boost=3 (лента и так
  одноколоночная full-width — boost 3 переиспользует усиление tier 2).

Закрыто 2026-06-11:
- `LandsScreen` доработан (карточки `LandCard` в стиле референса — фото, регион,
  цена; `LandsViewModel` грузит `categoryId = LAND_CATEGORY_ID` через `AgroRepository.getFeed`) и
  подключён в `Navigation.kt` как `Screen.Lands`, вход — иконка "Земли" в `AppTopBar` на `FeedScreen`.
- Тесты на refresh-логику и шифрование токенов: `TokenAuthenticatorTest` (7), `TokenManagerTest`
  (5, Robolectric), `CryptoManagerTest` (5, `app/src/androidTest`, ещё не запускался — нужен
  эмулятор). `TokenManager` теперь получает `CryptoManager` через конструктор (DI).
- Несовпадение `type` между приложением и `GET /ads/` бэкенда: сверено с
  `https://agro.assaru.space/openapi.json` — бэкенд самосогласован (`AdType = sale|rent|service`),
  `"land"` — это категория `category_id=56`, а не `type`. Поправлено приложение
  (`FeedScreen`/`CreateAdScreen`/`StatusBadge`/`LandsScreen` → `"sale"/"rent"/"service"` +
  `LAND_CATEGORY_ID`). Изменения пока не закоммичены.

**Why:** аудит обновляется по мере работы — перед крупными изменениями сверяться с `audit.md`, чтобы не дублировать уже закрытые пункты или забытые открытые.
**How to apply:** при выборе следующей задачи — приоритет открытым пунктам аудита (сейчас остался #2 «забыли пароль»/смена пароля).
