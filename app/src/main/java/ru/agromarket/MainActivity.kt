package ru.agromarket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
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
        super.onCreate(savedInstanceState)

        setContent {
            val isLoggedIn by tokenManager.isLoggedIn.collectAsStateWithLifecycle(initialValue = false)

            AgroMarketTheme {
                MainNavigation(isLoggedIn = isLoggedIn)
            }
        }
    }
}
