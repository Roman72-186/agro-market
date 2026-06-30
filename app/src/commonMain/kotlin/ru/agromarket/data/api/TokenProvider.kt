package ru.agromarket.data.api

/**
 * commonMain-абстракция доступа к токенам сессии для Ktor `Auth(bearer)`-плагина и репозитория.
 *
 * Реальное хранилище (DataStore + AndroidKeyStore через [TokenManager]) живёт в androidMain и
 * переедет в SecureStorage в Фазах 1e/2 — поэтому commonMain не ссылается на `TokenManager` напрямую.
 */
interface TokenProvider {
    suspend fun accessToken(): String?
    suspend fun refreshToken(): String?
    suspend fun saveTokens(access: String, refresh: String)
    suspend fun clear()
}
