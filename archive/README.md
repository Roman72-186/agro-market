# archive/ — мёртвый код, выведенный из сборки

Код здесь **не компилируется** (лежит вне `app/src/`). Перенесён по аудиту 2026-06-12
(отчёт: [../audit-2026-06-12.md](../audit-2026-06-12.md)). История файлов сохранена через `git mv`.

| Файл | Откуда | Почему мёртвый | Как вернуть |
|---|---|---|---|
| `ui/LandsScreen.kt` | `app/src/main/java/ru/agromarket/ui/lands/` | Экран «Земли СХ назначения» стал недостижим после удаления кнопки из шапки фида (коммит `8962d6a`); фильтр «Земли» в фиде покрывает тот же кейс | `git mv` обратно + вернуть `Screen.Lands`, composable-блок и точку входа в `Navigation.kt` |
| `ui/LandCard.kt` | `app/src/main/java/ru/agromarket/ui/components/` | Использовался только LandsScreen | `git mv` обратно |
| `ui/LandsLoadingState.kt` | вырезано из `ui/components/LoadingState.kt` | Скелетоны только для LandsScreen | вставить функции обратно в `LoadingState.kt` |

Также удалены без переноса (тривиально восстановимы из git):

- `Color.kt`: легаси-алиасы `AgroGreen`, `AgroGreenLight`, `AgroGreenDark`, `AgroGreenBg`, `AgroOrange`, `AgroGray`, `AgroBg` — нигде не использовались (`AgroRed` оставлен, он живой).
- `FeedScreen.kt`: поле `QuickCategory.icon` (эмодзи) — иконки давно берутся из `categoryIconFor()`.
