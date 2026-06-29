# AgroMarket — модерация объявлений: обратная связь, доработка, повторная отправка

**Дата:** 2026-06-13
**Статус:** draft
**Затрагивает:** `agromarket-android` (этот репозиторий) + `agro-market` (бэкенд, отдельный репозиторий, прод на VPS `server-main`, 72.56.77.253, источник правды — `/root/agromarket`, НЕ под git)

## Контекст (проверено по `https://agro.assaru.space/openapi.json`, 2026-06-13)

- `AdStatus` (бэкенд, enum): `draft, pending_payment, pending_moderation, active, rejected, expired, deleted`. Статуса «на доработке» нет.
- `POST /ads/{ad_id}/moderate` уже существует: `AdModerationAction = {action: "approve"|"reject", comment?: string (≤1000)}`. Отказ с комментарием уже пишется бэкендом в `moderation_comment` и отдаётся в `AdDetailResponse` и `AdMyListResponse`.
- В Android: `AdDetailResponse.moderationComment` (`Models.kt:97`) объявлен, но не выводится ни на одном экране. Локальная модель `AdMyListResponse` (`AgroMarketApi.kt:145-155`) поле `moderation_comment` вообще не объявляет — Gson его молча отбрасывает.
- Фото к фидбеку модератора, push-уведомления, повторная отправка `rejected`/новый статус «на доработке» — не реализовано ни на бэке, ни в приложении.
- Есть админ-эндпоинты `GET /ads/admin/moderation`, `GET /admin/ads`, `/admin/users/...` — админ-панель вне этого репозитория, контракт ниже её касается, но реализация в `agro-market`.

## Целевая логика (зафиксировано в обсуждении)

Статусы объявления:
- `draft` → `pending_moderation` — как сейчас (`POST /ads/{id}/submit`)
- `pending_moderation` → `active` — approve, как сейчас
- `pending_moderation` → `needs_revision` **(новый статус)** — модератор вернул с комментарием + опционально фото; можно исправить и отправить снова
- `pending_moderation` → `rejected` — как сейчас, окончательный отказ без права доработки
- `needs_revision` → `pending_moderation` — после `PUT /ads/{id}` (правки) и повторного `submit`

Фидбек модератора — текст (`comment`, уже есть) + фото, которые **загружает модератор** (новое, с историей по раундам).

Уведомление пользователя о смене статуса — push (FCM), полностью новая интеграция (на бэке нет даже таблицы device-токенов).

## Фаза A — показать уже существующий фидбек (без изменений бэкенда)

Бэкенд уже отдаёт `moderation_comment` для `rejected`. Это можно показать пользователю прямо сейчас.

- `Models.kt` / `AgroMarketApi.kt:145-155`: добавить `@SerializedName("moderation_comment") val moderationComment: String? = null` в `AdMyListResponse`.
- `StatusBadge.kt`: `adStatusBadge` уже маппит `"rejected"` → `"Отклонено"` / `DANGER` — без изменений.
- `AdDetailScreen.kt` (владелец объявления): если `status == "rejected" && moderationComment != null` — карточка «Причина отклонения» с текстом комментария.
- `ProfileScreen.kt` `MyAdRow` (`profile/ProfileScreen.kt:241-274`): при `rejected` — показать первую строку `moderationComment` под бейджем статуса (или иконку с подсказкой).

CTA «исправить» здесь не делаем — `rejected` сейчас терминальный статус, `submit` для него почти наверняка не разрешён (проверить в `agro-market` перед Фазой C).

## Фаза B — статус «на доработке» + фото фидбека (бэкенд, репозиторий `agro-market`)

Миграция БД:
```sql
ALTER TYPE adstatus ADD VALUE 'needs_revision';

CREATE TABLE ad_moderation_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_id UUID NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    moderator_id UUID NOT NULL REFERENCES users(id),
    decision VARCHAR(20) NOT NULL,  -- 'needs_revision' | 'rejected'
    comment TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at TIMESTAMPTZ          -- заполняется при повторной отправке
);

CREATE TABLE ad_moderation_feedback_photos (
    id SERIAL PRIMARY KEY,
    feedback_id UUID NOT NULL REFERENCES ad_moderation_feedback(id) ON DELETE CASCADE,
    url TEXT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);
```

