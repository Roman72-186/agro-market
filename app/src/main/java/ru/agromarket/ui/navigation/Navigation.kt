package ru.agromarket.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.agromarket.ui.auth.ForgotPasswordScreen
import ru.agromarket.ui.auth.LoginScreen
import ru.agromarket.ui.auth.RegisterScreen
import ru.agromarket.ui.feed.FeedScreen
import ru.agromarket.ui.ad.AdDetailScreen
import ru.agromarket.ui.create.CreateAdScreen
import ru.agromarket.ui.favorites.FavoritesScreen
import ru.agromarket.ui.lands.LandsScreen
import ru.agromarket.ui.profile.ChangePasswordScreen
import ru.agromarket.ui.profile.ProfileScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Feed : Screen("feed")
    object AdDetail : Screen("ad/{adId}") { fun createRoute(adId: String) = "ad/$adId" }
    object CreateAd : Screen("create_ad")
    object Favorites : Screen("favorites")
    object Profile : Screen("profile")
    object ChangePassword : Screen("change_password")
    object Lands : Screen("lands")
}

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Feed : BottomNavItem("feed", Icons.Default.Home, "Каталог")
    object Favorites : BottomNavItem("favorites", Icons.Default.Favorite, "Избранное")
    object CreateAd : BottomNavItem("create_ad", Icons.Default.AddCircle, "Разместить")
}

@Composable
fun MainNavigation(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomNavItems = listOf(BottomNavItem.Feed, BottomNavItem.Favorites, BottomNavItem.CreateAd)
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    // React to session loss at runtime (e.g. TokenAuthenticator wiped tokens after a failed
    // refresh): kick the user back to login instead of leaving them on a dead screen.
    // startDestination only applies on first composition, so this must be an explicit navigation.
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn && currentRoute != null &&
            currentRoute != Screen.Login.route && currentRoute != Screen.Register.route
        ) {
            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = {
                                if (item is BottomNavItem.CreateAd) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(item.icon, contentDescription = item.label, tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                } else {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) { popUpTo(Screen.Feed.route) { saveState = true }; launchSingleTop = true; restoreState = true }
                                }
                            },
                            colors = if (item is BottomNavItem.CreateAd) {
                                NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
                            } else {
                                NavigationBarItemDefaults.colors()
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn) Screen.Feed.route else Screen.Login.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 5 } },
            exitTransition = { fadeOut(tween(220)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(220)) + slideOutHorizontally(tween(220)) { it / 5 } },
        ) {
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screen.Feed.route) { popUpTo(0) { inclusive = true } } },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                    onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                )
            }
            composable(Screen.Register.route) {
                RegisterScreen(onRegisterSuccess = { navController.navigate(Screen.Feed.route) { popUpTo(0) { inclusive = true } } }, onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.ForgotPassword.route) {
                ForgotPasswordScreen(onResetSuccess = { navController.popBackStack() }, onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Feed.route) {
                FeedScreen(
                    onAdClick = { adId -> navController.navigate(Screen.AdDetail.createRoute(adId)) },
                    onProfileClick = { navController.navigate(Screen.Profile.route) },
                    onLandsClick = { navController.navigate(Screen.Lands.route) },
                )
            }
            composable(Screen.Lands.route) {
                LandsScreen(
                    onAdClick = { adId -> navController.navigate(Screen.AdDetail.createRoute(adId)) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Screen.AdDetail.route, arguments = listOf(navArgument("adId") { type = NavType.StringType })) { backStackEntry ->
                val adId = backStackEntry.arguments?.getString("adId") ?: return@composable
                AdDetailScreen(adId = adId, onBack = { navController.popBackStack() })
            }
            composable(Screen.CreateAd.route) {
                CreateAdScreen(onSuccess = { navController.navigate(Screen.Feed.route) { popUpTo(Screen.Feed.route) { inclusive = true } } }, onBack = { navController.popBackStack() })
            }
            composable(Screen.Favorites.route) {
                FavoritesScreen(onAdClick = { adId -> navController.navigate(Screen.AdDetail.createRoute(adId)) })
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = { navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } } },
                    onMyAds = { adId -> navController.navigate(Screen.AdDetail.createRoute(adId)) },
                    onBack = { navController.popBackStack() },
                    onChangePassword = { navController.navigate(Screen.ChangePassword.route) },
                )
            }
            composable(Screen.ChangePassword.route) {
                ChangePasswordScreen(onSuccess = { navController.popBackStack() }, onBack = { navController.popBackStack() })
            }
        }
    }
}
