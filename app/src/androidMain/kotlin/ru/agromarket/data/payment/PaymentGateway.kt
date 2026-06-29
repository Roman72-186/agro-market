package ru.agromarket.data.payment

import kotlinx.coroutines.delay
import ru.agromarket.data.model.SubscriptionResponse
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Платёжный «шов»: UI/ViewModel зависят только от этого интерфейса.
 *
 * Boost равняется на существующий бэкенд (ЮKassa): создаём платёж `/payments/create`,
 * получаем confirmation_url и открываем форму оплаты; активацию boost делает вебхук ЮKassa.
 * Pro-подписка отложена — пока чистый мок (бэкенда подписок нет).
 */
interface PaymentGateway {
    /**
     * Запуск оплаты boost. Возвращает confirmation_url для редиректа на форму оплаты,
     * либо null, если оплата сымитирована (демо-режим, [SimulatedPaymentGateway.MOCK_BOOST]).
     */
    suspend fun startBoostPayment(adId: String, type: String): ApiResult<String?>

    /** Оформление Pro (отложено — всегда имитация). */
    suspend fun purchaseSubscription(plan: String, months: Int): ApiResult<SubscriptionResponse>
}

@Singleton
class SimulatedPaymentGateway @Inject constructor(
    private val repository: AgroRepository
) : PaymentGateway {

    override suspend fun startBoostPayment(adId: String, type: String): ApiResult<String?> {
        delay(FAKE_PAYMENT_DELAY_MS)
        if (MOCK_BOOST) {
            // Демо без реальной оплаты: успех без редиректа (boost активирует вебхук — здесь его нет).
            return ApiResult.Success(null)
        }
        return when (val r = repository.createPayment(adId, type)) {
            is ApiResult.Success -> ApiResult.Success(r.data.confirmationUrl)
            is ApiResult.Error -> r
        }
    }

    override suspend fun purchaseSubscription(plan: String, months: Int): ApiResult<SubscriptionResponse> {
        delay(FAKE_PAYMENT_DELAY_MS)
        // Pro отложена: всегда мок, реального эндпоинта подписок на бэке нет.
        return ApiResult.Success(SubscriptionResponse(plan = plan, status = "active"))
    }

    private companion object {
        const val FAKE_PAYMENT_DELAY_MS = 1000L
        /**
         * true — boost имитируется офлайн (демо кликается без ЮKassa-ключей).
         * Выставить false, когда ключи ЮKassa (`YUKASSA_SHOP_ID/SECRET_KEY`) настроены на бэке —
         * тогда boost пойдёт через реальный `/payments/create` + confirmation_url.
         */
        const val MOCK_BOOST = true
    }
}