API:
- `AdModerationAction.action` pattern → `^(approve|reject|request_revision)$`. `request_revision` требует `comment` (валидация как у `reject`).
- `POST /ads/{ad_id}/moderate` → multipart вместо JSON: поля `action`, `comment`, файлы `photos[]` (опционально, для `reject`/`request_revision`). При `request_revision`/`reject` создаётся строка в `ad_moderation_feedback` (+ фото), статус объявления меняется, отправляется push.
- `AdDetailResponse` / `AdMyListResponse`: вместо плоского `moderation_comment` — объект `moderation_feedback: {decision, comment, photos: [{id, url, sort_order}], created_at} | null` (последний непогашенный фидбек). **Breaking change** — на переходный период отдавать оба поля (`moderation_comment` = `moderation_feedback.comment` для обратной совместимости со старыми клиентами), снести `moderation_comment` отдельным релизом.
- `POST /ads/{ad_id}/submit`: разрешить переход `needs_revision → pending_moderation` (сейчас, видимо, только `draft → pending_moderation` — проверить в коде). При успехе — `ad_moderation_feedback.resolved_at = now()` для последнего открытого фидбека.
- (v2, опционально) `GET /ads/{ad_id}/moderation-history` — все раунды фидбека.

Push-инфраструктура:
- `CREATE TABLE user_push_tokens (user_id UUID REFERENCES users(id), token TEXT, platform TEXT, created_at TIMESTAMPTZ)`.
- `POST /profile/me/push-tokens` `{token, platform: "android"}` — upsert; `DELETE /profile/me/push-tokens/{token}`.
- В `/ads/{id}/moderate` при `approve|reject|request_revision` — отправка push владельцу объявления через FCM HTTP v1 (нужны креды сервис-аккаунта Firebase в `.env` на VPS).

## Фаза C — Android: карточка фидбека + edit/resubmit (зависит от Фазы B)

- `StatusBadge.kt`: добавить `"needs_revision" -> "На доработке" to BadgeTone.WARNING`. `MyAdRow`/`AdCard` подхватят автоматически через `adStatusBadge`.
- `Models.kt`: `ModerationFeedbackResponse(decision, comment, photos: List<AdPhotoResponse>, createdAt)`; `AdDetailResponse`/`AdMyListResponse.moderationFeedback: ModerationFeedbackResponse? = null` (заменяет `moderationComment` из Фазы A).
- `AdDetailScreen.kt`, для владельца при `status in [needs_revision, rejected]` и `moderationFeedback != null` — карточка «Замечания модератора»:
  - текст `comment`;
  - галерея фото модератора (переиспользовать существующий fullscreen pinch/zoom компонент из ленты фото объявления);
  - при `needs_revision` — кнопка «Исправить и отправить снова» → переход в редактирование;
  - при `rejected` — без CTA (терминально).
- Навигация: новый маршрут `Screen.EditAd(adId)` либо опциональный `adId` в `Screen.CreateAd`.
- `CreateAdViewModel`: при наличии `adId` — загрузить `getAdDetail(adId)`, предзаполнить шаги визарда (категория/тип, гео-цепочка, заголовок/описание/цена/телефоны), существующие фото — список с `deletePhoto`, новые — через `uploadPhotos`.
- Финальный шаг: кнопка «Отправить на повторную проверку» вместо «Опубликовать», когда `adId != null && status == "needs_revision"`. На сабмите: `updateAd(adId, request)` → загрузка новых фото → `submitAd(adId)`.

## Фаза D — Push-уведомления (FCM, можно параллельно с C, но полезнее после неё — deep link на карточку фидбека)

Android:
- Gradle: плагин `com.google.gms:google-services` (project-level), `firebase-bom` + `firebase-messaging-ktx` (app-level).
- Ручной шаг: создать проект Firebase, добавить Android-приложение (`ru.agromarket`), положить `google-services.json` в `app/`.
- `AndroidManifest.xml`: сервис `AgroFirebaseMessagingService` с intent-filter `MESSAGING_EVENT`; разрешение `POST_NOTIFICATIONS` (Android 13+, runtime-запрос).
- `AgroFirebaseMessagingService : FirebaseMessagingService`:
  - `onNewToken` → `AgroRepository.registerPushToken(token)` (если не залогинен — закэшировать в DataStore и отправить после логина);
  - `onMessageReceived` → `NotificationCompat`, канал `moderation`, тап → deep link на `AdDetailScreen(adId)` из `data["ad_id"]`.
- `AgroMarketApi`: `POST profile/me/push-tokens`, `DELETE profile/me/push-tokens/{token}`; вызов register после успешного `login`/`setPassword`, unregister при логауте.

Backend: таблица `user_push_tokens` + эндпоинты + отправка push из `/ads/{id}/moderate` (см. Фазу B).

## Порядок реализации

A (независимо, можно сразу) → B (бэкенд, `agro-market` + VPS) → C (Android, требует API из B) → D (можно параллельно с B/C, но deep link на карточку фидбека полезен после C).

## Тесты

- `StatusBadgeTest`/существующий unit-тест бейджей — добавить кейс `needs_revision`.
- `ModelsSerializationTest.kt` — десериализация `moderation_feedback` (с фото и без).
- ViewModel-тест на резабмит: `CreateAdViewModel` с `adId` подгружает и сабмитит существующее объявление (мок `AgroRepository`).
