package ru.agromarket

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.android.ext.android.inject
import ru.agromarket.data.api.TokenManager
import ru.agromarket.ui.navigation.MainNavigation
import ru.agromarket.ui.theme.AgroMarketTheme

class MainActivity : ComponentActivity() {

    private val tokenManager: TokenManager by inject()

    private var pendingAdId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        pendingAdId = intent?.getStringExtra(EXTRA_AD_ID)

        splashScreen.setOnExitAnimationListener { splashScreenView ->
            ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f).apply {
                interpolator = AccelerateInterpolator()
                duration = 250L
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }

        setContent {
            val isLoggedIn by tokenManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

            // Notifications are only useful once we can register a push token for the user.
            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                    if (!granted) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            AgroMarketTheme {
                MainNavigation(
                    isLoggedIn = isLoggedIn,
                    pendingAdId = pendingAdId,
                    onPendingAdConsumed = { pendingAdId = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingAdId = intent.getStringExtra(EXTRA_AD_ID)
    }

    companion object {
        /** Intent extra carrying the ad id to open, set by [ru.agromarket.fcm.AgroFirebaseMessagingService]. */
        const val EXTRA_AD_ID = "ad_id"
    }
}
