# Глубокий дизайн-ресёрч AgroMarket — 2026-06-12

Продолжение [design-research-2026-06-12.md](design-research-2026-06-12.md) (этап 2 аудита):
разбор по 5 ключевым экранам с аналогами, паттернами и анти-паттернами. Дизайн-решения
учитывают проблемы интерактива из [audit-2026-06-12.md](audit-2026-06-12.md), секции 3–5.

Палитра проекта: «Глина и Олива» — primary оливковый `#63722E`, акцент глиняный `#8E7544`,
фон `#F5F7F2`, цены — JetBrains Mono.

---

## 1. Экран фида (поиск / фильтры / quick-категории / список объявлений)

### 1.1 Аналоги

| Источник | Как решён фид | Ссылка |
|---|---|---|
| Авито | Поисковая строка сверху, под ней горизонтальные категории-входы, лента карточек 2 колонки: фото-доминанта, цена жирно первой строкой, сердечко в углу фото. Продвинутые объявления — выделение цены цветом (на Android жёлтым) и XL-блоки | [Разбор подачи и выдачи (eLama)](https://elama.ru/blog/strategiya-prodvizheniya-na-avito-kak-sdelat-obyavlenie-zametnee-i-poluchat-bolshe-prosmotrov/), [chego.digital](https://chego.digital/blog/tpost/mxk4th7s81-zametnee-vishe-silnee-kak-sdelat-obyavle) |
| Юла | Гео-первая лента «рядом с вами», расстояние до товара прямо в карточке листинга; минимум текста, крупное фото | [Google Play](https://play.google.com/store/apps/details?id=com.allgoritm.youla&hl=en_US) |
| Своё Фермерство | Витрина-агрегатор: категории как крупные плитки-входы, режим «запрос → отклики продавцов» вместо бесконечной ленты | [svoefermerstvo.ru](https://svoefermerstvo.ru/) |
| Agriaffaires | Фасетный поиск как главный сценарий: марка → модель → год → цена; «top of list»-иконка у поднятых объявлений; сортировка по цене/расстоянию | [agriaffaires.us](https://www.agriaffaires.us/), [Options and services](https://www.agriaffaires.us/help-contact/service-options.html) |
| TractorHouse | Фильтры по категории/марке/модели/локации/мощности/году, сохранённый поиск + текстовые/email-алерты о новых объявлениях — главный retention-механизм | [Google Play](https://play.google.com/store/apps/details?id=sandhills.tractorhouse.app&hl=en_US), [How-to](https://www.tractorhouse.com/info/how-to) |
| Концепт: Farmers Marketplace App — Abdellah Askane | Чистый фид фермерского маркетплейса: карточки с большим фото, мягкие скругления, тёплая палитра — близко к нашей | [dribbble.com/shots/22494311](https://dribbble.com/shots/22494311-Farmers-Marketplace-App) |
| Концепт: Agrikore Marketplace — Victor Shiwani | B2B-агромаркетплейс: категории как цветные плитки, акцент на объёмы/партии | [dribbble.com/shots/13820973](https://dribbble.com/shots/13820973-Agrikore-Marketplace) |
| Концепт: Home & Search Flow — Vektora | Эталонный флоу «главная → поиск → выдача» маркетплейса: поиск с подсказками, чипы недавних запросов | [dribbble.com/shots/21836932](https://dribbble.com/shots/21836932-Home-and-Search-Flow-Fashion-Marketplace-App) |

### 1.2 Паттерны, которые стоит перенять

1. **Сердечко прямо в карточке листинга** (Авито, Юла, TractorHouse Watch List).
   Почему работает: сохранение в один тап без захода в карточку — самый дешёвый
   micro-commitment, наполняет избранное и retention-петлю.
   Адаптация: `AdCard` уже умеет (`isFavorite`/`onFavoriteClick` — п.3.3 аудита), пробросить
   из `FeedScreen`. Залитое сердечко — оливковый `#63722E` на белой подложке-кружке.
2. **Видимые применённые фильтры** ([LogRocket: mobile filter UX](https://blog.logrocket.com/ux-design/best-practices-mobile-search-filter/)).
   Почему работает: пользователь применяет фильтр и забывает о нём — потом не понимает,
   почему выдача «пустая». Адаптация: ряд removable-чипов под поиском (Material 3
   InputChip с крестиком), фон чипа — олива 12% opacity, текст `#63722E`.
3. **Расстояние до товара в карточке** (Юла, Agriaffaires «сортировка по расстоянию»).
   Почему работает: для техники и животных логистика — половина решения о покупке.
   Адаптация: вторая строка карточки «регион · N км», иконка геопина из общего icon-set.
4. **Сохранённый поиск с алертами** (TractorHouse: [Watch Lists](https://www.tractorhouse.com/blog/how-to-and-tips/2025/01/how-to-use-tractorhouse-watch-lists)).
   Почему работает: в нишевом классифайде нужного лота может не быть сегодня — подписка
   на запрос возвращает пользователя без усилий. Адаптация: кнопка «Сохранить поиск»
   в шторке фильтров; роадмап после базовых фиксов (нужен бэк).
5. **Состояние ошибки с Retry — отдельный экранный стейт**, не пустая лента.
   Почему работает: п.3.1 аудита — сейчас при ошибке сети юзер видит «Объявлений пока
   нет» (тихий отказ). Адаптация: переиспользовать `ErrorBanner`/будущий общий
   `ErrorState(message, onRetry)` из п.4.2 аудита.

### 1.3 Анти-паттерны

- **Пустое состояние вместо ошибки** — наш текущий баг (п.3.1 аудита): пользователь
  решает, что товаров нет, и уходит.
- **Сброс фильтров/позиции скролла при возврате из карточки** — частая жалоба на
  классифайды; в Compose решается сохранением `LazyListState` во ViewModel.
- **Монетизация, давящая на бесплатный сценарий** — антикейс Юлы: платные просмотры
  контактов и лимиты убили доверие аудитории ([отзыв-разбор](https://irecommend.ru/content/prilozhenie-yula-ili-besplatnye-obyavleniya-na-avito-teper-avito-platnoe-yula-nachala-za-zdr)).
  Boost-бейджи — да, paywall на связь — нет.
- **Перегруз ленты промо-блоками**: у Авито в выдаче до трети — реклама и платные
  блоки, что регулярно всплывает в негативных отзывах. Держать долю boosted-карточек
  в ленте визуально «тихой» (бейдж, а не кричащая рамка).

---

## 2. Карточка объявления (AdDetail)

### 2.1 Аналоги

| Источник | Как решён экран | Ссылка |
|---|---|---|
| Авито | Галерея на всю ширину со счётчиком «3/12», цена крупно сразу под галереей, блок продавца с рейтингом и бейджем «Документы проверены», sticky-низ с CTA «Позвонить»/«Написать» | [Что формирует доверие на Авито (ECOMHUB)](https://ecomhub.ru/russia-ecommerce-avito-trust-seller-verification-rating-reviews-online-marketplace/) |
| Юла | Фото-доминанта + быстрые действия (звонок/чат) закреплены снизу; расстояние до продавца у заголовка | [Google Play](https://play.google.com/store/apps/details?id=com.allgoritm.youla&hl=en_US) |
| Своё Фермерство | Карточка запроса со статусами откликов — наш паттерн «Запросить контакты»; характеристики таблицей «параметр — значение» | [svoefermerstvo.ru](https://svoefermerstvo.ru/) |
| Agriaffaires | Спеки техники структурированной таблицей (марка/модель/год/моточасы), «Обсерватория цен» — средняя цена по модели прямо в карточке как якорь доверия | [agriaffaires.us](https://www.agriaffaires.us/) |
| TractorHouse | Детальные фото и видео техники, спецификации; кнопка добавления в Watch List на карточке | [How-to](https://www.tractorhouse.com/info/how-to) |
| Концепт: Buy/Sell P2P Marketplace — Inayat Naqvi | Карточка P2P-объявления: продавец-блок с аватаром и рейтингом, sticky CTA | [dribbble.com/shots/21046848](https://dribbble.com/shots/21046848-Buy-Sell-P2P-Marketplace-App-UI) |
| Кейс: Noah — Farmer Marketplace (Ronas IT, 2.1K лайков) | Полный флоу фермерского маркетплейса, включая карточку товара с фермером-продавцом и earthy-палитрой | [behance.net/gallery/125935261](https://www.behance.net/gallery/125935261/Noah-Farmer-Marketplace-Mobile-App-UIUX) |

### 2.2 Паттерны

1. **Sticky CTA-бар внизу** («Позвонить» + «Запросить контакты»).
   Почему работает: по данным A/B-тестов sticky-блок покупки даёт значимый рост
   конверсии, CTA никогда не нужно «искать» ([ConvertCart](https://www.convertcart.com/blog/product-page-ux-design), [scandiweb](https://scandiweb.com/blog/best-practices-for-product-detail-pages/)).
   Адаптация: `Surface` с tonal elevation поверх скролла; primary-кнопка оливковая,
   secondary — outlined глина.
2. **Кликабельный телефон** (`Intent.ACTION_DIAL`) — стандарт всех классифайдов,
   у нас телефоны выглядят как контакт, но не звонят (п.3.2 аудита). `ACTION_DIAL`
   не требует разрешений.
3. **Счётчик фото + полноэкранный просмотр с зумом**. Почему работает: Baymard —
   50–80% покупателей не доглядывают фото, если миниатюры обрезаны; для б/у техники
   фото — фактор доверия №1 (63% покупателей Авито смотрят прежде всего реальные фото —
   [ECOMHUB](https://ecomhub.ru/russia-ecommerce-avito-trust-seller-verification-rating-reviews-online-marketplace/)).
   Адаптация: `HorizontalPager` + бейдж «N/M» в углу (глина 80% opacity, белый текст).
4. **Характеристики таблицей «параметр — значение»** (Agriaffaires, TractorHouse).
   Почему работает: техника покупается по спекам; таблица сканируется быстрее
   сплошного описания. Адаптация: две колонки, лейбл `onSurfaceVariant`, значение
   `onSurface`; числовые значения — JetBrains Mono.
5. **Блок продавца с маркерами доверия** (Авито: рейтинг + «Документы проверены»;
   >90% выбирают продавца со значком проверки при прочих равных —
   [d-economy](https://d-economy.ru/news/issledovanie-avito-pochti-70-pokupatelej-doverjajut-verifikacii-cherez-gosuslugi-a-38-prodavcov-prohodjat-proverku-s-pomoshhju-pasporta/)).
   Адаптация: пока без рейтинга — имя, дата регистрации, число объявлений; задел
   под бейдж верификации.

### 2.3 Анти-паттерны

- **Телефон-как-текст** — наш текущий баг (п.3.2 аудита): выглядит интерактивным,
  не реагирует — классический «false affordance».
- **Обрезанные миниатюры галереи со стрелками** — Baymard: большинство пользователей
  не видит остальные фото ([15 pitfalls](https://www.linkedin.com/posts/baymard-institute_product-page-ux-2025-15-pitfalls-and-best-activity-7285002885770182656-N_YX)).
- **CTA, который просто скроллит к секции покупки** — формат имеет значение, нужен
  настоящий sticky-бар, а не якорь (Baymard, там же).
- **«Цена по запросу»** — в агро-классифайдах распространена и стабильно снижает
  отклик: Agriaffaires борется с ней «Обсерваторией цен». Если бэк позволяет —
  поощрять указание цены на этапе подачи, а не прятать её в карточке.

---

## 3. Создание объявления (4-шаговый визард)

### 3.1 Аналоги

| Источник | Как решён флоу | Ссылка |
|---|---|---|
| Авито | Пошаговая подача: категория (с автоопределением по названию) → параметры → фото → цена/контакты; черновик сохраняется автоматически; поля открываются постепенно | [Пошаговая инструкция (eLama)](https://elama.ru/blog/kak-podat-obyavlenie-naavito-poshagovaya-instrukciya-dlya-biznesa/), [inSales](https://www.insales.ru/blogs/university/kak-podat-obyavlenie-na-avito) |
| Юла | Подача «в пару экранов»: фото первым шагом (с камеры сразу), категория угадывается; минимум обязательных полей | [Google Play](https://play.google.com/store/apps/details?id=com.allgoritm.youla&hl=en_US) |
| Agriaffaires | Подача техники через структурированные спеки: марка/модель из справочников, а не свободным текстом — данные сразу пригодны для фасетных фильтров | [Charter](https://www.agriaffaires.us/service/control-validation.html) |
| TractorHouse | Подача через категорийные формы с обязательными спеками (год, мощность, наработка) | [How-to](https://www.tractorhouse.com/info/how-to) |
| Концепт: Buy/Sell Marketplace Freebie — Kishore | Флоу «продать вещь»: шаги фото → описание → цена с крупным степпером | [dribbble.com/shots/5252805](https://dribbble.com/shots/5252805-Buy-Sell-Marketplace-App-Freebie-Day-257-365-Project365) |
| Кейс: Agriplant — UX-кейс агро-приложения | Формы агро-домена: подбор культуры/параметров пошагово, иллюстрированные пустые состояния | [behance.net/gallery/168782889](https://www.behance.net/gallery/168782889/Agriplant-Agriculture-UIUX-Case-Study-Mobile-App) |

Гайды по степперам и многошаговым формам: [Eleken: 32 stepper UI examples](https://www.eleken.co/blog-posts/stepper-ui-examples),
[UXPin: progress trackers](https://www.uxpin.com/studio/blog/design-progress-trackers/),
[Lollypop: wizard UI](https://lollypop.design/blog/2026/january/wizard-ui-design/),
[Growform: multi-step form UX](https://www.growform.co/must-follow-ux-best-practices-when-designing-a-multi-step-form/),
[Mobbin: progress indicator glossary](https://mobbin.com/glossary/progress-indicator).

### 3.2 Паттерны

1. **Явный степпер «Шаг X из 4» с подписями состояний.** Почему работает: индикатор
   прогресса с тремя состояниями (пройдено/текущее/впереди) даёт двузначный прирост
   completion rate (UXPin). Адаптация: тонкий progress-бар под `AppTopBar`: пройдено —
   олива `#63722E`, текущий сегмент — глина `#8E7544`, впереди — `surfaceVariant`;
   плюс текст «Шаг 2 из 4 · Фото».
2. **3–5 полей на шаг** (Growform/Webstacks). У нас `CreateAdFormStep` перегружен:
   каскад из трёх гео-пикеров + остальные поля. Адаптация: вынести гео в собственный
   подшаг или в один `PickerField` «Местоположение», открывающий каскад шторкой
   (заодно закрывает дубль из п.4.4 аудита; в API уже есть `searchLocality` —
   поиск строкой вместо каскада).
3. **Автосохранение черновика** (Авито). Почему работает: подача с фото — длинный
   сценарий, обрыв = потерянное объявление и потерянный листинг для платформы.
   Адаптация: сериализовать стейт визарда в DataStore, при входе предлагать
   «Продолжить черновик».
4. **Фото первым классом**: сетка добавленных фото с возможностью назначить обложку
   и подсказкой «Объявления с 3+ фото получают больше откликов» (паттерн Авито/Юлы).
   Числовая подсказка — мягкий нудж, не блокер.
5. **Справочные значения вместо свободного текста** для спеков техники (Agriaffaires):
   марка/год/состояние — пикеры. Сразу делает будущие фасетные фильтры возможными.

### 3.3 Анти-паттерны

- **Степпер без меток и количества шагов** — пользователь не знает, сколько осталось,
  и бросает (UXPin/Lollypop: rigid steppers без объяснения блокировки — главный
  источник drop-off).
- **Потеря введённого при выходе/повороте экрана** — смертельно для длинной формы
  с фото; черновик обязателен.
- **Блокировка кнопки «Далее» без объяснения причины** — валидация должна подсвечивать
  конкретное поле, а не молча дизейблить CTA (Lollypop).
- **Каскад обязательных пикеров без поиска** — три последовательных диалога
  (регион → район → пункт) на каждый раз; у нас уже есть `searchLocality` в API
  (п.2 аудита) — поиск строкой быстрее каскада.

---

## 4. Избранное

### 4.1 Аналоги

| Источник | Как решён экран | Ссылка |
|---|---|---|
| Авито | Избранное с вкладками (товары/поиски/продавцы), пометка «снято с публикации» у умерших объявлений, уведомление о снижении цены | [eLama: разбор функций](https://elama.ru/blog/strategiya-prodvizheniya-na-avito-kak-sdelat-obyavlenie-zametnee-i-poluchat-bolshe-prosmotrov/) |
| Юла | Простое избранное-список; сердечко доступно из ленты | [Google Play](https://play.google.com/store/apps/details?id=com.allgoritm.youla&hl=en_US) |
| TractorHouse | Watch List: приватный список техники + еженедельные email-апдейты по нему; рядом — «Want-to-Buy» объявление как обратный сценарий | [Watch Lists blog](https://www.tractorhouse.com/blog/how-to-and-tips/2025/01/how-to-use-tractorhouse-watch-lists) |
| Agriaffaires | Сохранённые объявления + сохранённые поиски как парные сущности | [agriaffaires.us](https://www.agriaffaires.us/) |
| Гайды | Дизайн wishlist в e-commerce: терминология, доступ, организация | [TheStory: Wishlists Design](https://thestory.is/en/journal/designing-wishlists-in-e-commerce/), [UX Chap: Hearts Don't Lie](https://medium.com/the-ux-chap/hearts-dont-lie-the-importance-of-favouriting-in-e-commerce-82d14d1c196f), [Mobiscroll: UI for Favorites](https://blog.mobiscroll.com/ui-for-favorites/) |
| Кейс: Shopee Wishlist UX | Разбор улучшения избранного в большом маркетплейсе: статусы товара, сортировка, напоминания | [Medium: Shopee case study](https://medium.com/@fadhil.ibrhm12/how-to-improve-the-experience-of-wishlist-feature-in-e-commerce-app-shopee-ux-case-study-eaa0e97ffca1) |

### 4.2 Паттерны

1. **Статус объявления прямо в строке избранного.** Почему работает: главная боль
   избранного в классифайде — лоты умирают; пользователь должен видеть «Продано»/
   «Снято» без перехода. Адаптация: у нас есть `StatusBadge` — показывать его
   в `FavoriteRow`, неактивные лоты приглушать (`alpha 0.6`) и группировать вниз.
2. **Снижение цены как событие** (Авито). Почему работает: повод вернуться + триггер
   покупки. Адаптация: хранить цену на момент добавления; при расхождении — зелёная
   стрелка вниз и старая цена зачёркнутой. Пуш — роадмап.
3. **Свайп для удаления + Undo-снекбар** (стандарт Material). Почему работает:
   чистка списка без режима редактирования; Undo страхует от случайного свайпа.
4. **Пустое состояние, продающее сердечко**: иллюстрация + «Нажимайте ♥ в ленте,
   чтобы сохранять объявления» + кнопка «К объявлениям» (паттерн из wishlist-гайдов
   TheStory/Mobiscroll). У нас уже есть `EmptyState` — добавить CTA-переход на фид.
5. **Пара «избранные объявления + сохранённые поиски»** (Agriaffaires, Авито) —
   когда появятся сохранённые поиски, им место вкладкой здесь, а не в фильтрах.

### 4.3 Анти-паттерны

- **Мёртвые лоты без пометки** — пользователь открывает карточку и получает ошибку;
  Shopee-кейс называет это главным разочарованием раздела.
- **Избранное только через карточку** — наш текущий баг (п.3.3 аудита): сердечко
  из ленты не работает, порог наполнения избранного завышен.
- **Требовать логин на «добавить в избранное» без объяснения** — допустимо требовать
  авторизацию, но с понятным экраном-приглашением, а не молчаливым отказом
  (TheStory: guest wishlist повышает наполнение).

---

## 5. Профиль

### 5.1 Аналоги

| Источник | Как решён экран | Ссылка |
|---|---|---|
| Авито | Профиль = центр управления: «Мои объявления» со статусами (активно/на проверке/отклонено) и действиями (редактировать/снять/продвинуть); бейджи верификации «Документы проверены»/«Реквизиты проверены» (36% покупателей проверяют их) | [ECOMHUB](https://ecomhub.ru/russia-ecommerce-avito-trust-seller-verification-rating-reviews-online-marketplace/), [CNews: верификация](https://www.cnews.ru/news/line/2023-12-08_proverka_cherez_bankovskie) |
| Юла | Профиль с рейтингом продавца и «достижениями»; публичный профиль = витрина всех объявлений юзера | [Google Play](https://play.google.com/store/apps/details?id=com.allgoritm.youla&hl=en_US) |
| Своё Фермерство / Своё Родное | «Магазин фермера» — публичный профиль как мини-витрина с историей хозяйства: эмоциональное доверие, не только транзакции | [svoefermerstvo.ru](https://svoefermerstvo.ru/) |
| Agriaffaires | Профиль дилера: логотип, парк техники списком, телефоны; частник — минимальный | [agriaffaires.us](https://www.agriaffaires.us/) |
| TractorHouse | Кабинет: мои объявления + сохранённые поиски + алерты в одном меню | [How-to](https://www.tractorhouse.com/info/how-to) |
| Кейс: Noah — Farmer Marketplace (Ronas IT) | Профиль фермера с фото хозяйства и списком товаров — образец «тёплого» публичного профиля | [behance.net/gallery/125935261](https://www.behance.net/gallery/125935261/Noah-Farmer-Marketplace-Mobile-App-UIUX) |

### 5.2 Паттерны

1. **«Мои объявления» = управление, а не просмотр.** Почему работает: у нас тап
   ведёт на публичный `AdDetail` без действий (п.3.4 аудита) — владелец не может
   ни удалить, ни отредактировать. Адаптация: меню-кебаб в строке (`MyAdRow` →
   общий `AdRow` из п.4.3 аудита) с «Редактировать» (когда появится экран,
   `updateAd` уже в API) и «Удалить» с подтверждением (`deleteAd` готов
   в репозитории).
2. **Статус модерации/публикации на каждом своём объявлении** (Авито). У нас есть
   `StatusBadge` (`adStatusBadge`) — выводить его в «Моих объявлениях» обязательно:
   продавец должен видеть «на проверке»/«отклонено» без сапорта.
3. **Бейдж верификации как главный маркер доверия** (Авито: >90% выбирают
   проверенного продавца). Адаптация: задел в UI — иконка-щит оливкового цвета
   у имени; когда бэк даст верификацию, место уже есть.
4. **Счётчики-входы**: «Объявлений · Активных · Запросов контактов» строкой чипов —
   и навигация, и статистика (паттерн кабинетов Авито/TractorHouse). `getMyContactRequests`
   уже есть в API (п.2 аудита) — экран истории запросов просится сюда.
5. **Разделение «личный кабинет» и «публичный профиль»** (Юла, Своё Родное):
   проектировать текущий экран как кабинет, а публичную витрину продавца — отдельным
   будущим экраном, не смешивать.

### 5.3 Анти-паттерны

- **Свои объявления без действий владельца** — наш текущий баг (п.3.4 аудита).
- **Скрытые статусы модерации** — продавец не понимает, почему объявление не видно,
  и пишет в поддержку или уходит.
- **Геймификация ради геймификации** (значки Юлы без влияния на сделку) — в b2b-агро
  доверие строится на верификации и реальных фото, не на «ачивках».

---

## 6. Подборка референсов под «Глину и Оливу»

| Ресурс | Что там | Ссылка |
|---|---|---|
| Pinterest: Green app color palette | Подборка идей зелёных палитр приложений — наш диапазон | [pinterest.com/ideas/green-app-color-palette](https://www.pinterest.com/ideas/green-app-color-palette/919195545803/) |
| Pinterest: доска «UI Inspiration: Green» (Nicholas Tenhue) | ~380 пинов живых зелёных интерфейсов | [pinterest.com/nicholastenhue/ui-inspiration-green](https://www.pinterest.com/nicholastenhue/ui-inspiration-green/) |
| Pinterest: доска «Green App Design UI» | Компактная курируемая доска зелёных приложений | [pinterest.com/raydawg88/green-app-design-ui](https://www.pinterest.com/raydawg88/green-app-design-ui/) |
| Pinterest: доска «Marketplace UI» (Chris Klamut) | Карточки, фиды, профили маркетплейсов | [pinterest.com/chrisklamut/marketplace-ui](https://www.pinterest.com/chrisklamut/marketplace-ui/) |
| Mobbin: Product Detail screens | Реальные карточки товара из живых приложений — лучший источник для AdDetail | [mobbin.com/explore/mobile/screens/product-detail](https://mobbin.com/explore/mobile/screens/product-detail) |
| Mobbin: Shop/Storefront screens | Фиды и витрины живых приложений | [mobbin.com/explore/mobile/screens/shop-storefront](https://mobbin.com/explore/mobile/screens/shop-storefront) |
| Олив-палитры с hex | 20 готовых olive-комбинаций (терракота, горчица, крем — родня нашей глине) | [media.io: olive green palettes](https://www.media.io/color-palette/olive-green-color-palette.html), [dark olive](https://www.media.io/color-palette/dark-olive-green-color-palette.html) |
| Dribbble: Farmers Marketplace App | Earthy-фид, прямое попадание в палитру | [dribbble.com/shots/22494311](https://dribbble.com/shots/22494311-Farmers-Marketplace-App) |
| Dribbble: Agriculture Assistant (Conceptzilla) | Оливково-кремовый агро-концепт, типографика и иконки | [dribbble.com/shots/18743673](https://dribbble.com/shots/18743673-Agriculture-Assistant-App-Design-Concept) |
| Dribbble: TieUp Farming | Агро web+mobile дизайн-система | [dribbble.com/shots/22483307](https://dribbble.com/shots/22483307-TieUp-Farming-UI-UX-design-for-Agriculture-Web-Mobile-App) |
| Behance: Noah — Farmer Marketplace | Самый полный earthy-кейс маркетплейса (Ronas IT) | [behance.net/gallery/125935261](https://www.behance.net/gallery/125935261/Noah-Farmer-Marketplace-Mobile-App-UIUX) |
| Behance: Agriplant UX-кейс | Агро-приложение с полным UX-процессом | [behance.net/gallery/168782889](https://www.behance.net/gallery/168782889/Agriplant-Agriculture-UIUX-Case-Study-Mobile-App) |

Вывод по палитре: тренд подтверждается — olive + терракота/горчица/крем читается как
«зрелое и надёжное» и устойчиво используется в эко/агро ([Ramotion: app palettes](https://www.ramotion.com/blog/app-design-color-palette/)).
Наша пара олива `#63722E` + глина `#8E7544` в этом каноне; добавить в токены
крем/терракоту для бейджей и иллюстраций пустых состояний.

## 7. Типографика и иконография агро-тематики

**Типографика.**
- Аналоги (Авито, Agriaffaires, TractorHouse) используют нейтральные гротески —
  фирменный шрифт Авито, system-стек у западных. Для агро-классифайда это правильно:
  «фермерскую» рукописность ([MyFonts: agriculture fonts](https://www.myfonts.com/pages/tags/agriculture-fonts/))
  оставлять только логотипу, не UI.
- Наш ход с JetBrains Mono для цен — удачный дифференциатор: моноширинные цифры
  выравнивают прайс-колонку в листинге. Расширить на все числовые данные:
  моточасы, год, площадь, телефоны в карточке.
- Иерархия по гайдам дизайн-систем ([designsystems.com: typography](https://www.designsystems.com/typography-guides/)):
  в карточке листинга цена — самый тяжёлый элемент (titleLarge/Bold), заголовок —
  bodyLarge, мета (регион, дата) — bodySmall `onSurfaceVariant`.

**Иконография.**
- Агро-категории требуют специфичных метафор (трактор, колос, копыто, поле) —
  в Material Icons их нет. Готовые консистентные line-наборы:
  [Icons8: agriculture set](https://icons8.com/icons/set/agriculture),
  [Flaticon: 55K+ agriculture icons](https://www.flaticon.com/free-icons/agriculture),
  [Flaticon: farm icon fonts](https://www.flaticon.com/free-icon-fonts/farm-agriculture),
  [Font Awesome: farm](https://fontawesome.com/icons/farm).
- Правило: один набор, один stroke weight (рекомендую outline 1.5–2dp под Material 3),
  цвет — олива для активных, `onSurfaceVariant` для пассивных. Сейчас у нас
  `categoryIconFor()` — проверить, что все категории из одного семейства.
- Концепты-образцы консистентной агро-иконографики: [Conceptzilla](https://dribbble.com/shots/18743673-Agriculture-Assistant-App-Design-Concept), [TieUp Farming](https://dribbble.com/shots/22483307-TieUp-Farming-UI-UX-design-for-Agriculture-Web-Mobile-App).

## 8. Бейджи продвижения (дизайн под `boostLevel`)

Как подают boosted-объявления аналоги:

| Платформа | Подача | Источник |
|---|---|---|
| Авито | Не «бейдж "реклама"», а усиление самой карточки: выделение цены цветом (Android — жёлтый, ~неделя), XL-блок (3 фото + расширенное описание + кнопка телефона), значки услуг | [eLama](https://elama.ru/blog/strategiya-prodvizheniya-na-avito-kak-sdelat-obyavlenie-zametnee-i-poluchat-bolshe-prosmotrov/), [chego.digital](https://chego.digital/blog/tpost/mxk4th7s81-zametnee-vishe-silnee-kak-sdelat-obyavle), [vc.ru: логика продвижения](https://vc.ru/marketing/2653587-prodvizhenie-ob-yavleniy-na-avito) |
| Авито (доверие) | Параллельный «органический» бейдж «Рыночная цена» — награда за честную цену, не покупается | [eLama](https://elama.ru/blog/kak-podat-obyavlenie-naavito-poshagovaya-instrukciya-dlya-biznesa/) |
| Agriaffaires | «Top of list»: объявление поднимается, и на нём 24 часа висит иконка «top of list» — честная пометка платного поднятия | [Charter](https://www.agriaffaires.us/service/control-validation.html), [Options](https://www.agriaffaires.us/help-contact/service-options.html) |
| Юла (антикейс) | Агрессивная монетизация показов/контактов → отток | [разбор](https://irecommend.ru/content/prilozhenie-yula-ili-besplatnye-obyavleniya-na-avito-teper-avito-platnoe-yula-nachala-za-zdr) |

**Предложение для AgroMarket** (у нас `boostLevel` приходит с API и игнорируется в UI):

- `boostLevel = 1` («поднятие») — маленькая плашка «↑ В топе» в углу фото `AdCard`:
  фон глина `#8E7544`, белый текст labelSmall, скругление 6dp. Аналог
  «top of list»-иконки Agriaffaires — честно и тихо.
- `boostLevel = 2` («выделение») — то же + тонкая (1.5dp) рамка карточки цветом глины
  и фон карточки кремовый (глина 6–8% opacity). Аналог цветового выделения Авито,
  но в нашей палитре — без жёлтого, он чужой для «Глины и Оливы».
- `boostLevel = 3` («XL») — карточка на всю ширину ленты: крупное фото, 2 строки
  описания, сразу кнопка «Запросить контакты». Аналог XL-блока Авито.
- Правила: бейдж продвижения никогда не маскируется под органический («Рыночная
  цена»-подобные органические бейджи — отдельный цвет, олива); в выдаче подряд
  не больше 1 boosted-карточки на экран — урок Юлы/Авито про рекламный шум.

---

## 9. Топ-10 дизайн-изменений (приоритизировано)

| # | Экран | Что меняем | Референс | Ожидаемый эффект |
|---|---|---|---|---|
| 1 | Фид | Стейт ошибки сети: иконка + текст + «Повторить» вместо «Объявлений пока нет» (общий `ErrorState`) | П.3.1 аудита; [LogRocket](https://blog.logrocket.com/ux-design/best-practices-mobile-search-filter/) | Убирает тихий отказ — главную потерю пользователей при плохой сети |
| 2 | Карточка | Телефоны кликабельны (`ACTION_DIAL`) + sticky CTA-бар «Позвонить / Запросить контакты» | Авито/Юла; [ConvertCart: sticky CTA](https://www.convertcart.com/blog/product-page-ux-design) | Прямой рост конверсии в контакт — целевое действие приложения |
| 3 | Фид | Сердечко избранного в `AdCard` (уже поддержано компонентом) | Авито, [TractorHouse Watch List](https://www.tractorhouse.com/blog/how-to-and-tips/2025/01/how-to-use-tractorhouse-watch-lists) | Наполнение избранного → retention-петля |
| 4 | Фид | Бейджи `boostLevel` 1/2/3: плашка «↑ В топе» (глина) / рамка+крем / XL-карточка | [Agriaffaires top-of-list](https://www.agriaffaires.us/help-contact/service-options.html), [Авито XL](https://elama.ru/blog/strategiya-prodvizheniya-na-avito-kak-sdelat-obyavlenie-zametnee-i-poluchat-bolshe-prosmotrov/) | Включает монетизацию: бэк уже шлёт поле, UI начинает его продавать |
| 5 | Профиль | «Мои объявления»: `StatusBadge` статуса + кебаб-меню «Удалить» (`deleteAd` готов), задел «Редактировать» | Авито-кабинет; п.3.4 аудита | Владелец управляет лотами без сапорта; меньше мёртвых объявлений |
| 6 | Карточка | Галерея: счётчик «N/M», полноэкранный просмотр с зумом, спеки таблицей «параметр—значение» | [Baymard: 15 pitfalls](https://www.linkedin.com/posts/baymard-institute_product-page-ux-2025-15-pitfalls-and-best-activity-7285002885770182656-N_YX), Agriaffaires | Фото — фактор доверия №1 (63%, ECOMHUB); быстрее решение о контакте |
| 7 | Избранное | Статус лота в `FavoriteRow` (продано/снято — приглушение), свайп-удаление с Undo, пустое состояние с CTA «К объявлениям» | [Shopee wishlist case](https://medium.com/@fadhil.ibrhm12/how-to-improve-the-experience-of-wishlist-feature-in-e-commerce-app-shopee-ux-case-study-eaa0e97ffca1), [TheStory](https://thestory.is/en/journal/designing-wishlists-in-e-commerce/) | Избранное перестаёт «протухать»; меньше разочарований |
| 8 | Создание | Степпер «Шаг X из 4» с подписями (олива=пройдено, глина=текущий) + черновик в DataStore | [Eleken](https://www.eleken.co/blog-posts/stepper-ui-examples), [UXPin](https://www.uxpin.com/studio/blog/design-progress-trackers/), Авито-черновики | Двузначный рост completion подачи — больше листингов |
| 9 | Фид | Применённые фильтры — removable-чипы под поиском + расстояние/регион в карточке | [LogRocket](https://blog.logrocket.com/ux-design/best-practices-mobile-search-filter/), Юла | Понятная выдача, меньше «пустых» сессий с забытым фильтром |
| 10 | Все | Единый агро-icon-set (outline, один stroke) + JetBrains Mono на все числа (моточасы, год, площадь); токены крем/терракота для бейджей | [Icons8 agriculture](https://icons8.com/icons/set/agriculture), [media.io olive palettes](https://www.media.io/color-palette/olive-green-color-palette.html) | Визуальная целостность; «Глина и Олива» становится системой, а не двумя цветами |

Порядок 1–5 совпадает с приоритетами аудита (секция 5) — дизайн и техдолг закрываются
одними и теми же задачами. Пункты 6–10 — следующий слой после базовых фиксов.
