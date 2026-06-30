package ru.agromarket.data.api

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Android-реализация [PushTokenProvider] поверх FCM. Возвращает null, если Firebase не
 * инициализирован (нет google-services.json) или вызов иначе упал — тогда регистрация токена no-op.
 */
class FirebasePushTokenProvider : PushTokenProvider {
    override suspend fun currentToken(): String? = try {
        FirebaseMessaging.getInstance().token.await()
    } catch (_: Exception) {
        null
    }
}
