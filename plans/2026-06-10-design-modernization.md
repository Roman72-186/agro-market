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

**Сделано:**
- [StatusBadge.kt](../app/src/main/java/ru/agromarket/ui/components/StatusBadge.kt) —
  `BadgeTone` (`SUCCESS`/`WARNING`/`DANGER`/`INFO`/`NEUTRAL`) на токенах `AgroSuccess`/`AgroWarning`/
  `AgroDanger`/`AgroInfo` из Фазы 0, плюс хелперы `adTypeBadge(type)` и `adStatusBadge(status)`
  для единых подписей/цветов типа и статуса объявления.
- [EmptyState.kt](../app/src/main/java/ru/agromarket/ui/components/EmptyState.kt) —
  иконка в цветном круге (`primaryContainer`) + заголовок + опциональные подпись и `action`,
  заменяет разрозненные блоки "Нет избранных"/"Объявлений пока нет".
- [LoadingState.kt](../app/src/main/java/ru/agromarket/ui/components/LoadingState.kt) —
  `shimmerBrush()` (анимированный градиент по `surfaceVariant`), `AdCardSkeleton()` (плейсхолдер
  по форме `AdCard`) и `FeedLoadingState()` — список скелетонов вместо центрального
  `CircularProgressIndicator`.
- [AppTopBar.kt](../app/src/main/java/ru/agromarket/ui/components/AppTopBar.kt) —
  `CenterAlignedTopAppBar` на `colorScheme.primary`/`onPrimary`, опциональная кнопка "назад" и
  слот `actions` — единая шапка для Feed/Favorites/Profile/AdDetail/CreateAd.
- [CategoryIcon.kt](../app/src/main/java/ru/agromarket/ui/components/CategoryIcon.kt) —
  `categoryIconFor(categoryName)` маппит названия категорий (`Техника`, `Запчасти`, `Семена`,
  `Удобрения`, `Корма`, `Животные`, `Земля`, `Оборудование`, `Прочее`) на Material Symbols
  Outlined (`Agriculture`/`Build`/`Grass`/`Science`/`Grain`/`Pets`/`Terrain`/`Construction`/
  `Inventory2`, fallback `Category`), плюс `CategoryIcon` — круглый бейдж на
  `secondaryContainer`.
- [AdCard.kt](../app/src/main/java/ru/agromarket/ui/components/AdCard.kt) — новый дизайн карточки
  объявления: фото на всю ширину (16:9, плейсхолдер при отсутствии `photoUrl`), бейдж типа
  (и опционально статуса) поверх фото через `StatusBadge`/`adTypeBadge`/`adStatusBadge`, кнопка
  "избранное" (опциональная, `onFavoriteClick`), цена — overlay с градиентным скримом и
  шрифтом `JetBrainsMono`, заголовок и регион под фото.
- `./gradlew assembleDebug` — успешно.

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

**Сделано:**
- `TopAppBar` заменён на общий [AppTopBar.kt](../app/src/main/java/ru/agromarket/ui/components/AppTopBar.kt)
  (заголовок "🌾 АгроМаркет" + кнопка профиля в `actions`).
- Старая локальная функция `AdCard` (Row с фото 100dp) удалена, список объявлений рендерится
  новым [AdCard.kt](../app/src/main/java/ru/agromarket/ui/components/AdCard.kt) из Фазы 1
  (фото 16:9, бейдж типа, цена-оверлей).
- Полноэкранная загрузка (`CircularProgressIndicator` по центру) заменена на
  `FeedLoadingState()` (скелетоны карточек), пустой список — на `EmptyState` с иконкой
  `Inventory2` и подсказкой "Попробуйте изменить фильтры или поисковый запрос".
- Чипы быстрых категорий (`QUICK_CATEGORIES`) заменены на новый `QuickCategoryTile` —
  плитка `Surface` (иконка из `categoryIconFor` + подпись), подсветка `primaryContainer`
  при выборе вместо `FilterChip` с эмодзи в тексте. Поле `icon` (emoji) в `QuickCategory`
  оставлено — используется в Фазе 5 (`CreateAdScreen`).
