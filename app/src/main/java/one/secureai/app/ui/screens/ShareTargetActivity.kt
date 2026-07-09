package one.secureai.app.ui.screens

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import one.secureai.app.ui.theme.SecureAITheme

// Receives text/images shared from other apps and opens the chat with the content pre-filled.
class ShareTargetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedText = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        }

        val url = if (sharedText != null) {
            "https://secureai.one/chat?share=${android.net.Uri.encode(sharedText)}"
        } else {
            "https://secureai.one/chat"
        }

        setContent {
            SecureAITheme {
                ChatWebViewScreen(deepLinkUrl = url)
            }
        }
    }
}
