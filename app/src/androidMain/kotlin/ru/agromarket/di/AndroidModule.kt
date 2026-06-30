package ru.agromarket.di

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import ru.agromarket.BuildConfig
import ru.agromarket.data.ApiJson
import ru.agromarket.data.api.CryptoManager
import ru.agromarket.data.api.FirebasePushTokenProvider
import ru.agromarket.data.api.PushTokenProvider
import ru.agromarket.data.api.TokenManager
import ru.agromarket.data.api.TokenManagerTokenProvider
import ru.agromarket.data.api.TokenProvider
import ru.agromarket.data.api.createHttpClient
import ru.agromarket.data.draft.AdDraftManager
import ru.agromarket.ui.ad.AdDetailViewModel
import ru.agromarket.ui.auth.ForgotPasswordViewModel
import ru.agromarket.ui.auth.LoginViewModel
import ru.agromarket.ui.auth.RegisterViewModel
import ru.agromarket.ui.boost.BoostPurchaseViewModel
import ru.agromarket.ui.create.CreateAdViewModel
import ru.agromarket.ui.favorites.FavoritesViewModel
import ru.agromarket.ui.feed.FeedViewModel
import ru.agromarket.ui.profile.ChangePasswordViewModel
import ru.agromarket.ui.profile.ProfileViewModel
import ru.agromarket.ui.subscription.SubscriptionViewModel
import java.util.concurrent.TimeUnit

/**
 * Koin-модуль платформенного (Android) слоя: сетевой движок, HttpClient (нужен BuildConfig —
 * поэтому НЕ протекает в commonMain), хранилища/крипто и ВСЕ ViewModel.
 *
 * ViewModel физически в androidMain до Фазы 1d, поэтому их регистрация здесь, а не в commonMain
 * (перенос VM-биндингов в commonMain — задача 1d). Data-DI (AgroMarketApi/AgroRepository/
 * PaymentGateway) живёт в [commonModule].
 *
 * `singleOf`/`viewModelOf` (конструкторные ссылки) выбраны намеренно: они дают `verify()`
 * прочитать параметры конструктора и поймать недостающий биндинг.
 */
val androidModule: Module = module {
    // OkHttp-движок Ktor с прежними таймаутами (connect 30 / read 30 / write 60).
    single<HttpClientEngine> {
        OkHttp.create {
            config {
                connectTimeout(30, TimeUnit.SECONDS)
                readTimeout(30, TimeUnit.SECONDS)
                writeTimeout(60, TimeUnit.SECONDS)
            }
        }
    }
    // HttpClient биндится здесь: baseUrl/DEBUG берутся из BuildConfig (платформенный артефакт).
    single<HttpClient> {
        createHttpClient(
            engine = get(),
            baseUrl = BuildConfig.API_BASE_URL,
            tokenProvider = get(),
            json = ApiJson,
            enableLogging = BuildConfig.DEBUG,
        )
    }

    singleOf(::TokenManagerTokenProvider) bind TokenProvider::class
    singleOf(::FirebasePushTokenProvider) bind PushTokenProvider::class

    singleOf(::CryptoManager)
    // Context приходит из androidContext(), зарегистрированного в startKoin (AgroMarketApp).
    singleOf(::TokenManager)
    singleOf(::AdDraftManager)

    // ViewModel (11): экраны ещё в androidMain. koinViewModel() резолвит их на call-site.
    viewModelOf(::FeedViewModel)
    viewModelOf(::AdDetailViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
    viewModelOf(::ForgotPasswordViewModel)
    viewModelOf(::ChangePasswordViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::FavoritesViewModel)
    viewModelOf(::CreateAdViewModel)
    viewModelOf(::BoostPurchaseViewModel)
    viewModelOf(::SubscriptionViewModel)
}
