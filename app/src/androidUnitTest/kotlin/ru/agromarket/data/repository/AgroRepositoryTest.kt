package ru.agromarket.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.agromarket.data.api.AgroMarketApi
import ru.agromarket.data.api.TokenManager
import ru.agromarket.data.model.LoginResponse

/**
 * Покрывает контракт AgroRepository.safeCall и побочный эффект сохранения токенов:
 *  - успешный ответ -> ApiResult.Success;
 *  - HTTP-ошибка -> ApiResult.Error с распарсенным полем "detail" и кодом;
 *  - брошенное исключение -> ApiResult.Error;
 *  - login сохраняет токены только при успехе.
 */
class AgroRepositoryTest {

    private val api: AgroMarketApi = mockk()
    private val tokenManager: TokenManager = mockk(relaxed = true)
    private lateinit var repository: AgroRepository

    @Before
    fun setUp() {
        repository = AgroRepository(api, tokenManager)
    }

    @Test
    fun `login success returns data and saves tokens`() = runTest {
        coEvery { api.login(any()) } returns
            Response.success(LoginResponse("acc", "ref", "bearer"))

        val result = repository.login("user@mail.ru", "secret")

        assertTrue(result is ApiResult.Success)
        assertEquals("acc", (result as ApiResult.Success).data.accessToken)
        coVerify(exactly = 1) { tokenManager.saveTokens("acc", "ref") }
    }

    @Test
    fun `login http error parses detail and does not save tokens`() = runTest {
        val errorBody = """{"detail":"Неверный пароль"}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { api.login(any()) } returns Response.error(401, errorBody)

        val result = repository.login("user@mail.ru", "wrong")

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("Неверный пароль", result.message)
        assertEquals(401, result.code)
        coVerify(exactly = 0) { tokenManager.saveTokens(any(), any()) }
    }

    @Test
    fun `thrown exception is mapped to ApiResult Error`() = runTest {
        coEvery { api.getProfile() } throws RuntimeException("boom")

        val result = repository.getProfile()

        assertTrue(result is ApiResult.Error)
        assertEquals("boom", (result as ApiResult.Error).message)
    }

    @Test
    fun `validation error with detail list is parsed into a readable message`() = runTest {
        val errorBody = """{"detail":[{"type":"enum","loc":["body","type"],"msg":"Input should be 'sell', 'service' or 'land'","input":"SELL"}]}"""
            .toResponseBody("application/json".toMediaTypeOrNull())
        coEvery { api.login(any()) } returns Response.error(422, errorBody)

        val result = repository.login("user@mail.ru", "secret")

        assertTrue(result is ApiResult.Error)
        result as ApiResult.Error
        assertEquals("type: Input should be 'sell', 'service' or 'land'", result.message)
        assertEquals(422, result.code)
    }
}
