package one.secureai.app.navigation

import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.secureai.app.ui.screens.BiometricLockScreen
import one.secureai.app.ui.screens.ChatWebViewScreen

sealed class Screen(val route: String) {
    object Lock : Screen("lock")
    object Chat : Screen("chat")
}

@Composable
fun AppNavGraph(deepLinkUrl: String? = null) {
    val context = LocalContext.current
    val navController = rememberNavController()

    val biometricAvailable = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    val start = if (biometricAvailable) Screen.Lock.route else Screen.Chat.route

    NavHost(navController = navController, startDestination = start) {
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
            ChatWebViewScreen(deepLinkUrl = deepLinkUrl)
        }
    }
}
