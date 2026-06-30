package ru.agromarket.data.api

/**
 * commonMain-абстракция текущего push-токена устройства.
 *
 * Android `actual` оборачивает `FirebaseMessaging.getInstance().token`; iOS обернёт APNs (Фаза 4).
 * Возвращает null, если push недоступен (например, нет google-services.json) — тогда регистрация
 * токена молча no-op, как и раньше.
 */
interface PushTokenProvider {
    suspend fun currentToken(): String?
}
