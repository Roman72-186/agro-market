package ru.agromarket.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ru.agromarket.MainActivity
import ru.agromarket.R
import ru.agromarket.data.repository.AgroRepository

/**
 * Receives FCM push notifications (e.g. moderation status changes) and shows them in the
 * status bar with a deep link to the relevant ad. Token registration is best-effort and
 * silently no-ops until google-services.json is added (no FirebaseApp = no token).
 */
class AgroFirebaseMessagingService : FirebaseMessagingService() {

    private val repository: AgroRepository by inject()

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch { repository.registerPushToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: return
        val adId = message.data["ad_id"]
        showNotification(title, body, adId)
    }

    private fun showNotification(title: String, body: String, adId: String?) {
        val channelId = getString(R.string.default_notification_channel_id)
        val notificationManager = getSystemService(NotificationManager::class.java)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            adId?.let { putExtra(MainActivity.EXTRA_AD_ID, it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, adId?.hashCode() ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