- Поисковое поле — `shape = MaterialTheme.shapes.medium` (было `RoundedCornerShape(12.dp)`
  напрямую); подкатегории (`SuggestionChip`) — тоже `shapes.medium`. Текст фильтров типа
  (`FilterChip`) и поиска переведён с хардкода `fontSize` на `MaterialTheme.typography.*`.
- `./gradlew assembleDebug` — успешно.

---

## Фаза 4 — Ad Detail

**Файлы:** [AdDetailScreen.kt](../app/src/main/java/ru/agromarket/ui/ad/AdDetailScreen.kt)

- Галерея фото: добавить индикатор количества/текущей позиции (точки или счётчик "2/5")
  поверх `LazyRow`.
- Цена/заголовок/бейдж типа — выровнять под новую типографику и цвета бейджей из Фазы 0.
- Карточка "Контакты" — обновить под новые `Shapes`/цвета (`AgroGreenBg` → токен).
- Кнопка "Запросить контакты" — привести к единому стилю кнопок (см. Фазу 1, если будет
  отдельный `PrimaryButton`).

**Сделано:**
- `TopAppBar` заменён на общий `AppTopBar` (заголовок "Объявление", кнопка "назад", сердечко
  избранного в `actions`, тон сердечка — `AgroRed`/`onPrimary`).
- Галерея фото переведена с `LazyRow` (квадратные фото, скролл) на `HorizontalPager` (4:3,
  на всю ширину, edge-to-edge) со счётчиком "тек./всего" — `Surface` с полупрозрачным чёрным
  фоном поверх изображения, показывается только при `photos.size > 1`. Если фото нет —
  плейсхолдер `Icons.Outlined.Image` на `surfaceVariant` (как в `AdCard`).
- Бейдж типа объявления — через `StatusBadge`/`adTypeBadge` из Фазы 1 вместо ручной
  `Surface` с `AgroGreen.copy(alpha = 0.1f)`.
- Цена — `headlineMedium` шрифтом `JetBrainsMono` (как в `AdCard`), цвет
  `colorScheme.primary` вместо `AgroGreen`.
- Локация/категория — иконки и текст на `colorScheme.onSurfaceVariant` вместо `AgroGray`.
- Карточка "Контакты" — `colorScheme.primaryContainer`/`onPrimaryContainer` вместо
  `AgroGreenBg`/хардкод-зелёного, `shapes.large`.
- Сообщение после "Запросить контакты": ошибка — общий `ErrorBanner`, успех — аналогичный
  баннер на `primaryContainer` с иконкой `CheckCircle` (новое поле `contactError` в
  `AdDetailViewModel`), вместо голого `Text`.
- Кнопка "Запросить контакты" — `height(52.dp)`, `shape = shapes.medium`, текст
  `titleMedium` (как в Auth-экранах из Фазы 2).
- `./gradlew assembleDebug` — успешно.

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

**Сделано:**
- `TopAppBar`/условный заголовок заменены на общий `AppTopBar` (показывается на всех шагах,
  кнопка "назад" — везде кроме первого шага "Что размещаем?").
- Прогресс мастера: под `AppTopBar` добавлены `LinearProgressIndicator` и текст
  "Шаг X из Y" — `ViewModel.stepNumber`/`totalSteps` (3 шага без подкатегории, 4 — с ней).
- Шаг "Что размещаем?": эмодзи в карточках типов заменены на `Icons.Outlined.Storefront`/
  `Build`/`Terrain` в круглом бейдже (`secondaryContainer`, как в `CategoryIcon`), карточки —
  `shapes.large` + `CardDefaults.cardElevation`, текст — `MaterialTheme.typography.*`.
- Грид категорий (шаг 2): эмодзи `cat.icon` заменён на `CategoryIcon(categoryName = cat.name)`,
  карточки — `shapes.large`, `colorScheme.surfaceVariant`, небольшая elevation.
