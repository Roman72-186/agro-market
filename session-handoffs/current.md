# Session Handoff

**Updated:** 2026-06-29 +0500
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
| 0 | Каркас CMP + bump Kotlin 1.9→2.0, KMP+Compose MP плагины, source sets | Windows | **В РАБОТЕ** (агент в worktree, ждём ревью) |
| 1a | Gson → kotlinx.serialization | Windows | не начата |
| 1b | Retrofit/OkHttp → Ktor | Windows | не начата |
| 1c | Hilt → Koin | Windows | не начата |
| 1d | Coil 2→3, выпил Accompanist | Windows | не начата |
| 1e | DataStore → multiplatform | Windows | не начата |
| 2 | expect/actual платформенного слоя (Android actual) | Windows | не начата |
| 3 | iOS-приложение: Xcode-проект + iosMain actual + запуск | macOS | не начата |
| 4 | iOS-интеграции: APNs/push, deep links, оплата | macOS | не начата |
| 5 | Релиз: CI macOS, подпись, TestFlight | macOS/CI | не начата |
| 6 | Монетизация iOS через StoreKit IAP (поздняя) | macOS | не начата |

## Current State

- Код пока нетронут в основной ветке (нативный Android, всё зелёное по прошлой сессии).
- **Фаза 0 запущена отдельным агентом в изолированном worktree.** Результат на момент написания
  ещё не получен. При продолжении: проверить вывод агента, убедиться что
  `gradlew.bat assembleDebug` + `testDebugUnitTest` зелёные, отревьюить diff, и только потом
  вливать worktree и запускать Фазу 1a. <!-- ОБНОВИТЬ этот блок по завершении Фазы 0 -->

## Открытые решения владельца (заложены дефолтами, не блокируют фазы 0–2)

- **Mac-окружение** — НЕ определено. Нужно к Фазе 3 (минимум — CI macOS-раннер).
- **Apple In-App Purchase** — Apple (Guideline 3.1.1) почти наверняка потребует Boost/Pro через
  StoreKit (комиссия 30%), а не ЮKassa. **Дефолт: в первом релизе iOS платные функции скрыты**;
  монетизация iOS — поздняя Фаза 6.
- **Apple Developer Program ($99/год)** — нужен к фазам 4–5 (push/публикация). Оформить заранее.

## Next Steps

1. Дождаться завершения агента Фазы 0, прочитать его отчёт.
2. Отревьюить diff в worktree, прогнать `gradlew.bat assembleDebug` + `testDebugUnitTest` —
   убедиться, что зелёные и приложение Android не сломано.
3. По подтверждению владельца — влить изменения Фазы 0, обновить этот handoff (статус Фазы 0 →
   готова, статус 1a → в работе), запустить агента Фазы 1a по тому же шаблону.
4. Повторять цикл «агент-фаза в worktree → ревью → влить → следующая» для 1a…2.
5. К Фазе 3 — решить вопрос Mac-окружения и Apple Developer Program.

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
