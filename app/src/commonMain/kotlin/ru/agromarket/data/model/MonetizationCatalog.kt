package ru.agromarket.data.model

/**
 * Дефолтный каталог подписок — фолбэк/мок для отложенной Pro (Фаза 2): бэкенд подписок ещё
 * не отдаёт. Boost-каталога здесь нет: цена boost живёт на бэкенде (PRICE_BOOST_7/30) и
 * показывается на форме оплаты ЮKassa.
 *
 * Цены — плейсхолдеры (market-research-competitors-2026-06-28.md), в копейках.
 */
object MonetizationCatalog {

    val DEFAULT_PLANS = SubscriptionPlansResponse(
        plans = listOf(
            SubscriptionPlan(
                code = "FREE",
                name = "Бесплатный",
                priceKopecks = 0,
                adLimit = 10,
                perks = listOf("До 10 активных объявлений", "Базовый поиск")
            ),
            SubscriptionPlan(
                code = "PRO",
                name = "Pro · Дилер",
                priceKopecks = 49_900,
                adLimit = null,
                perks = listOf(
                    "Безлимит объявлений",
                    "Размещение по нескольким регионам",
                    "Бейдж «Проверенный продавец»",
                    "Приоритет в выдаче"
                )
            ),
        )
    )
}

/** Цена в рублях для отображения: 49900 копеек → "499 ₽". */
fun Long.kopecksToRubLabel(): String {
    val rub = this / 100
    return "$rub ₽"
}