- Подкатегории (шаг 3) — карточки на `shapes.medium`, текст/иконка через
  `typography`/`onSurfaceVariant`.
- Форма (шаг 4): блок "путь" (тип › категория › подкатегория) — `primaryContainer`/
  `onPrimaryContainer` вместо `AgroGreen.copy(alpha=0.1f)`; пикеры регион/район/нас.пункт —
  `shapes.medium` вместо `RoundedCornerShape(4.dp)`, подсказки — `onSurfaceVariant` вместо
  `AgroGray`; плейсхолдер "Добавить фото" и счётчик фото — токены `colorScheme.primary`/
  `onSurfaceVariant`/`error` вместо `AgroGreen`/`AgroGray`/`AgroRed`; ошибка формы —
  через общий `ErrorBanner`; кнопка "Отправить на модерацию" — `shapes.medium`,
  `typography.titleMedium`.
- Загрузка/ошибка категорий (шаг 2 без данных) — цвета на `colorScheme.primary`/
  `onSurfaceVariant` вместо `AgroGreen`/`AgroGray` (dark theme).
- `./gradlew assembleDebug` — успешно.

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

**Сделано:**
- `TopAppBar` в Favorites/Profile заменён на общий `AppTopBar` (Profile — кнопка "выйти"
  в `actions`).
- Новый компонент [FavoriteRow.kt](../app/src/main/java/ru/agromarket/ui/components/FavoriteRow.kt) —
  компактная строка избранного: миниатюра 72dp (`shapes.medium`, плейсхолдер `Icons.Outlined.Image`
  на `surfaceVariant`, как в `AdCard`), заголовок, цена шрифтом `JetBrainsMono` на
  `colorScheme.primary`, опциональный `StatusBadge`/`adStatusBadge(adStatus)` под заголовком,
  кнопка "убрать из избранного" — `Icons.Filled.Favorite` на `colorScheme.error`. Карточка —
  `shapes.large`.
