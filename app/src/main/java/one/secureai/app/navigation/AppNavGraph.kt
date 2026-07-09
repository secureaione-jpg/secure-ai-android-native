package one.secureai.app.navigation

import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.secureai.app.data.Prefs
import one.secureai.app.ui.screens.BiometricLockScreen
import one.secureai.app.ui.screens.ChatWebViewScreen
import one.secureai.app.ui.screens.NotificationPermissionScreen
import one.secureai.app.ui.screens.SettingsScreen
import one.secureai.app.ui.screens.onboarding.OnboardingScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object NotificationPermission : Screen("notification_permission")
    object Lock : Screen("lock")
    object Chat : Screen("chat")
    object Settings : Screen("settings")
}

@Composable
fun AppNavGraph(deepLinkUrl: String? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val isOnboarded = Prefs.isOnboarded(context)
    val biometricEnabled = Prefs.isBiometricEnabled(context)
    val biometricAvailable = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    val notificationsPrompted = Prefs.isNotificationsPrompted(context)

    val start = when {
        !isOnboarded -> Screen.Onboarding.route
        biometricEnabled && biometricAvailable -> Screen.Lock.route
        else -> Screen.Chat.route
    }

    NavHost(navController = navController, startDestination = start) {

        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinish = {
                Prefs.setOnboarded(context)
                val next = if (!notificationsPrompted) Screen.NotificationPermission.route
                           else if (biometricEnabled && biometricAvailable) Screen.Lock.route
                           else Screen.Chat.route
                navController.navigate(next) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
            })
        }

        composable(Screen.NotificationPermission.route) {
            NotificationPermissionScreen(onResult = {
                Prefs.setNotificationsPrompted(context)
                val next = if (biometricEnabled && biometricAvailable) Screen.Lock.route else Screen.Chat.route
                navController.navigate(next) { popUpTo(Screen.NotificationPermission.route) { inclusive = true } }
            })
        }

        composable(Screen.Lock.route) {
            BiometricLockScreen(
                onUnlocked = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Lock.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Screen.Chat.route) {
                        popUpTo(Screen.Lock.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Chat.route) {
            ChatWebViewScreen(
                deepLinkUrl = deepLinkUrl,
                onOpenSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
