package ru.agromarket.data.api

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val crypto: CryptoManager,
) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { prefs -> prefs[ACCESS_TOKEN]?.let { crypto.decrypt(it) } }
    val refreshToken: Flow<String?> = context.dataStore.data.map { prefs -> prefs[REFRESH_TOKEN]?.let { crypto.decrypt(it) } }

    // Presence of the (encrypted) access token is enough to know we're logged in — no decrypt needed.
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { it[ACCESS_TOKEN] != null }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = crypto.encrypt(accessToken)
            prefs[REFRESH_TOKEN] = crypto.encrypt(refreshToken)
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN]?.let { crypto.decrypt(it) }
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[REFRESH_TOKEN]?.let { crypto.decrypt(it) }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
