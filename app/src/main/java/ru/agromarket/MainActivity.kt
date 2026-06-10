package ru.agromarket

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.agromarket.data.api.TokenManager
import ru.agromarket.ui.navigation.MainNavigation
import ru.agromarket.ui.theme.AgroMarketTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

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

            AgroMarketTheme {
                MainNavigation(isLoggedIn = isLoggedIn)
            }
        }
    }
}
