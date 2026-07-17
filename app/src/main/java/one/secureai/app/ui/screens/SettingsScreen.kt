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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.res.stringResource
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
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
    var showRedeemDialog by remember { mutableStateOf(false) }

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
                    stringResource(R.string.settings),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SettingsText
                )
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(R.string.done),
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

            // Invite Friends — matches iOS
            DarkCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out Secure AI: https://play.google.com/store/apps/details?id=${context.packageName}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painterResource(R.drawable.logo_full),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.invite_friends),
                            fontSize = 16.sp,
                            color = SettingsText
                        )
                        Text(
                            stringResource(R.string.invite_friends_sub),
                            fontSize = 13.sp,
                            color = SettingsSecondary
                        )
                    }
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = SettingsSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Preferences section — matches iOS
            SettingsLabel(stringResource(R.string.preferences))
            DarkCard {
                SettingsToggleRow(
                    icon = R.drawable.ic_notification,
                    label = stringResource(R.string.haptic_feedback),
                    checked = hapticsEnabled,
                    onChecked = {
                        hapticsEnabled = it
                        Prefs.setHapticsEnabled(context, it)
                    }
                )
                Divider()
                SettingsSegmentRow(
                    icon = R.drawable.ic_new_chat,
                    label = stringResource(R.string.text_size),
                    options = listOf(stringResource(R.string.size_small), stringResource(R.string.size_medium), stringResource(R.string.size_large)),
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
                        stringResource(R.string.background),
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
                    label = stringResource(R.string.notifications),
                    onClick = onOpenNotifications
                )
            }

            Spacer(Modifier.height(24.dp))

            // Subscription — matches iOS
            SettingsLabel(stringResource(R.string.subscription))
            DarkCard {
                SettingsLinkRow(
                    label = stringResource(R.string.upgrade),
                    sublabel = stringResource(R.string.upgrade_sublabel),
                    onClick = onOpenPaywall
                )
                Divider()
                SettingsLinkRow(
                    label = stringResource(R.string.restore_purchases),
                    onClick = { one.secureai.app.data.store.StoreManager.restorePurchases() }
                )
                Divider()
                SettingsLinkRow(
                    label = stringResource(R.string.redeem_code),
                    sublabel = stringResource(R.string.redeem_code_sub),
                    onClick = { showRedeemDialog = true }
                )
            }

            Spacer(Modifier.height(24.dp))

            // Account
            if (!AuthManager.isAnonymous) {
                SettingsLabel(stringResource(R.string.account))
                DarkCard {
                    SettingsLinkRow(
                        label = stringResource(R.string.sign_out),
                        onClick = {
                            AuthManager.signOut()
                            onBack()
                        }
                    )
                    Divider()
                    SettingsLinkRow(
                        label = stringResource(R.string.delete_app_data),
                        tint = Color(0xFFFF3B30),
                        onClick = {
                            scope.launch { UserProfileManager.deleteAppData() }
                        }
                    )
                    Divider()
                    SettingsLinkRow(
                        label = stringResource(R.string.delete_account),
                        tint = Color(0xFFFF3B30),
                        onClick = { showDeleteConfirm = true }
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // About
            SettingsLabel(stringResource(R.string.about))
            DarkCard {
                SettingsInfoRow(label = stringResource(R.string.version), value = BuildConfig.VERSION_NAME)
                Divider()
                SettingsInfoRow(label = stringResource(R.string.build), value = BuildConfig.VERSION_CODE.toString())
            }

            Spacer(Modifier.height(24.dp))

            // Our Apps — cross-promote the family, matches iOS companySection.
            SettingsLabel(stringResource(R.string.company_apps))
            DarkCard {
                CompanyAppRow("Sanna Health", "https://play.google.com/store/apps/details?id=one.sanna.health")
                Divider()
                CompanyAppRow("Secure AI", "https://play.google.com/store/apps/details?id=one.secureai.app")
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showRedeemDialog) {
        RedeemCodeDialog(
            onDismiss = { showRedeemDialog = false },
            onRedeemed = { showRedeemDialog = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.delete_account_title)) },
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
                    Text(stringResource(R.string.cancel))
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
private fun CompanyAppRow(name: String, storeUrl: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(storeUrl)))
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 16.sp, color = SettingsText, modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.Share,
            contentDescription = stringResource(R.string.share_app, name),
            tint = SettingsSecondary,
            modifier = Modifier
                .size(20.dp)
                .clickable {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Check out $name: $storeUrl")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, null))
                }
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

@Composable
private fun RedeemCodeDialog(onDismiss: () -> Unit, onRedeemed: () -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var success by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(stringResource(R.string.redeem_code)) },
        text = {
            Column {
                Text(stringResource(R.string.redeem_code_sub), fontSize = 14.sp, color = SettingsSecondary)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase(); error = null },
                    placeholder = { Text("Enter code") },
                    singleLine = true,
                    enabled = !isLoading && success == null,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = Color(0xFFFF3B30), fontSize = 13.sp)
                }
                if (success != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(success!!, color = Color(0xFF34C759), fontSize = 13.sp)
                }
                if (isLoading) {
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val trimmed = code.trim()
                    if (trimmed.isEmpty()) { error = "Please enter a code"; return@TextButton }
                    isLoading = true
                    error = null
                    scope.launch {
                        try {
                            val result = redeemCode(trimmed)
                            success = "Upgraded to ${result.uppercase()}"
                            kotlinx.coroutines.delay(1500)
                            onRedeemed()
                        } catch (e: Exception) {
                            error = e.message ?: "Failed to redeem code"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading && success == null
            ) { Text("Redeem") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private suspend fun redeemCode(code: String): String = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val token = AuthManager.getIdToken() ?: throw Exception("Please sign in first")
    val url = "https://secureai.one/api/redeem"
    val body = org.json.JSONObject().put("code", code)
    val request = okhttp3.Request.Builder()
        .url(url)
        .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull()))
        .addHeader("Authorization", "Bearer $token")
        .build()
    val response = okhttp3.OkHttpClient().newCall(request).execute()
    val responseBody = response.body?.string() ?: ""
    if (!response.isSuccessful) {
        val msg = try { org.json.JSONObject(responseBody).optString("error", "Unknown error") } catch (_: Exception) { "Unknown error" }
        throw Exception(msg)
    }
    try { org.json.JSONObject(responseBody).optString("tier", "plus") } catch (_: Exception) { "plus" }
}
