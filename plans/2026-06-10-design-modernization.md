# AgroMarket — модернизация дизайна (план по фазам)

**Дата:** 2026-06-10
**Статус:** draft
**Проект:** `agromarket-android` (Kotlin · Jetpack Compose · Material3)
**Источник:** аудит дизайна от 2026-06-10 (чат), см. также [audit.md](../audit.md) — код-аудит, отдельная задача.

## Цель

Перевести визуал AgroMarket с дефолтного Material3-шаблона на современный продукт:
своя палитра + dark theme, единый icon-set вместо эмодзи, переработанные карточки/экраны,
брендированный splash и иконка. Референсы — Planta, Greg, PictureThis, Dribbble-теги
`garden-app`/`gardening_app` (см. чат-аудит).

## Принципы выполнения

- Каждая фаза — самостоятельный коммит/PR, приложение должно собираться и работать после неё.
- Сначала фундамент (токены темы), потом общие компоненты, потом экраны — чтобы не красить
  каждый экран дважды.
- UI-строки остаются на русском, код/комментарии — на английском (см. `CLAUDE.md`).
- После каждой фазы — `./gradlew assembleDebug` и визуальная проверка в эмуляторе/устройстве
  (skill `/run` или `/verify`).

---

## Фаза 0 — Дизайн-токены и фундамент темы

**Файлы:** [Theme.kt](../app/src/main/java/ru/agromarket/ui/theme/Theme.kt)

- Заменить стоковую Material Green палитру на брендовую: глубокий "лесной" зелёный как primary,
  тёплый акцент (терракота/охра) вместо `AgroOrange`.
- Добавить `darkColorScheme` и переключение по `isSystemInDarkTheme()`.
- Завести `Typography` (вместо хардкода `fontSize = ...sp` по экранам) и `Shapes`
  (единые радиусы скругления для карточек/полей/кнопок).
- Создать файл `Color.kt` / расширить `Theme.kt` токенами: `success`, `warning`, `danger`,
  цвета бейджей типов объявлений (`sell`/`service`/`land`).
- Не трогать сами экраны — только токены. Цель фазы: приложение собирается, выглядит как раньше
  (или чуть иначе из-за новой палитры), но появляется единая база для следующих фаз.

**Результат:** новая палитра видна на всех экранах через `MaterialTheme.colorScheme.*`,
dark theme работает (хотя бы базово, без доработки иконок).

---

## Фаза 1 — Общие компоненты (design system)

**Новые файлы:** `ui/components/` (AdCard, StatusBadge, EmptyState, LoadingState, AppTopBar)

