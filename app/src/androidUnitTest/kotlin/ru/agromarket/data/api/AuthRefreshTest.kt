package ru.agromarket.data.api

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.agromarket.data.ApiJson
import ru.agromarket.data.repository.AgroRepository
import ru.agromarket.data.repository.ApiResult
import java.util.concurrent.atomic.AtomicInteger

/**
 * Через реальный [createHttpClient] (expectSuccess + Ktor Auth(bearer)) и Ktor MockEngine
 * эмпирически проверяет рефреш-токены — то, что в Retrofit-варианте делал TokenAuthenticator.
 * Конкурентность и «стоп после одной попытки» теперь внутренние для Ktor-плагина, поэтому
 * вместо них — smoke «нет бесконечного цикла рефреша».
 */
class AuthRefreshTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    private val pushTokenProvider: PushTokenProvider = object : PushTokenProvider {
        override suspend fun currentToken(): String? = null
    }

    /** In-memory TokenProvider — позволяет проверить, какие токены реально сохранились/сброшены. */
    private class FakeTokenProvider(
        var access: String? = null,
        var refresh: String? = null,
    ) : TokenProvider {
        var cleared = false
        override suspend fun accessToken(): String? = access
        override suspend fun refreshToken(): String? = refresh
        override suspend fun saveTokens(access: String, refresh: String) {
            this.access = access; this.refresh = refresh
        }
        override suspend fun clear() {
            access = null; refresh = null; cleared = true
        }
    }

    private fun repository(
        tokenProvider: TokenProvider,
        handler: suspend io.ktor.client.engine.mock.MockRequestHandleScope.(io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData,
    ): AgroRepository {
        val client = createHttpClient(
            engine = MockEngine(handler),
            baseUrl = "https://test.local/api/v1/",
            tokenProvider = tokenProvider,
            json = ApiJson,
            enableLogging = false,
        )
        return AgroRepository(AgroMarketApi(client), tokenProvider, pushTokenProvider)
    }

    @Test
    fun `expired access token is refreshed and the original request retried with the new token`() = runTest {
        val tp = FakeTokenProvider(access = "old-access", refresh = "old-refresh")
        val refreshCalls = AtomicInteger(0)

        val repo = repository(tp) { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("auth/refresh") -> {
                    refreshCalls.incrementAndGet()
                    respond(
                        """{"access_token":"new-access","refresh_token":"new-refresh","token_type":"bearer"}""",
                        HttpStatusCode.OK, jsonHeaders,
                    )
                }
                request.headers[HttpHeaders.Authorization] == "Bearer new-access" ->
                    respond("""{"message":"ok"}""", HttpStatusCode.OK, jsonHeaders)
                else -> respond("""{"detail":"expired"}""", HttpStatusCode.Unauthorized, jsonHeaders)
            }
        }

        val result = repo.addFavorite("ad-1")

        assertTrue(result is ApiResult.Success)
        assertEquals(1, refreshCalls.get())
        assertEquals("new-access", tp.access)
        assertEquals("new-refresh", tp.refresh)
    }

    @Test
    fun `failed refresh clears the session and surfaces an error`() = runTest {
        val tp = FakeTokenProvider(access = "old-access", refresh = "old-refresh")

        val repo = repository(tp) { request ->
            // Любой запрос (включая рефреш) отвечает 401 — рефреш не удаётся, сессия мертва.
            respond("""{"detail":"expired"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }

        val result = repo.addFavorite("ad-1")

        assertTrue(result is ApiResult.Error)
        assertEquals(401, (result as ApiResult.Error).code)
        assertTrue(tp.cleared)
        assertNull(tp.access)
        assertNull(tp.refresh)
    }

    @Test
    fun `auth endpoints never trigger a token refresh`() = runTest {
        val tp = FakeTokenProvider(access = "old-access", refresh = "old-refresh")
        val refreshCalls = AtomicInteger(0)

        val repo = repository(tp) { request ->
            val path = request.url.encodedPath
            if (path.endsWith("auth/refresh")) refreshCalls.incrementAndGet()
            respond("""{"detail":"bad credentials"}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }

        val result = repo.login("user@mail.ru", "wrong")

        assertTrue(result is ApiResult.Error)
        assertEquals(0, refreshCalls.get())
        // Провал логина не должен трогать сессию.
        assertTrue(!tp.cleared)
    }

    @Test
    fun `refresh does not loop infinitely when the retry still fails`() = runTest {
        val tp = FakeTokenProvider(access = "old-access", refresh = "old-refresh")
        val refreshCalls = AtomicInteger(0)

        val repo = repository(tp) { request ->
            val path = request.url.encodedPath
            if (path.endsWith("auth/refresh")) {
                refreshCalls.incrementAndGet()
                respond(
                    """{"access_token":"new-access","refresh_token":"new-refresh","token_type":"bearer"}""",
                    HttpStatusCode.OK, jsonHeaders,
                )
            } else {
                // Защищённый эндпоинт всё равно 401 даже с новым токеном — рефреш не должен зациклиться.
                respond("""{"detail":"still expired"}""", HttpStatusCode.Unauthorized, jsonHeaders)
            }
        }

        val result = repo.addFavorite("ad-1")

        assertTrue(result is ApiResult.Error)
        // Ktor Auth делает ровно одну попытку рефреша на запрос — не бесконечный цикл.
        assertEquals(1, refreshCalls.get())
    }

    @Test
    fun `token cache is cleared after login so the first authed request carries the token`() = runTest {
        val tp = FakeTokenProvider()
        val protected401 = AtomicInteger(0)

        val repo = repository(tp) { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("auth/login") ->
                    respond(
                        """{"access_token":"acc","refresh_token":"ref","token_type":"bearer"}""",
                        HttpStatusCode.OK, jsonHeaders,
                    )
                request.headers[HttpHeaders.Authorization] == "Bearer acc" ->
                    respond("""{"message":"ok"}""", HttpStatusCode.OK, jsonHeaders)
                else -> {
                    protected401.incrementAndGet()
                    respond("""{"detail":"no token"}""", HttpStatusCode.Unauthorized, jsonHeaders)
                }
            }
        }

        // 1) Запрос без токена кэширует null в Auth-плагине.
        assertTrue(repo.addFavorite("ad-0") is ApiResult.Error)
        // 2) Логин сохраняет токены и сбрасывает кэш Auth-плагина.
        assertTrue(repo.login("user@mail.ru", "secret") is ApiResult.Success)
        protected401.set(0)
        // 3) Теперь первый же защищённый запрос уходит уже с токеном — без предварительного 401.
        assertTrue(repo.addFavorite("ad-1") is ApiResult.Success)
        assertEquals(0, protected401.get())
    }
}
