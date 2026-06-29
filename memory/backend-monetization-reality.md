---
name: backend-monetization-reality
description: На бэкенде AgroMarket уже есть спящая платёжка ЮKassa (boost 7/30, платное размещение), но не 3-уровневая и без подписок
metadata:
  type: project
---

Бэкенд AgroMarket (FastAPI, VPS server-main `/root/agromarket/backend`, НЕ под git) **уже
содержит** монетизацию — это важно, чтобы не дублировать (проверено 2026-06-29):

- `Ad.boost_level` (enum `BoostLevel`: `none / top_7days / top_30days`) и `Ad.boost_expires_at`
  с индексом `ix_ads_boost` — **уже в БД**. Boost смоделирован как ОДИН уровень «топ» на
  7 или 30 дней, **не** 3 визуальных уровня и **без** срока 14 дней.
- `models/payment.py` (`Payment`, `PaymentType = ad_placement/boost_7/boost_30`,
  `yukassa_payment_id`, `yukassa_confirmation_url`), `services/yukassa_service.py`.
- Рабочий флоу: `POST /api/v1/payments/create {ad_id, type}` → `confirmation_url` ЮKassa →
  пользователь платит → `POST /api/v1/payments/webhook` ставит `boost_level`+`boost_expires_at`.
  Роутер `/payments` подключён в `app/api/v1/router.py`.
- **НО ЮKassa НЕ настроена**: `YUKASSA_SHOP_ID`/`YUKASSA_SECRET_KEY` в `.env` пустые →
  платёжка спящая, реально не списывает.
- Есть **платное размещение** (`AdStatus.PENDING_PAYMENT`, `settings.PRICE_AD_PLACEMENT`) —
  противоречит нашей стратегии «размещение бесплатно» ([[monetization-direction]]).
- **Подписок/Pro/`is_pro` на бэке НЕТ** — Фаза 2 на бэке greenfield.

Разрешено 2026-06-29 (решение владельца): **равняемся на существующий бэкенд**. Приложение
переделано под реальный контракт — boost = один уровень, срок 7/30 через
`POST /payments/create {ad_id, type: boost_7|boost_30}` → `confirmation_url` (открывается
`Intent.ACTION_VIEW`). `boostTier()` маппит `top_7days/top_30days → 1`. Платёжный шов —
`PaymentGateway.startBoostPayment`; флаг `MOCK_BOOST=true` в `SimulatedPaymentGateway` пока
имитирует boost офлайн (ключи ЮKassa пустые). Когда `YUKASSA_SHOP_ID/SECRET_KEY` настроят на
бэке — выставить `MOCK_BOOST=false`, и boost пойдёт через реальную ЮKassa. Прод НЕ менялся.

Pro-подписка **отложена**: бэкенда подписок нет, в приложении экран/бейдж есть, но
`purchaseSubscription` всегда мок. Эндпоинты `/payments/create` + `/payments/webhook`
(активация boost) уже на бэке — дорабатывать их не нужно, нужны только ключи ЮKassa.
