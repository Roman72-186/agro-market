package ru.agromarket.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.agromarket.data.model.LoginResponse
import ru.agromarket.data.model.RefreshTokenRequest

/**
 * Собирает общий Ktor [HttpClient] для AgroMarket: ContentNegotiation(JSON) + Logging + Auth(bearer).
 *
 * Платформенное (движок и таймауты) приходит снаружи: на Android — OkHttp с connect/read/write
 * таймаутами, на iOS — Darwin (Фаза 3). Поэтому здесь таймауты НЕ настраиваются.
 *
 * @param engine        платформенный движок (OkHttp / Darwin).
 * @param baseUrl       базовый URL API (с завершающим `/`), напр. `https://agro.assaru.space/api/v1/`.
 * @param tokenProvider доступ к access/refresh-токенам сессии.
 * @param json          общий JSON-конфиг (ApiJson), переиспользуется и для ошибок FastAPI.
 * @param enableLogging логировать тела (LogLevel.BODY) только в debug-сборке.
 */
fun createHttpClient(
    engine: HttpClientEngine,
    baseUrl: String,
    tokenProvider: TokenProvider,
    json: Json,
    enableLogging: Boolean,
): HttpClient = HttpClient(engine) {
    // expectSuccess=true: не-2xx бросает ResponseException, которую ловит AgroRepository.safeCall.
    // Ретрай Auth-плагина на 401 отрабатывает в Send-фазе ДО валидации, поэтому успешный рефреш
    // не доходит до этого исключения.
    expectSuccess = true

    install(ContentNegotiation) {
        json(json)
    }

    if (enableLogging) {
        install(Logging) {
            level = LogLevel.BODY
        }
    }

    install(Auth) {
        bearer {
            // Начальные токены из локального хранилища.
            loadTokens {
                val access = tokenProvider.accessToken() ?: return@loadTokens null
                BearerTokens(access, tokenProvider.refreshToken() ?: "")
            }

            // Рефреш на 401. Установлен ровно один auth-провайдер, поэтому Ktor вызывает рефреш
            // даже без заголовка WWW-Authenticate. markAsRefreshTokenRequest() исключает сам
            // запрос рефреша из ретрая (без рекурсии). При успехе сохраняем токены и возвращаем
            // новые; при отсутствии refresh / ошибке — чистим сессию и возвращаем null (уход на логин).
            refreshTokens {
                // Эндпоинты /auth/ (включая сам refresh) никогда не триггерят рефреш — как старый
                // TokenAuthenticator. Ktor при единственном auth-провайдере зовёт рефреш на любой 401,
                // поэтому отсекаем /auth/-пути здесь по исходному запросу (Url.encodedPath доступен).
                if (response.call.request.url.encodedPath.contains("/auth/")) {
                    return@refreshTokens null
                }
                val refresh = tokenProvider.refreshToken()
                if (refresh.isNullOrEmpty()) {
                    tokenProvider.clear()
                    return@refreshTokens null
                }
                try {
                    val tokens = client.post("auth/refresh") {
                        markAsRefreshTokenRequest()
                        contentType(ContentType.Application.Json)
                        setBody(RefreshTokenRequest(refresh))
                    }.body<LoginResponse>()
                    tokenProvider.saveTokens(tokens.accessToken, tokens.refreshToken)
                    BearerTokens(tokens.accessToken, tokens.refreshToken)
                } catch (_: Exception) {
                    tokenProvider.clear()
                    null
                }
            }

            // Слать Bearer превентивно всем запросам, кроме /auth/ (как старый AuthInterceptor).
            // URLBuilder.encodedPath в Ktor 3.x недоступен — проверяем сегменты пути.
            sendWithoutRequest { request ->
                request.url.encodedPathSegments.none { it == "auth" }
            }
        }
    }

    // Базовый URL: методы AgroMarketApi и запрос рефреша используют относительные пути.
    defaultRequest {
        url(baseUrl)
    }
}
