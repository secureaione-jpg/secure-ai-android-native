package one.secureai.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import one.secureai.app.BuildConfig
import one.secureai.app.R
import one.secureai.app.auth.AuthManager
import one.secureai.app.auth.UserProfileManager
import one.secureai.app.data.ChatBackground
import one.secureai.app.data.Prefs
import one.secureai.app.ui.theme.Brand

private val LabelSecondary = Color(0xFF8E8E93)

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by UserProfileManager.profile.collectAsState()
    val biometricAvailable = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    var biometricEnabled by remember { mutableStateOf(Prefs.isBiometricEnabled(context)) }
    var hapticsEnabled by remember { mutableStateOf(Prefs.isHapticsEnabled(context)) }
    var incognito by remember { mutableStateOf(Prefs.isIncognito(context)) }
    var textSize by remember { mutableIntStateOf(Prefs.getTextSize(context)) }
    var chatBackground by remember { mutableStateOf(ChatBackground.fromKey(Prefs.chatBackground(context))) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_back),
                    contentDescription = "Back",
                    tint = Brand,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBack() }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Settings",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(24.dp))
            }

            // Profile header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile?.userInitials?.ifEmpty { "?" } ?: "?",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = profile?.userName?.ifEmpty { "Guest" } ?: "Guest",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (!AuthManager.isAnonymous) {
                    Text(
                        text = AuthManager.user.value?.email ?: "",
                        fontSize = 14.sp,
                        color = LabelSecondary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Chat section
            SettingsSection("Chat") {
                SettingsToggle(
                    icon = R.drawable.ic_lock,
                    label = "Incognito mode",
                    sublabel = "Messages are not saved",
                    checked = incognito,
                    onChecked = {
                        incognito = it
                        Prefs.setIncognito(context, it)
                    }
                )
                SettingsSegment(
                    icon = R.drawable.ic_new_chat,
                    label = "Text size",
                    options = listOf("Small", "Medium", "Large"),
                    selected = textSize,
                    onSelected = {
                        textSize = it
                        Prefs.setTextSize(context, it)
                    }
                )
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Background",
                        fontSize = 13.sp,
                        color = LabelSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                    ) {
                        ChatBackground.entries.forEach { bg ->
                            val colors = if (bg == ChatBackground.SYSTEM) listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background
                            ) else bg.gradient
                            Box(
                                modifier = Modifier
                                    .size(width = 52.dp, height = 72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.verticalGradient(colors))
                                    .then(
                                        if (bg == chatBackground) Modifier.border(
                                            2.dp, Brand, RoundedCornerShape(10.dp)
                                        ) else Modifier
                                    )
                                    .clickable {
                                        chatBackground = bg
                                        Prefs.setChatBackground(context, bg.key)
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Text(
                                    bg.label,
                                    fontSize = 9.sp,
                                    color = if (bg.usesLightText) Color.White else MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Privacy & Security
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
                SettingsToggle(
                    icon = R.drawable.ic_notification,
                    label = "Haptic feedback",
                    sublabel = "Vibrate on interactions",
                    checked = hapticsEnabled,
                    onChecked = {
                        hapticsEnabled = it
                        Prefs.setHapticsEnabled(context, it)
                    }
                )
                SettingsLink(
                    icon = R.drawable.ic_lock,
                    label = "Privacy policy",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://secureai.one/privacy")))
                    }
                )
                SettingsLink(
                    icon = R.drawable.ic_document,
                    label = "Terms of service",
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://secureai.one/terms")))
                    }
                )
            }

            // Support
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

            // Account
            SettingsSection("Account") {
                if (AuthManager.isAnonymous) {
                    SettingsInfo(label = "Status", value = "Guest")
                } else {
                    SettingsInfo(
                        label = "Email",
                        value = AuthManager.user.value?.email ?: "Signed in"
                    )
                    SettingsLink(
                        icon = R.drawable.ic_back,
                        label = "Sign out",
                        onClick = {
                            AuthManager.signOut()
                            onBack()
                        }
                    )
                    SettingsLink(
                        icon = R.drawable.ic_back,
                        label = "Delete app data",
                        tint = Color(0xFFFF3B30),
                        onClick = {
                            scope.launch {
                                UserProfileManager.deleteAppData()
                            }
                        }
                    )
                    SettingsLink(
                        icon = R.drawable.ic_back,
                        label = "Delete account",
                        tint = Color(0xFFFF3B30),
                        onClick = { showDeleteConfirm = true }
                    )
                }
            }

            // About
            SettingsSection("About") {
                SettingsInfo(label = "Version", value = BuildConfig.VERSION_NAME)
                SettingsInfo(label = "Build", value = BuildConfig.VERSION_CODE.toString())
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Account") },
            text = { Text("This will permanently delete your account and all data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    scope.launch {
                        UserProfileManager.deleteAccount()
                        AuthManager.signOut()
                        onBack()
                    }
                }) {
                    Text("Delete", color = Color(0xFFFF3B30))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = LabelSecondary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
        Icon(painterResource(icon), null, tint = Brand, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(sublabel, fontSize = 13.sp, color = LabelSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Brand)
        )
    }
}

@Composable
private fun SettingsLink(icon: Int, label: String, tint: Color = Brand, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            label,
            fontSize = 16.sp,
            color = if (tint == Brand) MaterialTheme.colorScheme.onBackground else tint,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
        Icon(painterResource(R.drawable.ic_chevron), null, tint = LabelSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
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
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = LabelSecondary)
    }
}

@Composable
private fun SettingsSegment(icon: Int, label: String, options: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, tint = Brand, modifier = Modifier.size(20.dp))
        Text(
            label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 14.dp)
        )
        Spacer(Modifier.weight(1f))
        Row {
            options.forEachIndexed { idx, option ->
                val isSelected = idx == selected
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) Brand else LabelSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Brand.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onSelected(idx) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                )
                if (idx < options.lastIndex) Spacer(Modifier.width(4.dp))
            }
        }
    }
}