- Пустой список избранного — `EmptyState` (`Icons.Outlined.FavoriteBorder`, заголовок "Нет
  избранных", подпись-подсказка), загрузка — `CircularProgressIndicator` на `colorScheme.primary`.
- Profile: карточка профиля — `colorScheme.primaryContainer`/`onPrimaryContainer` вместо
  `AgroGreenBg`/дефолтных цветов текста, `shapes.large`; кружок камеры аватара —
  `colorScheme.primary`/`onPrimary` вместо `AgroGreen`; иконка редактирования —
  `onPrimaryContainer`; сообщение "Сохранено!"/ошибка сохранения — `onPrimaryContainer`,
  полужирным.
- Список "Мои объявления": статус-бейдж объявления (`adStatusBadge` + `StatusBadge` из Фазы 1)
  вместо ручного `when` с цветами `AgroGreen`/`AgroRed`/`AgroOrange`/`AgroGray`; карточки —
  `shapes.medium`, фото — `shapes.small`. Состояние ошибки загрузки профиля — иконка/текст на
  `colorScheme.onSurfaceVariant` вместо `AgroGray`.
- Bottom navigation: центральный пункт "Разместить" — иконка в круге `colorScheme.primary`/
  `onPrimary` (40dp, `CircleShape`), индикатор выбора `NavigationBarItem` для этого пункта
  отключён (`indicatorColor = Color.Transparent`), чтобы не дублировать круг.
- `./gradlew assembleDebug` — успешно.

---

## Фаза 7 — Брендинг: иконка приложения и splash

**Файлы:** [ic_launcher_foreground.xml](../app/src/main/res/drawable/ic_launcher_foreground.xml), [themes.xml](../app/src/main/res/values/themes.xml)

- Новая иконка приложения (адаптивная, foreground/background) — узнаваемый символ
  (росток/колос/корзина), не абстрактные "капли".
- Анимированный splash через `androidx.core.splashscreen` (SplashScreen API) вместо
  голой заливки `#2E7D32`.
- Обновить `statusBarColor`/`navigationBarColor` под новую палитру и dark theme.

**Сделано:**
- Адаптивная иконка: вместо старого `drawable/ic_launcher_foreground.xml` (вектор
  "белая капля") — растровый foreground (корзина с зелёным колосом/ростком,
  палитра "Глина и Олива"), сгенерирован из PNG-макета и разложен по
  `mipmap-{m,h,x,xx,xxx}hdpi/ic_launcher_foreground.png` (66% safe zone, прозрачные
  поля). `mipmap-anydpi-v26/ic_launcher.xml` и `ic_launcher_round.xml` теперь
  ссылаются на `@mipmap/ic_launcher_foreground`.
- `drawable/ic_launcher_background.xml` — сплошная заливка `@color/ic_launcher_background`
  (`#F5F7F2`, `AgroNeutralLight` — перекликается с фоном иконки), новый
  [colors.xml](../app/src/main/res/values/colors.xml).
- Splash через `androidx.core:core-splashscreen:1.0.1`: новый стиль
  `Theme.AgroMarket.Starting` (`parent="Theme.SplashScreen"`,
  `windowSplashScreenBackground=@color/splash_background` (`#F5F7F2`),
  `windowSplashScreenAnimatedIcon=@mipmap/ic_launcher_foreground`,
  `postSplashScreenTheme=Theme.AgroMarket`) вместо `Theme.AgroMarket.Splash` с
  заливкой `#2E7D32`. Активити в манифесте использует `Theme.AgroMarket.Starting`.
- [MainActivity.kt](../app/src/main/java/ru/agromarket/MainActivity.kt) —
  `installSplashScreen()` до `super.onCreate`, кастомная анимация выхода
  (`ObjectAnimator` fade-out, 250мс, `AccelerateInterpolator`).
- `statusBarColor`/`navigationBarColor` переведены на токены из `colors.xml`:
  light — `@color/status_bar` (`#63722E`, `AgroPrimary`) /
  `@color/nav_bar` (`#F5F7F2`, `AgroNeutralLight`); добавлен
  [values-night/themes.xml](../app/src/main/res/values-night/themes.xml) —
  `@color/status_bar_dark`/`@color/nav_bar_dark` (`#11150B`, `AgroBackgroundDark`)
  и тёмный вариант `windowSplashScreenBackground` (`@color/splash_background_dark`).
- `./gradlew assembleDebug` — успешно.

---

## Фаза 8 — Полировка и QA

- Проверить dark theme на всех экранах (после фаз 2-6 могли остаться хардкод-цвета,
  не завязанные на `colorScheme`).
- Базовые анимации переходов между экранами (`NavHost` animations) и появления карточек
  (`AnimatedVisibility` для списков).
- Прогнать `./gradlew lint` — заодно поправить deprecated `Icons.Default.ArrowBack` →
  `Icons.AutoMirrored.Filled.ArrowBack`, если ещё остались (отмечено в код-аудите).
- Финальный визуальный проход по всем экранам в эмуляторе (light + dark).

**Сделано:**
- Dark theme: grep-аудит хардкод-цветов (`Color.Black`/`Color.White`/`Color(0x...)`,
  `AgroGreen`/`AgroGray`/`AgroOrange` и т.п.) по всем экранам. Найден один
  оставшийся случай — [LandsScreen.kt](../app/src/main/java/ru/agromarket/ui/lands/LandsScreen.kt)
  (неподключённый экран): `TopAppBar` с `AgroGreen`/`AgroGray` заменён на общий
  `AppTopBar`, цвета текста/иконки переведены на `colorScheme.onSurfaceVariant`.
  Остальные хардкод-цвета (`AdCard`, `AdDetailScreen`, `AuthHero`, `StatusBadge`,
  `Theme.kt`) — намеренные (оверлеи на фото, фиксированные брендовые градиенты,
  цвета бейджей, определения самой палитры) и не требуют правок.
- [Navigation.kt](../app/src/main/java/ru/agromarket/ui/navigation/Navigation.kt) —
  `NavHost` получил `enterTransition`/`exitTransition`/`popEnterTransition`/
  `popExitTransition`: переход вперёд — fade + slide-in справа (220мс), назад —
  fade + slide-out вправо.
- Анимация перестановки элементов списков (`Modifier.animateItemPlacement()`,
  `@OptIn(ExperimentalFoundationApi::class)`) добавлена в
  [FeedScreen.kt](../app/src/main/java/ru/agromarket/ui/feed/FeedScreen.kt) (карточки
  объявлений), [FavoritesScreen.kt](../app/src/main/java/ru/agromarket/ui/favorites/FavoritesScreen.kt)
  (строки избранного) и [ProfileScreen.kt](../app/src/main/java/ru/agromarket/ui/profile/ProfileScreen.kt)
  (карточки "Мои объявления") — анимирует перестановку/удаление элементов
  при смене данных, не появление новых карточек при пагинации.
- `Icons.Default.ArrowBack` — не найдено, уже заменено на
  `Icons.AutoMirrored.Filled.ArrowBack` в предыдущих фазах.
- `./gradlew lint`: исправлена ошибка `PermissionImpliesUnsupportedChromeOsHardware` —
  добавлен `<uses-feature android:name="android.hardware.camera" android:required="false" />`
  в [AndroidManifest.xml](../app/src/main/AndroidManifest.xml). Оставшиеся
  warning'и (`GradleDependency` x10, `MonochromeLauncherIcon` x2,
  `DataExtractionRules`/`UnusedAttribute` — намеренно, см. комментарий в
  `data_extraction_rules.xml`, `ObsoleteSdkInt`, `ObsoleteLintCustomCheck`) —
  вне рамок этой фазы (требуют отдельных задач: апдейт зависимостей, новая
  monochrome-иконка).
