package ru.agromarket.data.api

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * CryptoManager relies on a real AndroidKeyStore key, which Robolectric cannot back with
 * working AES/GCM crypto (see audit notes), so it is mocked here. TokenManager itself only
 * needs a Context-backed DataStore, which Robolectric provides.
 */
@RunWith(RobolectricTestRunner::class)
class TokenManagerTest {

    private val crypto: CryptoManager = mockk()
    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        // identity-ish round trip by default: ciphertext is just the plaintext prefixed
        every { crypto.encrypt(any()) } answers { "enc:${firstArg<String>()}" }
        every { crypto.decrypt(any()) } answers {
            firstArg<String>().removePrefix("enc:").takeIf { it != firstArg<String>() }
        }
        tokenManager = TokenManager(context, crypto)
        // The DataStore file backing "auth_prefs" persists on disk across Robolectric tests
        // within the same run, so start each test from a clean slate.
        runBlocking { tokenManager.clear() }
    }

    @Test
    fun `accessToken and refreshToken flows emit null when nothing is stored`() = runTest {
        assertNull(tokenManager.accessToken.first())
        assertNull(tokenManager.refreshToken.first())
        assertFalse(tokenManager.isLoggedIn.first())
        assertNull(tokenManager.getAccessToken())
        assertNull(tokenManager.getRefreshToken())
    }

    @Test
    fun `saveTokens round trips through getAccessToken and getRefreshToken`() = runTest {
        tokenManager.saveTokens("access-1", "refresh-1")

        assertEquals("access-1", tokenManager.getAccessToken())
        assertEquals("refresh-1", tokenManager.getRefreshToken())
        assertEquals("access-1", tokenManager.accessToken.first())
        assertEquals("refresh-1", tokenManager.refreshToken.first())
    }

    @Test
    fun `isLoggedIn is true once an access token is stored`() = runTest {
        assertFalse(tokenManager.isLoggedIn.first())

        tokenManager.saveTokens("access-1", "refresh-1")

        assertTrue(tokenManager.isLoggedIn.first())
    }

    @Test
    fun `undecryptable stored tokens surface as null without throwing`() = runTest {
        // Simulate legacy/corrupt ciphertext: stored, but decrypt() returns null.
        every { crypto.decrypt(any()) } returns null

        tokenManager.saveTokens("access-1", "refresh-1")

        assertNull(tokenManager.getAccessToken())
        assertNull(tokenManager.getRefreshToken())
        assertNull(tokenManager.accessToken.first())
        assertNull(tokenManager.refreshToken.first())
        // The (encrypted, undecryptable) access token is still present in DataStore.
        assertTrue(tokenManager.isLoggedIn.first())
    }

    @Test
    fun `clear wipes both tokens and flips isLoggedIn back to false`() = runTest {
        tokenManager.saveTokens("access-1", "refresh-1")

        tokenManager.clear()

        assertNull(tokenManager.getAccessToken())
        assertNull(tokenManager.getRefreshToken())
        assertNull(tokenManager.accessToken.first())
        assertNull(tokenManager.refreshToken.first())
        assertFalse(tokenManager.isLoggedIn.first())
    }
}
