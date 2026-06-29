# AgroMarket — фикс «Ошибка сервера» на login/register: переход на Transaction Pooler

**Дата:** 2026-06-18
**Статус:** done (применено на проде 2026-06-19)
**Затрагивает:** `agro-market` (бэкенд, отдельный репозиторий, прод на VPS `server-main`, 72.56.77.253, источник правды — `/root/agromarket`, НЕ под git). Android-приложение (`agromarket-android`) не меняется — баг целиком на бэкенде.

## Симптом

Пользователь видит «Ошибка сервера» на экране входа/регистрации (общее сообщение из
`AgroRepository.kt:141`, подставляется когда тело ответа не парсится как JSON с полем `detail`).

## Диагностика (проверено 2026-06-18)

- `POST /api/v1/auth/register` стабильно (2/2 в проверке) отвечает `500 Internal Server Error`
  (голый текст, не JSON) — отсюда и generic сообщение в приложении.
- В логах backend-контейнера (`docker compose logs backend`) трейс показывает падение на
  первом же обращении к БД в хендлере (`backend/app/api/v1/endpoints/auth.py:44`,
  `db.execute(select(User).where(User.email == ...))`), **до** шага отправки кода на email:
  ```
  asyncpg.exceptions.InternalServerError: (EMAXCONNSESSION) max clients reached in session
  mode - max clients are limited to pool_size: 15
  ```
- Та же ошибка ловится и на других эндпоинтах (видел на `GET /ads/admin/moderation`) — за
  10 минут наблюдения 23 случая. Это один баг, не два (версия «падает на отправке email»
  отброшена — трейс показывает падение раньше, на чтении из БД).

### Корневая причина

- БД подключена через **Supabase Session Pooler**
  (`.env`: `POSTGRES_HOST=aws-0-eu-west-1.pooler.supabase.com`, `POSTGRES_PORT=5432`) — у него
  жёсткий лимит **15 одновременных клиентских подключений**.
- `backend/app/core/database.py` создаёт SQLAlchemy async-движок с
  `pool_size=20, max_overflow=10` — до **30 подключений с одного процесса**.
- Процессов несколько, и каждый при импорте `app.core.database` создаёт свой экземпляр
  движка с теми же лимитами:
  - uvicorn `--workers 2` (`docker-compose.yml`) → до 2×30 = 60;
  - Celery worker (`concurrency=2`, `backend/app/services/tasks.py` использует тот же
    `async_session`/`engine` из `app.core.database`) → ещё до 30;
  - Celery beat — тоже импортирует модуль, создаёт движок (даже если сам почти не ходит в БД).
- Итого потенциальный спрос — **до ~90+ подключений** на пулер с лимитом 15. Любой всплеск
  параллельных запросов (вход, регистрация, лента) выбивает лимит.
- Побочная находка: `config.py:43 database_url_sync` объявлен, но не используется нигде в
  коде — мёртвый код, не часть этого фикса, но стоит знать при следующей уборке.

## Выбранное решение: Transaction Pooler (порт 6543) + фикс под asyncpg

Session Pooler (5432) — не предназначен для нескольких процессов/воркеров с большими пулами
подключений, это его архитектурное ограничение, не баг конфигурации, который можно
«подкрутить» цифрами. Transaction Pooler (6543) рассчитан именно на этот паттерн — много
коротких подключений от нескольких процессов.

**Важный нюанс (иначе получим новый класс ошибок вместо старого):** PgBouncer в transaction
mode не умеет шарить server-side prepared statements между транзакциями — каждая транзакция
может уехать на любое физическое соединение. `asyncpg` по умолчанию кэширует prepared
statements на стороне клиента, и под transaction pooling это будет валиться с
`prepared statement "..." already exists` / `does not exist`. Обязательно отключить кэш:
`connect_args={"statement_cache_size": 0}` в `create_async_engine`.

### Шаги

1. **Получить connection string Transaction Pooler** в дашборде Supabase проекта
   (`osnpscfrxehzzuthbbrr`) — Database → Connection pooling → Mode: Transaction. Хост обычно
   тот же (`aws-0-eu-west-1.pooler.supabase.com`), отличается порт (`6543` вместо `5432`) и
   возможно формат `user`. Зафиксировать также **лимит подключений транзакционного пулера**
   для тарифа проекта (показан в том же разделе дашборда) — он определяет безопасный суммарный
   `pool_size × число процессов`.
2. **Бэкап перед правкой** (VPS не под git — единственная защита, см. прецедент
   `*.pre-revision-button` в `memory/project-state.md`):
   ```
   cp /root/agromarket/.env /root/agromarket/.env.pre-pooler-fix.bak
   cp /root/agromarket/backend/app/core/database.py /root/agromarket/backend/app/core/database.py.pre-pooler-fix.bak
   ```
3. **`.env`** — поменять порт (и при необходимости юзера, если Supabase даёт другой) на
   Transaction Pooler, обновить комментарий:
   ```
   # ---- PostgreSQL (Supabase, Transaction Pooler) ----
   POSTGRES_PORT=6543
   ```