- Финальный визуальный проход по всем экранам в эмуляторе (light + dark) —
  **не выполнен**: эмулятор/adb недоступны в текущем окружении. Корректность
  тёмной темы проверена статически (grep-аудит выше) и сборкой/линтом; реальный
  визуальный прогон остаётся открытым пунктом.
- `./gradlew assembleDebug` — успешно. `./gradlew lint` — успешно (BUILD
  SUCCESSFUL, остались только warning'и, перечисленные выше).

**Деплой:**
- Debug APK (релизной подписи в проекте нет) залит на VPS `server-main`
  (72.56.77.253) и раздаётся nginx'ом по прямой ссылке:
  `https://agro.assaru.space/downloads/agromarket-debug.apk`.
- На хосте добавлен `location /downloads/` (alias на
  `/var/www/agromarket-downloads/`, `autoindex off`) в
  `/etc/nginx/sites-enabled/agro.assaru.space`, конфиг провалидирован
  (`nginx -t`) и применён (`systemctl reload nginx`). Бэкап исходного
  конфига — `/root/nginx-backups/`.
- `agroprompis.tw1.ru` для деплоя не используется — актуальный домен бэкенда
  совпадает с `API_BASE_URL` (`agro.assaru.space`).

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
  стандартная M3-шкала на брендовых шрифтах [Font.kt](../app/src/main/java/ru/agromarket/ui/theme/Font.kt) —
  `PlusJakartaSans` (вариативный `.ttf`, `res/font/plus_jakarta_sans.ttf`) для всей шкалы и
  `JetBrainsMono` (`res/font/jetbrains_mono.ttf`) для цены в `AdCard`
- **Icon-set**: остаёмся на Material Symbols Outlined — `material-icons-extended` уже подключена
  (build.gradle.kts:60), сторонний icon-set не нужен

`./gradlew assembleDebug` после Фазы 0 — успешно.

## Открытые вопросы (требуют решения)

- ~~Нужна ли новая иконка приложения от дизайнера или генерировать программно (vector)~~ —
  решено в Фазе 7: иконка сгенерирована из готового PNG-макета (растровый foreground).
