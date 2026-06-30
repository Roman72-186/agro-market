package ru.agromarket.di

import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agromarket.data.api.AgroMarketApi
import ru.agromarket.data.payment.PaymentGateway
import ru.agromarket.data.payment.SimulatedPaymentGateway
import ru.agromarket.data.repository.AgroRepository

/**
 * Koin-модуль data-слоя — только то, что физически лежит в commonMain (без платформенных типов).
 *
 * Платформенные зависимости (HttpClient/HttpClientEngine, TokenProvider, PushTokenProvider,
 * TokenManager/CryptoManager/AdDraftManager) и все ViewModel-биндинги — в [androidModule].
 * Конструкторные ссылки (`singleOf`) выбраны намеренно: они позволяют `verify()` reflective-но
 * прочитать параметры конструктора и реально поймать недостающий биндинг (lambda-форму verify
 * может пропустить).
 */
val commonModule: Module = module {
    singleOf(::AgroMarketApi)
    singleOf(::AgroRepository)
    // Шов оплаты: экраны boost/subscription зависят только от интерфейса PaymentGateway,
    // смена провайдера = смена этого биндинга.
    singleOf(::SimulatedPaymentGateway) bind PaymentGateway::class
}
