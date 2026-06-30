package ru.agromarket.data.repository

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.agromarket.data.ApiJson
import ru.agromarket.data.api.AgroMarketApi
import ru.agromarket.data.api.PushTokenProvider
import ru.agromarket.data.api.TokenProvider
import ru.agromarket.data.api.createHttpClient
import java.io.IOException

/**
 * Покрывает контракт AgroRepository.safeCall и побочный эффект сохранения токенов поверх Ktor
 * MockEngine (вместо Retrofit-моков):
 *  - успешный ответ -> ApiResult.Success + сохранение токенов;
 *  - HTTP-ошибка -> ApiResult.Error с распарсенным полем "detail" и кодом, токены НЕ сохранены;
 *  - брошенное исключение -> ApiResult.Error;
 *  - 422-список detail -> читаемое «field: msg».
 */
class AgroRepositoryTest {

    private val tokenProvider: TokenProvider = mockk(relaxed = true)
    private val pushTokenProvider: PushTokenProvider = mockk(relaxed = true)

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
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
    fun `login success returns data, saves tokens and resolves the full api base path`() = runTest {
        var firstPath: String? = null
        val repo = repository { request ->
            // MockEngine видит уже разрешённый URL (defaultRequest применился до движка) — проверяем,
            // что относительные пути склеились с baseUrl и префикс /api/v1/ не потерян (гейт это не ловит).
            // Берём именно первый запрос (логин), т.к. успешный логин затем дёргает регистрацию push-токена.
            if (firstPath == null) firstPath = request.url.encodedPath
            respond(
                content = """{"access_token":"acc","refresh_token":"ref","token_type":"bearer"}""",
                status = HttpStatusCode.OK,
                headers = jsonHeaders,
            )
        }

        val result = repo.login("user@mail.ru", "secret")

        assertTrue(result is ApiResult.Success)
        assertEquals("acc", (result as ApiResult.Success).data.accessToken)
        assertEquals("/api/v1/auth/login", firstPath)
        coVerify(exactly = 1) { tokenProvider.saveTokens("acc", "ref") }
    }

    @Test
    fun `login http error parses detail and does not save tokens`() = runTest {
        val repo = repository {
            respond(
                content = """{"detail":"Неверный пароль"}""",
                status = HttpStatusCode.Unauthorized,
                headers = jsonHeaders,
            )
        }

        val result = repo.login("user@mail.ru", "wrong")

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("Неверный пароль", result.message)
        assertEquals(401, result.code)
        coVerify(exactly = 0) { tokenProvider.saveTokens(any(), any()) }
    }

    @Test
    fun `thrown exception is mapped to ApiResult Error`() = runTest {
        val repo = repository { throw IOException("boom") }

        val result = repo.getProfile()

        assertTrue(result is ApiResult.Error)
        assertTrue((result as ApiResult.Error).message.contains("boom"))
    }

    @Test
    fun `validation error with detail list is parsed into a readable message`() = runTest {
        val repo = repository {
            respond(
                content = """{"detail":[{"type":"enum","loc":["body","type"],"msg":"Input should be 'sell', 'service' or 'land'","input":"SELL"}]}""",
                status = HttpStatusCode.UnprocessableEntity,
                headers = jsonHeaders,
            )
        }

        val result = repo.login("user@mail.ru", "secret")

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("type: Input should be 'sell', 'service' or 'land'", result.message)
        assertEquals(422, result.code)
    }
}
