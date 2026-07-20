package one.secureai.app.navigation

import androidx.biometric.BiometricManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import one.secureai.app.data.Prefs
import one.secureai.app.ui.screens.BiometricLockScreen
import one.secureai.app.ui.screens.ChatScreen
import one.secureai.app.ui.screens.NotificationPermissionScreen
import one.secureai.app.ui.screens.SavedChatsScreen
import one.secureai.app.ui.screens.SettingsScreen
import one.secureai.app.ui.screens.LibraryScreen
import one.secureai.app.ui.screens.AIPhotosScreen
import one.secureai.app.ui.screens.NotesScreen
import one.secureai.app.ui.screens.PaywallScreen
import one.secureai.app.ui.screens.ProjectsScreen
import one.secureai.app.ui.screens.ProjectDetailScreen
import one.secureai.app.data.store.ProjectStore
import one.secureai.app.chat.ChatViewModel
import one.secureai.app.ui.screens.onboarding.OnboardingScreen
import androidx.lifecycle.viewmodel.compose.viewModel

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object NotificationPermission : Screen("notification_permission")
    object Lock : Screen("lock")
    object Chat : Screen("chat")
    object Settings : Screen("settings")
    object SavedChats : Screen("saved_chats")
    object Library : Screen("library")
    object Photos : Screen("photos")
    object Notes : Screen("notes")
    object Paywall : Screen("paywall")
    object Profile : Screen("profile")
    object Projects : Screen("projects")
    object ProjectDetail : Screen("project_detail")
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

    val chatViewModel: ChatViewModel = viewModel()

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
            ChatScreen(
                viewModel = chatViewModel,
                onOpenSettings = { navController.navigate(Screen.Settings.route) },
                onOpenSavedChats = { navController.navigate(Screen.SavedChats.route) },
                onOpenLibrary = { navController.navigate(Screen.Library.route) },
                onOpenPhotos = { navController.navigate(Screen.Photos.route) },
                onOpenPaywall = { navController.navigate(Screen.Paywall.route) },
                onOpenProfile = { navController.navigate(Screen.Profile.route) },
                onOpenProjects = { navController.navigate(Screen.Projects.route) },
                onOpenNotes = { navController.navigate(Screen.Notes.route) },
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onBack = { navController.popBackStack() },
                onOpenProjects = { navController.navigate(Screen.Projects.route) }
            )
        }

        composable(Screen.Photos.route) {
            AIPhotosScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Notes.route) {
            NotesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Paywall.route) {
            PaywallScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPaywall = { navController.navigate(Screen.Paywall.route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenPaywall = { navController.navigate(Screen.Paywall.route) }
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                onBack = { navController.popBackStack() },
                onSelectProject = { project ->
                    ProjectStore.setActive(project)
                    navController.navigate(Screen.ProjectDetail.route)
                }
            )
        }

        composable(Screen.ProjectDetail.route) {
            val project = ProjectStore.activeProject.value
            if (project != null) {
                ProjectDetailScreen(
                    project = project,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        navController.popBackStack()
                    },
                    onStartChat = {
                        navController.popBackStack(Screen.Chat.route, inclusive = false)
                    }
                )
            }
        }

        composable(Screen.SavedChats.route) {
            SavedChatsScreen(
                onBack = { navController.popBackStack() },
                onSelectConversation = { id ->
                    chatViewModel.loadConversation(id)
                },
                onNewChat = {
                    chatViewModel.clearHistory()
                    navController.popBackStack()
                }
            )
        }
    }
}
