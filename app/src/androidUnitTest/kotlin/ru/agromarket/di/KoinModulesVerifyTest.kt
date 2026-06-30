package ru.agromarket.di

import android.content.Context
import io.ktor.client.HttpClientConfig
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

/**
 * Рантайм-проверка графа Koin без устройства. Hilt валидировал DI на компиляции (kapt); Koin
 * резолвит на первом обращении в рантайме — значит сборка/unit-тесты могут быть зелёными, а
 * приложение падать на старте «No definition found for …». verify() рефлексивно проходит граф
 * и ловит это как обычный JVM-тест.
 *
 * Проверяем ОБА модуля вместе: commonMain-биндинги (AgroRepository, PaymentGateway) зависят от
 * androidMain-реализаций (TokenProvider→TokenManagerTokenProvider, PushTokenProvider, HttpClient),
 * поэтому verify по отдельности дал бы ложный фейл.
 *
 * extraTypes:
 * - `Context` — его поставляет androidContext() при startKoin (для TokenManager/AdDraftManager).
 * - `HttpClientConfig` — verify рефлексивно читает конструктор объявленного типа HttpClient
 *   (engine, userConfig: HttpClientConfig); userConfig собирает наша фабрика createHttpClient
 *   внутри лямбды, это НЕ инжектируемая зависимость, поэтому он в whitelist.
 */
class KoinModulesVerifyTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `common and android modules resolve fully`() {
        module { includes(commonModule, androidModule) }
            .verify(extraTypes = listOf(Context::class, HttpClientConfig::class))
    }
}
