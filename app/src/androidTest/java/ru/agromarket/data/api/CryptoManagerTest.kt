package ru.agromarket.data.api

import android.security.keystore.KeyProperties
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import javax.crypto.KeyGenerator

/**
 * CryptoManager uses a real AndroidKeyStore-backed AES/GCM key, which Robolectric cannot
 * emulate (see audit notes for app/src/test). These tests need a real device/emulator:
 * ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CryptoManagerTest {

    private val cryptoManager = CryptoManager()

    @Test
    fun encryptThenDecryptReturnsOriginalPlaintext() {
        val plain = "super-secret-token"

        val encrypted = cryptoManager.encrypt(plain)
        val decrypted = cryptoManager.decrypt(encrypted)

        assertNotEquals(plain, encrypted)
        assertEquals(plain, decrypted)
    }

    @Test
    fun decryptReturnsNullForRandomBase64Garbage() {
        val garbage = android.util.Base64.encodeToString(
            ByteArray(32) { it.toByte() },
            android.util.Base64.NO_WRAP,
        )

        assertNull(cryptoManager.decrypt(garbage))
    }

    @Test
    fun decryptReturnsNullForEmptyString() {
        assertNull(cryptoManager.decrypt(""))
    }

    @Test
    fun decryptReturnsNullWhenShorterThanIvLength() {
        val tooShort = android.util.Base64.encodeToString(ByteArray(4), android.util.Base64.NO_WRAP)

        assertNull(cryptoManager.decrypt(tooShort))
    }

    @Test
    fun decryptReturnsNullAfterTheKeyIsRotated() {
        val encrypted = cryptoManager.encrypt("rotated-away")

        // Simulate the keystore key being invalidated/rotated under the same alias.
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry("agromarket_token_key")
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                "agromarket_token_key",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        generator.generateKey()

        assertNull(cryptoManager.decrypt(encrypted))
    }
}