4. **`backend/app/core/database.py`** — добавить `connect_args`:
   ```python
   engine = create_async_engine(
       settings.database_url,
       echo=settings.DEBUG,
       pool_size=20,
       max_overflow=10,
       pool_pre_ping=True,
       connect_args={"statement_cache_size": 0},
   )
   ```
   `pool_size`/`max_overflow` пока не трогаем — у transaction pooler потолок выше, но нужно
   сверить с лимитом из шага 1 и при необходимости подрезать (учитывая 2 uvicorn-воркера +
   celery worker + beat — те же 4 процесса, что и раньше, теперь упираются в лимит транзакционного
   пулера, а не сессионного).
5. **Деплой:** `docker compose build backend && docker compose up -d backend` (не `restart` —
   `.env` не перечитывается без пересоздания контейнера, см. прецедент с DaData в
   `memory/project-state.md`). Аналогично пересоздать `celery-worker`/`celery-beat`, если они
   отдельные сервисы в `docker-compose.yml`.
6. **Проверка:**
   - `docker compose logs backend --tail=50` — нет `EMAXCONNSESSION` и нет
     `prepared statement ... already exists` после деплоя.
   - Несколько параллельных запросов на `register`/`login` (имитация нагрузки):
     ```
     for i in 1 2 3 4 5; do curl -s -o /dev/null -w "%{http_code}\n" -X POST \
       https://agro.assaru.space/api/v1/auth/register -H "Content-Type: application/json" \
       -d "{\"email\":\"diag_pool_$i@example.com\"}" & done; wait
     ```
     Ожидаем `200` (или `422`/`409` по бизнес-логике, но не `500`).
   - Реальный сценарий в приложении: регистрация нового пользователя до конца (код на email,
     установка пароля, вход).

### Откат

Если после перехода полезут `prepared statement` ошибки или транзакционный пулер ведёт себя
хуже — вернуть `.env`/`database.py` из `.pre-pooler-fix.bak` и передеплоить.

## Не входит в этот фикс

- Уборка мёртвого `database_url_sync` (`config.py:43`) — отдельная задача.
- Снижение `pool_size`/`max_overflow` как самостоятельная мера (вариант 1 из обсуждения) —
  не делаем, выбран сразу вариант с transaction pooler.

## Выполнено (2026-06-19)

- Подтверждено документацией Supabase (`mcp__supabase__search_docs`, "Connect to your database",
  "Supavisor FAQ"): в session mode пулер выделяет клиенту выделенное backend-подключение 1:1, и
  именно поэтому максимум одновременных клиентов жёстко равен backend pool size (15) — это и есть
  источник `EMAXCONNSESSION`. В transaction mode пулер мультиплексирует — много клиентов делят то
  же количество backend-подключений, лимит клиентов резко выше. Подтверждены формат connection
  string (хост и `postgres.<project_ref>` те же, отличается только порт 6543) и обязательность
  `statement_cache_size: 0` для asyncpg под transaction pooling.
- Бэкапы перед правкой: `/root/agromarket/.env.pre-pooler-fix.bak`,
  `/root/agromarket/backend/app/core/database.py.pre-pooler-fix.bak`.
- `.env`: `POSTGRES_PORT` `5432` → `6543`, комментарий обновлён на «Transaction Pooler».
  `pool_size=20`/`max_overflow=10` в `database.py` оставлены без изменений — после перехода они
  не упираются в лимит клиентов; пересматривать только если в дальнейшем появятся явные признаки
  нагрузки на backend-подключения (см. Observability → Database в дашборде Supabase).
- `backend/app/core/database.py`: добавлен `connect_args={"statement_cache_size": 0}` в
  `create_async_engine`.
- Деплой: `docker compose build backend && docker compose up -d --no-deps backend celery-worker
  celery-beat` (все три сервиса импортируют один и тот же `app.core.database`).
- Проверка:
  - `docker compose logs backend` после рестарта — чистый старт, без ошибок подключения к БД.
  - `POST /auth/register` с реальным email → `200` вместо `500` (было воспроизведено 2/2 до фикса).
  - 20 параллельных запросов (10× register, 5× login, 5× categories) — все ответили корректными
    кодами (`200`/`401`), ни одного `500`, ни `EMAXCONNSESSION`, ни `prepared statement` в логах
    backend/celery-worker/celery-beat за время теста.
  - В логах остались только ожидаемые `SMTPRecipientRefused` для тестовых `@example.com` — это
    реакция почтового сервера на несуществующие адреса, не относится к багу с пулом; в БД эти
    вызовы оставили только строки `EmailCode` (TTL 15 минут), `User` создаётся позже на шаге
    `set-password` — орфанных пользователей не осталось.
- Открыто на будущее: мёртвый `database_url_sync` (см. «Не входит в этот фикс») и периодическая
  сверка лимита backend-подключений Supabase (Database Settings → Pool Size) с суммой
  `pool_size`/`max_overflow` по всем процессам, если проект перейдёт на больший тариф/нагрузку.
