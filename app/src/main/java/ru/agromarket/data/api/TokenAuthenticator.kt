package ru.agromarket.data.api

import dagger.Lazy
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.agromarket.data.model.RefreshTokenRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Refreshes the access token automatically when the server replies 401.
 *
 * OkHttp calls [authenticate] on every 401 and retries the original request with
 * whatever we return here. If the refresh fails, tokens are wiped so that
 * [TokenManager.isLoggedIn] flips to false and the app routes back to login.
 *
 * Note on the dependency cycle: AgroMarketApi -> Retrofit -> OkHttpClient -> this
 * authenticator. We inject [api] as [Lazy] so Hilt can break the cycle.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val api: Lazy<AgroMarketApi>,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // The /auth/ endpoints (including refresh itself) must never trigger a refresh.
        if (response.request.url.encodedPath.contains("/auth/")) return null

        // Stop after a couple of attempts to avoid an infinite 401 loop.
        if (responseCount(response) >= 2) return null

        // Serialize concurrent refreshes so parallel 401s don't each burn the refresh token.
        return synchronized(this) {
            val currentToken = runBlocking { tokenManager.getAccessToken() }
            val failedToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            // Another thread already refreshed while we waited on the lock — just reuse it.
            if (currentToken != null && currentToken != failedToken) {
                return@synchronized response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refresh = runBlocking { tokenManager.getRefreshToken() }
            if (refresh == null) {
                // No usable refresh token (missing, or legacy/undecryptable after the encryption
                // migration) → the session is dead. Wipe tokens so the app routes back to login.
                runBlocking { tokenManager.clear() }
                return@synchronized null
            }

            val newTokens = runBlocking {
                try {
                    val resp = api.get().refreshToken(RefreshTokenRequest(refresh))
                    if (resp.isSuccessful) resp.body() else null
                } catch (e: Exception) {
                    null
                }
            }

            if (newTokens == null) {
                // Refresh rejected/failed → session is dead. Wipe tokens to force re-login.
                runBlocking { tokenManager.clear() }
                return@synchronized null
            }

            runBlocking { tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken) }
            response.request.newBuilder()
                .header("Authorization", "Bearer ${newTokens.accessToken}")
                .build()
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
