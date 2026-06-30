package ru.agromarket.data.api

/**
 * Android-реализация commonMain-интерфейса [TokenProvider] поверх [TokenManager]
 * (DataStore `auth_prefs` + AES-256/GCM в AndroidKeyStore). commonMain про TokenManager не знает.
 */
class TokenManagerTokenProvider(
    private val tokenManager: TokenManager,
) : TokenProvider {
    override suspend fun accessToken(): String? = tokenManager.getAccessToken()
    override suspend fun refreshToken(): String? = tokenManager.getRefreshToken()
    override suspend fun saveTokens(access: String, refresh: String) = tokenManager.saveTokens(access, refresh)
    override suspend fun clear() = tokenManager.clear()
}
