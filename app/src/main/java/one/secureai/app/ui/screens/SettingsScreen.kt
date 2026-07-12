package one.secureai.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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

private val SettingsBg = Color(0xFF000000)
private val SettingsSurface = Color(0xFF1C1C1E)
private val SettingsText = Color.White
private val SettingsSecondary = Color(0xFF8E8E93)
private val SettingsSeparator = Color(0xFF38383A)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val profile by UserProfileManager.profile.collectAsState()

    var hapticsEnabled by remember { mutableStateOf(Prefs.isHapticsEnabled(context)) }
    var textSize by remember { mutableIntStateOf(Prefs.getTextSize(context)) }
    var chatBackground by remember { mutableStateOf(ChatBackground.fromKey(Prefs.chatBackground(context))) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SettingsBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header — matches iOS "Ajustes" / "Listo"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    "Settings",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsText
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Done",
                    fontSize = 17.sp,
                    color = Brand,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Profile card — matches iOS: avatar + short name + @username
            DarkCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* open profile editor */ }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A3A3C)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile?.userInitials?.ifEmpty { "?" } ?: "?",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SettingsText
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        val displayName = profile?.userName?.split(" ")?.firstOrNull()?.ifEmpty { "Guest" } ?: "Guest"
                        Text(
                            text = displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SettingsText
                        )
                        val username = profile?.username
                        if (!username.isNullOrEmpty()) {
                            Text(
                                text = "@$username",
                                fontSize = 14.sp,
                                color = SettingsSecondary
                            )
                        }
                    }
                    Icon(
                        painterResource(R.drawable.ic_chevron),
                        null,
                        tint = SettingsSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Preferences section — matches iOS
            SettingsLabel("Preferences")
            DarkCard {
                SettingsToggleRow(
                    icon = R.drawable.ic_notification,
                    label = "Haptic feedback",
                    checked = hapticsEnabled,
                    onChecked = {
                        hapticsEnabled = it
                        Prefs.setHapticsEnabled(context, it)
                    }
                )
                Divider()
                SettingsSegmentRow(
                    icon = R.drawable.ic_new_chat,
                    label = "Text size",
                    options = listOf("Small", "Medium", "Large"),
                    selected = textSize,
                    onSelected = {
                        textSize = it
                        Prefs.setTextSize(context, it)
                    }
                )
                Divider()
                // Background picker
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "Background",
                        fontSize = 13.sp,
                        color = SettingsSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ChatBackground.entries.forEach { bg ->
                            val colors = if (bg == ChatBackground.SYSTEM) listOf(
                                Color(0xFF2C2C2E),
                                Color(0xFF2C2C2E)
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
                                    color = if (bg == ChatBackground.SYSTEM || bg.usesLightText) Color.White
                                           else Color.Black,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Notifications — matches iOS
            DarkCard {
                SettingsLinkRow(
                    label = "Notifications",
                    onClick = onOpenNotifications
                )
            }

            Spacer(Modifier.height(24.dp))

            // Subscription — matches iOS
            SettingsLabel("Subscription")
            DarkCard {
                SettingsLinkRow(
                    label = "Upgrade",
                    sublabel = "Unlock premium models and higher limits",
                    onClick = onOpenPaywall
                )
                Divider()
                SettingsLinkRow(
                    label = "Restore purchases",
                    onClick = {
                        // TODO: restore purchases
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Account
            if (!AuthManager.isAnonymous) {
                SettingsLabel("Account")
                DarkCard {
                    SettingsLinkRow(
                        label = "Sign out",
                        onClick = {
                            AuthManager.signOut()
                            onBack()
                        }
                    )
                    Divider()
                    SettingsLinkRow(
                        label = "Delete app data",
                        tint = Color(0xFFFF3B30),
                        onClick = {
                            scope.launch { UserProfileManager.deleteAppData() }
                        }
                    )
                    Divider()
                    SettingsLinkRow(
                        label = "Delete account",
                        tint = Color(0xFFFF3B30),
                        onClick = { showDeleteConfirm = true }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // About
            SettingsLabel("About")
            DarkCard {
                SettingsInfoRow(label = "Version", value = BuildConfig.VERSION_NAME)
                Divider()
                SettingsInfoRow(label = "Build", value = BuildConfig.VERSION_CODE.toString())
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

// Dark card container matching iOS grouped settings style
@Composable
private fun DarkCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SettingsSurface)
    ) { content() }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .height(0.5.dp)
            .background(SettingsSeparator)
    )
}

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        color = SettingsSecondary,
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsToggleRow(
    icon: Int,
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, tint = Brand, modifier = Modifier.size(20.dp))
        Text(
            label,
            fontSize = 16.sp,
            color = SettingsText,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Brand,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF39393D)
            )
        )
    }
}

@Composable
private fun SettingsLinkRow(
    label: String,
    sublabel: String? = null,
    tint: Color? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 16.sp,
                color = tint ?: SettingsText
            )
            if (sublabel != null) {
                Text(
                    sublabel,
                    fontSize = 13.sp,
                    color = SettingsSecondary
                )
            }
        }
        Icon(
            painterResource(R.drawable.ic_chevron),
            null,
            tint = SettingsSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, color = SettingsText, modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, color = SettingsSecondary)
    }
}

@Composable
private fun SettingsSegmentRow(
    icon: Int,
    label: String,
    options: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(icon), null, tint = Brand, modifier = Modifier.size(20.dp))
        Text(
            label,
            fontSize = 16.sp,
            color = SettingsText,
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
                    color = if (isSelected) Brand else SettingsSecondary,
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
