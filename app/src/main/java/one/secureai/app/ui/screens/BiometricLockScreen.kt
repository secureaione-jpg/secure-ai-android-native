package one.secureai.app.ui.screens

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import one.secureai.app.R

@Composable
fun BiometricLockScreen(onUnlocked: () -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current

    fun showPrompt() {
        val executor = ContextCompat.getMainExecutor(context)
        val activity = context as? FragmentActivity ?: return
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlocked()
            }
        })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.unlock_secure_ai))
            .setSubtitle(context.getString(R.string.biometric_subtitle))
            .setNegativeButtonText(context.getString(R.string.use_passcode))
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) { showPrompt() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0B0B0C)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_lock),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF2563EB)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_locked),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF5F5F7)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.authenticate_to_continue),
                fontSize = 15.sp,
                color = Color(0xFF8E8E93),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(36.dp))
            IconButton(
                onClick = { showPrompt() },
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_fingerprint),
                    contentDescription = "Unlock",
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF2563EB)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            TextButton(onClick = onSkip) {
                Text("Skip for now", color = Color(0xFF8E8E93))
            }
        }
    }
}
