package ru.agromarket.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.agromarket.data.payment.PaymentGateway
import ru.agromarket.data.payment.SimulatedPaymentGateway
import javax.inject.Singleton

/**
 * Привязка платёжного шлюза. Сейчас — имитация. Чтобы перейти на реальный Т-Банк,
 * достаточно заменить связывание на TBankPaymentGateway здесь — экраны не трогаются.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PaymentModule {

    @Binds
    @Singleton
    abstract fun bindPaymentGateway(impl: SimulatedPaymentGateway): PaymentGateway
}
