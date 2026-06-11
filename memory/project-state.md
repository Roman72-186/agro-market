---
name: project-state
description: Текущий статус проекта — открытые пункты аудита, статус дизайн-модернизации
metadata:
  type: project
---

Дизайн-модернизация (фазы 0-8) завершена — палитра «Глина и Олива», новые компоненты (AdCard, StatusBadge, EmptyState, AppTopBar, AuthHero, ErrorBanner, CategoryIcon), dark theme, `./gradlew lint` чист. Подробности — [plans/2026-06-10-design-modernization.md](../plans/2026-06-10-design-modernization.md).

Открытые пункты аудита (см. [audit.md](../audit.md) от 2026-06-11):
1. Мёртвый функционал в UI: `forgotPassword`/`resetPassword`/`changePassword` реализованы в API/репозитории, но не вызываются из UI (нет экранов).

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