- **AdCard** ([FeedScreen.kt:188-212](../app/src/main/java/ru/agromarket/ui/feed/FeedScreen.kt#L188-L212)):
  крупное фото (полная ширина или 16:9), бейдж типа объявления и цена — overlay поверх фото,
  кнопка избранного (сердечко) на карточке.
- **AppTopBar**: единая обёртка над `TopAppBar` с одинаковыми цветами/высотой —
  заменить разнобой между Feed/Favorites/Profile (зелёный фон) и AdDetail/CreateAd (дефолтный).
- **EmptyState**: компонент с иллюстрацией/крупной иконкой + заголовок + подпись —
  заменить разрозненные блоки "Нет избранных" / "Объявлений пока нет" / "Нет объявлений".
- **LoadingState / Skeleton**: shimmer-плейсхолдер для списка карточек вместо
  `CircularProgressIndicator` по центру экрана.
- Иконки категорий: завести `CategoryIcon` на базе единого icon-set (Material Symbols Outlined
  или Phosphor через `compose-icons`), с маппингом `category.icon`/ключ → `ImageVector`.
  Эмодзи остаются как fallback, но не как основной путь.

**Результат:** новые компоненты готовы и покрыты превью (`@Preview`), но ещё не подключены
везде — подключение по экранам в фазах 3-6.

---

## Фаза 2 — Auth (Login / Register)

**Файлы:** [LoginScreen.kt](../app/src/main/java/ru/agromarket/ui/auth/LoginScreen.kt), [RegisterScreen.kt](../app/src/main/java/ru/agromarket/ui/auth/RegisterScreen.kt)

- Убрать паттерн "сплошная зелёная заливка + белая карточка по центру".
- Новый layout: верхняя зона с иллюстрацией/градиентом и логотипом, нижняя —
  скруглённая "шторка" (surface) с формой входа на белом/тёмном фоне.
- Поля и кнопки — через обновлённые `Shapes`/`Typography` из Фазы 0.
- Сообщения об ошибках — единый стиль (например, через компонент-баннер, не голый `Text`).

**Результат:** первый экран, который видит пользователь, выглядит современно — задаёт тон
остальному приложению.

**Сделано:**
- Новый общий компонент [AuthHero.kt](../app/src/main/java/ru/agromarket/ui/components/AuthHero.kt) —
  градиентная "шапка" (`AgroPrimary` → `AgroSecondary`, фиксированные брендовые токены, не зависят
  от темы) с лого (`Icons.Rounded.Eco` в кружке) + заголовок/подзаголовок.
- Новый общий компонент [ErrorBanner.kt](../app/src/main/java/ru/agromarket/ui/components/ErrorBanner.kt) —
  единый баннер ошибок (`errorContainer`/`onErrorContainer` + иконка), заменил голый красный `Text`.
- [LoginScreen.kt](../app/src/main/java/ru/agromarket/ui/auth/LoginScreen.kt) и
  [RegisterScreen.kt](../app/src/main/java/ru/agromarket/ui/auth/RegisterScreen.kt) переведены на
  layout "градиентная шапка + скруглённая шторка-форма (`topStart/topEnd = 32dp`,
  `colorScheme.surface`, корректно для dark theme)".
- Поля и кнопки — `shape = MaterialTheme.shapes.medium` вместо хардкода `RoundedCornerShape(...)`,
  кнопки увеличены до `height(52.dp)`, текст кнопок — `titleMedium`.
- Шаги регистрации (`Шаг X из 3`) перенесены из тела карточки в подзаголовок шапки.
- `./gradlew assembleDebug` — успешно.
- Проект инициализирован как отдельный git-репозиторий и запушен в
  [github.com/Roman72-186/agro-market](https://github.com/Roman72-186/agro-market) (ветка `main`,
  коммит `21d9f29`) — единый источник правды для локали/git/VPS.

---

## Фаза 3 — Feed (главный экран)

**Файлы:** [FeedScreen.kt](../app/src/main/java/ru/agromarket/ui/feed/FeedScreen.kt)

- Подключить `AppTopBar`, новый `AdCard`, `EmptyState`, `Skeleton`.
- Категории (`QUICK_CATEGORIES`): заменить эмодзи на `CategoryIcon`, обновить визуал чипов
  (например, карточки-плитки с иконкой + подписью вместо `FilterChip` с эмодзи в тексте).
- Поисковая строка и фильтры типа — обновить под новые `Shapes`/цвета.
- Список объявлений: проверить отступы/плотность с новым крупным `AdCard`.

---

## Фаза 4 — Ad Detail

**Файлы:** [AdDetailScreen.kt](../app/src/main/java/ru/agromarket/ui/ad/AdDetailScreen.kt)

- Галерея фото: добавить индикатор количества/текущей позиции (точки или счётчик "2/5")
  поверх `LazyRow`.
- Цена/заголовок/бейдж типа — выровнять под новую типографику и цвета бейджей из Фазы 0.
- Карточка "Контакты" — обновить под новые `Shapes`/цвета (`AgroGreenBg` → токен).
- Кнопка "Запросить контакты" — привести к единому стилю кнопок (см. Фазу 1, если будет
  отдельный `PrimaryButton`).

---

## Фаза 5 — Create Ad (мастер размещения)

**Файлы:** [CreateAdScreen.kt](../app/src/main/java/ru/agromarket/ui/create/CreateAdScreen.kt)

- Шаг "Что размещаем?" и грид категорий — заменить эмодзи на `CategoryIcon`,
  карточки выбора типа — под новые `Shapes`/elevation.
- Прогресс мастера: добавить визуальный индикатор шага (1/4, 2/4...) — сейчас шаги
  различаются только заголовком TopAppBar.
- Поля формы и пикеры регион/район/населённый пункт — единый стиль с остальными полями
  (после Фазы 0 типографика подтянется автоматически, но проверить визуально).
- Блок загрузки фото — обновить плейсхолдер "Добавить" под новую палитру.

---

## Фаза 6 — Favorites / Profile

**Файлы:** [FavoritesScreen.kt](../app/src/main/java/ru/agromarket/ui/favorites/FavoritesScreen.kt), [ProfileScreen.kt](../app/src/main/java/ru/agromarket/ui/profile/ProfileScreen.kt)

- Favorites: подключить `AppTopBar`, `EmptyState`, обновлённую карточку (компактный вариант
  `AdCard` или отдельный `FavoriteRow` с теми же токенами).
- Profile: карточка профиля — обновить цвета (`AgroGreenBg` → токен из Фазы 0), статус-бейджи
  объявлений ("Активно"/"На модерации"/"Отклонено") — через `StatusBadge` из Фазы 1.
- Bottom navigation ([Navigation.kt](../app/src/main/java/ru/agromarket/ui/navigation/Navigation.kt)):
  рассмотреть выделение центрального пункта "Разместить" как акцентной кнопки (FAB-стиль)
  вместо равнозначной иконки в `NavigationBar`.

---

## Фаза 7 — Брендинг: иконка приложения и splash

**Файлы:** [ic_launcher_foreground.xml](../app/src/main/res/drawable/ic_launcher_foreground.xml), [themes.xml](../app/src/main/res/values/themes.xml)

- Новая иконка приложения (адаптивная, foreground/background) — узнаваемый символ
  (росток/колос/корзина), не абстрактные "капли".
- Анимированный splash через `androidx.core.splashscreen` (SplashScreen API) вместо
  голой заливки `#2E7D32`.
- Обновить `statusBarColor`/`navigationBarColor` под новую палитру и dark theme.

---

## Фаза 8 — Полировка и QA

- Проверить dark theme на всех экранах (после фаз 2-6 могли остаться хардкод-цвета,
  не завязанные на `colorScheme`).
- Базовые анимации переходов между экранами (`NavHost` animations) и появления карточек
  (`AnimatedVisibility` для списков).
- Прогнать `./gradlew lint` — заодно поправить deprecated `Icons.Default.ArrowBack` →
  `Icons.AutoMirrored.Filled.ArrowBack`, если ещё остались (отмечено в код-аудите).
- Финальный визуальный проход по всем экранам в эмуляторе (light + dark).

---

## Зависимости между фазами

```
Фаза 0 (токены) ──┬─→ Фаза 1 (компоненты) ──┬─→ Фаза 3 (Feed)
                   │                          ├─→ Фаза 4 (AdDetail)
                   │                          ├─→ Фаза 5 (CreateAd)
                   │                          └─→ Фаза 6 (Favorites/Profile)
                   └─→ Фаза 2 (Auth) ───────────────────────────────────┘
                                                                          │
Фаза 7 (брендинг) — независима, можно делать параллельно ────────────────┤
                                                                          ▼
                                                                    Фаза 8 (QA/полировка)
```

## Принятые решения

- **Палитра "Глина и Олива" (Clay & Olive)** зафиксирована и реализована в Фазе 0
  ([Color.kt](../app/src/main/java/ru/agromarket/ui/theme/Color.kt),
  [Theme.kt](../app/src/main/java/ru/agromarket/ui/theme/Theme.kt)):
  - `primary` `#63722E`, `secondary` `#7B8B41`, `tertiary`/accent (клей/охра) `#8E7544`
  - `neutral-dark` `#11150B`, `neutral-light` `#F5F7F2`, `info` `#D7E9F7`
  - light + dark `ColorScheme`, переключение по `isSystemInDarkTheme()`
  - старые токены (`AgroGreen`, `AgroOrange`, `AgroGray`, `AgroBg` и т.д.) сохранены как алиасы
    на новые значения — экраны не трогали, как и предполагала Фаза 0
  - добавлены семантические токены `AgroSuccess`/`AgroWarning`/`AgroDanger`/`AgroInfo` для
    `StatusBadge` (Фаза 1) и бейджей типов объявлений
- **Shapes**: единая шкала скруглений в [Shape.kt](../app/src/main/java/ru/agromarket/ui/theme/Shape.kt)
  (`extraSmall=4dp` … `extraLarge=24dp`, по `--radius-screen`)
- **Typography**: заведена в [Type.kt](../app/src/main/java/ru/agromarket/ui/theme/Type.kt),
  стандартная M3-шкала на `FontFamily.Default` — готова к замене шрифта одной строкой
- **Icon-set**: остаёмся на Material Symbols Outlined — `material-icons-extended` уже подключена
  (build.gradle.kts:60), сторонний icon-set не нужен

`./gradlew assembleDebug` после Фазы 0 — успешно.

## Открытые вопросы (требуют решения)

- **Шрифты бренда** — присланы токены `--agro-font-sans: 'Plus Jakarta Sans'` и
  `--agro-font-mono: 'JetBrains Mono'`, но файлов шрифтов в проекте нет. Нужно решить:
  бандлить статические `.ttf` в `res/font/` (офлайн, +вес APK) или подключить Downloadable
  Fonts API (`androidx.compose.ui.text.googlefonts`, без файлов, но нужен Google Play Services
  и сертификат провайдера). До решения `Type.kt` использует `FontFamily.Default`.
- Нужна ли новая иконка приложения от дизайнера или генерировать программно (vector) —
  влияет на объём Фазы 7.
