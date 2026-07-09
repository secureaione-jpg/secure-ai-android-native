package one.secureai.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.secureai.app.ui.screens.ChatWebViewScreen

sealed class Screen(val route: String) {
    object Chat : Screen("chat")
}

@Composable
fun AppNavGraph(deepLinkUrl: String? = null) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Chat.route) {
        composable(Screen.Chat.route) {
            ChatWebViewScreen(deepLinkUrl = deepLinkUrl)
        }
    }
}
