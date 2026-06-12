# Дизайн-ресёрч AgroMarket — 2026-06-12

Этап 2 аудита (см. [audit-2026-06-12.md](audit-2026-06-12.md)): аналоги среди веб- и
мобильных приложений, источники вдохновения (Pinterest, Dribbble, Behance), выводы для нашего UI.

## 1. Прямые аналоги — российские агро-платформы

| Платформа | Что взять |
|---|---|
| [Своё Фермерство](https://svoefermerstvo.ru/) (РСХБ) | Главный отечественный референс: агрегатор-модель «покупатель формирует запрос → продавцы откликаются» — ровно наш паттерн «Запросить контакты». Смотреть, как они оформляют карточку запроса и статусы |
| Своё Родное (РСХБ) | Витрина фермерских продуктов: простая подача карточек, бесплатный «магазин» фермера — паттерн для будущего публичного профиля продавца |
| [Обзор: топ-9 приложений для АПК](https://svoefermerstvo.ru/svoemedia/articles/top-9-rossijskih-mobil-nyh-prilozhenij-dlja-apk), [топ-11](https://svoefermerstvo.ru/svoemedia/articles/top-11-rossijskih-prilozhenij-dlja-apk) | Карта конкурентного поля АПК-приложений |
| [13 агро-маркетплейсов](https://xn----dtbhaacat8bfloi8h.xn--p1ai/Marketplaces-agro-rb-2023), [обзор sellermarket](https://sellermarket.ru/blog/1720-luchshie-marketplejsy-dlya-prodazhi-selskohozyajstvennyh-tovarov) | Yorso, Agrisale, Ешь Деревенское и др. — ниши и позиционирование |
| [Агроинвестор: маркетплейс для фермера](https://www.agroinvestor.ru/analytics/article/38911-marketpleys-dlya-fermera-stanut-li-agrarnye-platformy-nezamenimym-instrumentom-dlya-fermerov/) | Аналитика: почему фермеры (не) доверяют платформам — аргументы для онбординга |

## 2. Генеральные доски объявлений (паттерны листинга)

| Платформа | Что взять |
|---|---|
| Авито | Эталон РФ: карточка в листинге (фото-доминанта, цена жирно, бейдж продвижения), сохранённые поиски, фильтр-чипы. У нас `boostLevel` уже приходит с бэка — в UI не используется, у Авито подсмотреть подачу «продвинутых» объявлений |
| [Юла](https://play.google.com/store/apps/details?id=com.allgoritm.youla&hl=en_US) | Гео-первый листинг («рядом с вами»), простая публикация. Их [история падения](https://irecommend.ru/content/prilozhenie-yula-ili-besplatnye-obyavleniya-na-avito-teper-avito-platnoe-yula-nachala-za-zdr) — антикейс монетизации, давящей на UX |
| [Как сделать приложение типа Авито/Юлы (Purrweb)](https://www.purrweb.com/ru/blog/kak-razrabotat-prilozhenie-pohozhee-na-avito-i-yulu/) | Разбор обязательных флоу классифайда по экранам |
| [173 гайдлайна карточек листинга](https://hardclient.com/ecommerce-product-listing-cards) | Чек-лист по нашим `AdCard`/`FavoriteRow`: иерархия цены, бейджей, фото |

## 3. Западные агро-классифайды (узкая ниша = наш кейс)

| Платформа | Что взять |
|---|---|
| [Agriaffaires](https://www.agriaffaires.us/) | Крупнейший агро-классифайд Европы: «Обсерватория цен» (средняя цена по модели техники) — сильная идея для доверия к цене; фасетные фильтры по марке/модели/году |
| [TractorHouse](https://www.tractorhouse.com/) ([iOS-приложение](https://apps.apple.com/us/app/tractorhouse-farm-equipment/id380177623)) | Мобильный паттерн поиска техники: фильтры → сохранённый поиск → пуш о новых объявлениях |
| [Machinery Pete](https://www.machinerypete.com/) | «Найди рядом» + популярные бренды как быстрые входы — аналог наших quick-категорий |
| [Каталог агро-приложений Farms.com](https://m.farms.com/agriculture-apps/machinery) | Длинный хвост нишевых приложений для насмотренности |

## 4. Визуальное вдохновение — Pinterest / Dribbble / Behance

**Dribbble:**
- [agriculture-app](https://dribbble.com/tags/agriculture-app) (≈80 работ), [agriculture-mobile-app](https://dribbble.com/tags/agriculture-mobile-app), [поиск agriculture mobile app](https://dribbble.com/search/agriculture-mobile-app)
- [farmers-app](https://dribbble.com/tags/farmers-app), [farmer-app](https://dribbble.com/tags/farmer-app), [farmers-market-app](https://dribbble.com/tags/farmers-market-app)
- [agriculture_marketplace_app](https://dribbble.com/tags/agriculture_marketplace_app) — самый точный тег под наш продукт

**Behance** (смотреть кейсы целиком, у них есть и флоу, и дизайн-системы):
- [Agriculture app](https://www.behance.net/search/projects/?search=Agriculture+app) — в выдаче: Noah – Farmer Marketplace (2.1K лайков), AgroLink, Agriplant (UX-кейс)
- [Farmer app design](https://www.behance.net/search/projects/farmer%20app%20design), [Farming app](https://www.behance.net/search/projects?search=Farming+app)

**Pinterest:**
- [Marketplace app design](https://www.pinterest.com/ideas/marketplace-app-design/898174250022/), [доска Marketplace UI](https://www.pinterest.com/chrisklamut/marketplace-ui/)
- [Olive green branding palettes](https://www.pinterest.com/ideas/olive-green-branding-color-palettes/929455672354/), [Green app color palette](https://www.pinterest.com/ideas/green-app-color-palette/919195545803/) — прямое попадание в нашу «Глину и Оливу»
- Инструменты: [Coolors](https://coolors.co/), [100 палитр Figma](https://www.figma.com/resource-library/color-combinations/)

## 5. Выводы для AgroMarket (связь с аудитом кода)

1. **Палитра «Глина и Олива» — в тренде.** Earthy/olive — устойчивый стиль для агро и эко.
   Менять не надо; добивать контрастом и фото-доминантой в карточках.
2. **Сердечко в листинге — стандарт рынка** (Авито, Юла, TractorHouse). У нас `AdCard` уже
   умеет (`onFavoriteClick`), осталось пробросить из фида — п.3.3 аудита.
3. **Кликабельный телефон/CTA-связь — стандарт** во всех классифайдах. П.3.2 аудита.
4. **Подача boost-объявлений:** бэк уже шлёт `boostLevel`, UI игнорирует. У Авито это бейдж
   и позиция в выдаче — задел на монетизацию.
5. **Сохранённый поиск + пуш о новых** (TractorHouse) — главный retention-механизм нишевого
   классифайда; кандидат в роадмап после базовых фиксов.
6. **«Обсерватория цен»** (Agriaffaires) — средняя цена по категории/региону как бейдж
   «цена ниже рынка». Сильный дифференциатор в перспективе.
7. **Модель «запрос контактов» = модель «Своего Фермерства»** — мы в правильной парадигме
   для b2b-агро; стоит докрутить экран истории запросов (`getMyContactRequests` уже в API).
