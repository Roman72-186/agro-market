package ru.agromarket.data.api

import dagger.Lazy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import retrofit2.Response as RetrofitResponse
import ru.agromarket.data.model.LoginResponse
import ru.agromarket.data.model.RefreshTokenRequest

class TokenAuthenticatorTest {

    private val tokenManager: TokenManager = mockk(relaxed = true)
    private val api: AgroMarketApi = mockk()
    private val lazyApi: Lazy<AgroMarketApi> = mockk()
    private lateinit var authenticator: TokenAuthenticator

    @Before
    fun setUp() {
        every { lazyApi.get() } returns api
        authenticator = TokenAuthenticator(tokenManager, lazyApi)
    }

    private fun response(
        url: String = "https://agro.assaru.space/api/v1/feed",
        authHeader: String? = "Bearer old-access",
        priorAttempts: Int = 0,
    ): Response {
        val request = Request.Builder()
            .url(url)
            .apply { authHeader?.let { header("Authorization", it) } }
            .build()

        var prior: Response? = null
        repeat(priorAttempts) {
            prior = Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(401)
                .message("Unauthorized")
                .apply { prior?.let { priorResponse(it) } }
                .build()
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .apply { prior?.let { priorResponse(it) } }
            .build()
    }

    @Test
    fun `successful refresh returns request with new token and saves tokens`() {
        coEvery { tokenManager.getAccessToken() } returns "old-access"
        coEvery { tokenManager.getRefreshToken() } returns "old-refresh"
        coEvery { api.refreshToken(RefreshTokenRequest("old-refresh")) } returns
            RetrofitResponse.success(LoginResponse("new-access", "new-refresh", "bearer"))

        val result = authenticator.authenticate(null, response(authHeader = "Bearer old-access"))

        assertEquals("Bearer new-access", result?.header("Authorization"))
        coVerify(exactly = 1) { tokenManager.saveTokens("new-access", "new-refresh") }
    }

    @Test
    fun `failed refresh response wipes tokens and returns null`() {
        coEvery { tokenManager.getAccessToken() } returns "old-access"
        coEvery { tokenManager.getRefreshToken() } returns "old-refresh"
        val errorBody = "".toResponseBody()
        coEvery { api.refreshToken(RefreshTokenRequest("old-refresh")) } returns
            RetrofitResponse.error(401, errorBody)

        val result = authenticator.authenticate(null, response(authHeader = "Bearer old-access"))

        assertNull(result)
        coVerify(exactly = 1) { tokenManager.clear() }
    }

    @Test
    fun `refresh call throwing exception wipes tokens and returns null`() {
        coEvery { tokenManager.getAccessToken() } returns "old-access"
        coEvery { tokenManager.getRefreshToken() } returns "old-refresh"
        coEvery { api.refreshToken(RefreshTokenRequest("old-refresh")) } throws RuntimeException("boom")

        val result = authenticator.authenticate(null, response(authHeader = "Bearer old-access"))

        assertNull(result)
        coVerify(exactly = 1) { tokenManager.clear() }
    }

    @Test
    fun `missing refresh token wipes tokens without calling refresh endpoint`() {
        coEvery { tokenManager.getAccessToken() } returns "old-access"
        coEvery { tokenManager.getRefreshToken() } returns null

        val result = authenticator.authenticate(null, response(authHeader = "Bearer old-access"))

        assertNull(result)
        coVerify(exactly = 1) { tokenManager.clear() }
        coVerify(exactly = 0) { api.refreshToken(any()) }
    }

    @Test
    fun `concurrent 401 reuses already refreshed token without calling refresh endpoint`() {
        coEvery { tokenManager.getAccessToken() } returns "already-refreshed"

        val result = authenticator.authenticate(null, response(authHeader = "Bearer old-access"))

        assertEquals("Bearer already-refreshed", result?.header("Authorization"))
        coVerify(exactly = 0) { api.refreshToken(any()) }
        coVerify(exactly = 0) { tokenManager.getRefreshToken() }
    }

    @Test
    fun `gives up after the second attempt without touching token manager`() {
        val result = authenticator.authenticate(null, response(priorAttempts = 2))

        assertNull(result)
        coVerify(exactly = 0) { tokenManager.getAccessToken() }
        coVerify(exactly = 0) { api.refreshToken(any()) }
    }

    @Test
    fun `auth endpoints never trigger a refresh`() {
        val result = authenticator.authenticate(
            null,
            response(url = "https://agro.assaru.space/api/v1/auth/refresh"),
        )

        assertNull(result)
        coVerify(exactly = 0) { tokenManager.getAccessToken() }
        coVerify(exactly = 0) { api.refreshToken(any()) }
    }
}
