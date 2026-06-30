package ru.agromarket

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import ru.agromarket.di.androidModule
import ru.agromarket.di.commonModule

class AgroMarketApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Guard: Robolectric instantiates this Application once per test in a shared JVM, so a
        // second startKoin would throw KoinApplicationAlreadyStartedException. In production
        // onCreate runs once and the guard is a no-op.
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@AgroMarketApp)
                modules(commonModule, androidModule)
            }
        }
        createNotificationChannel()
    }

    // FCM looks up this channel by id (see default_notification_channel_id meta-data in the
    // manifest) when it builds notifications for backgrounded notification-payload messages,
    // so it must exist before any message arrives, not just when onMessageReceived runs.
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
