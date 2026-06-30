package ru.agromarket.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import ru.agromarket.BuildConfig
import ru.agromarket.data.ApiJson
import ru.agromarket.data.api.AgroMarketApi
import ru.agromarket.data.api.FirebasePushTokenProvider
import ru.agromarket.data.api.PushTokenProvider
import ru.agromarket.data.api.TokenManager
import ru.agromarket.data.api.TokenManagerTokenProvider
import ru.agromarket.data.api.TokenProvider
import ru.agromarket.data.repository.AgroRepository
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTokenProvider(tokenManager: TokenManager): TokenProvider =
        TokenManagerTokenProvider(tokenManager)

    @Provides
    @Singleton
    fun providePushTokenProvider(): PushTokenProvider = FirebasePushTokenProvider()

    /** OkHttp-движок Ktor с теми же таймаутами, что и раньше (connect 30 / read 30 / write 60). */
    @Provides
    @Singleton
    fun provideHttpClientEngine(): HttpClientEngine = OkHttp.create {
        config {
            connectTimeout(30, TimeUnit.SECONDS)
            readTimeout(30, TimeUnit.SECONDS)
            writeTimeout(60, TimeUnit.SECONDS)
        }
    }

    @Provides
    @Singleton
    fun provideHttpClient(
        engine: HttpClientEngine,
        tokenProvider: TokenProvider,
    ): HttpClient = ru.agromarket.data.api.createHttpClient(
        engine = engine,
        baseUrl = BuildConfig.API_BASE_URL,
        tokenProvider = tokenProvider,
        json = ApiJson,
        enableLogging = BuildConfig.DEBUG,
    )

    @Provides
    @Singleton
    fun provideApi(client: HttpClient): AgroMarketApi = AgroMarketApi(client)

    @Provides
    @Singleton
    fun provideRepository(
        api: AgroMarketApi,
        tokenProvider: TokenProvider,
        pushTokenProvider: PushTokenProvider,
    ): AgroRepository = AgroRepository(api, tokenProvider, pushTokenProvider)
}
