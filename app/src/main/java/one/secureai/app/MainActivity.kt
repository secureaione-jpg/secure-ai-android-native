package one.secureai.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import one.secureai.app.data.Prefs
import one.secureai.app.navigation.AppNavGraph
import one.secureai.app.review.ReviewManager
import one.secureai.app.ui.theme.SecureAITheme
import one.secureai.app.update.InAppUpdateManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        Prefs.incrementSession(this)
        InAppUpdateManager.checkAndUpdate(this)
        ReviewManager.maybeRequestReview(this, this)

        val deepLinkUrl = intent?.data?.toString()
        setContent {
            SecureAITheme {
                AppNavGraph(deepLinkUrl = deepLinkUrl)
            }
        }
    }
}
