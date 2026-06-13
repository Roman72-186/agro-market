---
name: project-state
description: Текущий статус проекта — открытые пункты аудита, статус дизайн-модернизации
metadata:
  type: project
---

Дизайн-модернизация (фазы 0-8) завершена — палитра «Глина и Олива», новые компоненты (AdCard, StatusBadge, EmptyState, AppTopBar, AuthHero, ErrorBanner, CategoryIcon), dark theme, `./gradlew lint` чист. Подробности — [plans/2026-06-10-design-modernization.md](../plans/2026-06-10-design-modernization.md).

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
