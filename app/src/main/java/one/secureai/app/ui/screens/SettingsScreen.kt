package one.secureai.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.secureai.app.BuildConfig
import one.secureai.app.R
import one.secureai.app.data.Prefs

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val biometricAvailable = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    var biometricEnabled by remember { mutableStateOf(Prefs.isBiometricEnabled(context)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B0C))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBack() }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Settings",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF5F5F7)
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(24.dp))
            }

            Spacer(Modifier.height(8.dp))

            SettingsSection("Privacy & Security") {
                if (biometricAvailable) {
                    SettingsToggle(
                        icon = R.drawable.ic_fingerprint,
                        label = "Biometric lock",
                        sublabel = "Require fingerprint or face to open",
                        checked = biometricEnabled,
                        onChecked = {
                            biometricEnabled = it
                            Prefs.setBiometricEnabled(context, it)
                        }
                    )
                }
                SettingsLink(
                    icon = R.drawable.ic_lock,
                    label = "Privacy policy",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://secureai.one/privacy")))
                    }
                )
            }

            SettingsSection("Support") {
                SettingsLink(
                    icon = R.drawable.ic_offline,
                    label = "Help & support",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://secureai.one/support")))
                    }
                )
                SettingsLink(
                    icon = R.drawable.ic_new_chat,
                    label = "Send feedback",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:support@secureai.one")
                            putExtra(Intent.EXTRA_SUBJECT, "Feedback — Secure AI Android")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            SettingsSection("About") {
                SettingsInfo(label = "Version", value = BuildConfig.VERSION_NAME)
                SettingsInfo(label = "Build", value = BuildConfig.VERSION_CODE.toString())
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF8E8E93),
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A1C))
    ) { content() }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun SettingsToggle(icon: Int, label: String, sublabel: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(label, fontSize = 16.sp, color = Color(0xFFF5F5F7))
            Text(sublabel, fontSize = 13.sp, color = Color(0xFF8E8E93))
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
        )
    }
}

@Composable
private fun SettingsLink(icon: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
        Text(label, fontSize = 16.sp, color = Color(0xFFF5F5F7), modifier = Modifier.weight(1f).padding(start = 14.dp))
        Icon(painterResource(R.drawable.ic_chevron), null, tint = Color(0xFF3A3A3C), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsInfo(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = Color(0xFFF5F5F7), modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = Color(0xFF8E8E93))
    }
}
