package one.secureai.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.secureai.app.R
import one.secureai.app.auth.AuthManager
import one.secureai.app.auth.UserProfileManager
import one.secureai.app.data.Prefs
import one.secureai.app.data.store.StoreManager
import one.secureai.app.data.model.SubscriptionTier
import kotlin.math.roundToInt

private val DrawerWidth = 280.dp
private val UpgradeGold = Color(0xFFD9A621)
private val AccentBlue = Color(0xFF2563EB)
private val DrawerBg = Color(0xFF1C1C1E)
private val DrawerText = Color.White
private val DrawerTextSecondary = Color(0xFF8E8E93)

data class SidebarCallbacks(
    val onChats: () -> Unit = {},
    val onHistory: () -> Unit = {},
    val onLibrary: () -> Unit = {},
    val onPhotos: () -> Unit = {},
    val onNotes: () -> Unit = {},
    val onVoiceMemos: () -> Unit = {},
    val onApps: () -> Unit = {},
    val onProjects: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onNewChat: () -> Unit = {},
    val onUpgrade: () -> Unit = {},
    val onSignIn: (String) -> Unit = {},
)

@Composable
fun SideMenuLayout(
    isExpanded: Boolean,
    onToggle: (Boolean) -> Unit,
    callbacks: SidebarCallbacks,
    content: @Composable () -> Unit
) {
    val drawerWidthPx = with(LocalDensity.current) { DrawerWidth.toPx() }

    val offsetX by animateFloatAsState(
        targetValue = if (isExpanded) drawerWidthPx else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "drawer_offset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Sidebar behind content
        SidebarContent(
            callbacks = callbacks,
            onCollapse = { onToggle(false) }
        )

        // Main content that slides right
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .shadow(if (isExpanded) 16.dp else 0.dp, RoundedCornerShape(if (isExpanded) 24.dp else 0.dp))
                .clip(RoundedCornerShape(if (isExpanded) 24.dp else 0.dp))
                .pointerInput(isExpanded) {
                    detectHorizontalDragGestures(
                        onDragEnd = {},
                        onHorizontalDrag = { _, dragAmount ->
                            if (!isExpanded && dragAmount > 20f) {
                                onToggle(true)
                            } else if (isExpanded && dragAmount < -20f) {
                                onToggle(false)
                            }
                        }
                    )
                }
        ) {
            content()

            // Dim overlay when drawer is open
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable { onToggle(false) }
                )
            }
        }
    }
}

@Composable
private fun SidebarContent(
    callbacks: SidebarCallbacks,
    onCollapse: () -> Unit
) {
    val context = LocalContext.current
    val isAnonymous = AuthManager.isAnonymous
    val tier by StoreManager.currentTier.collectAsState()
    val profile by UserProfileManager.profile.collectAsState()
    val isSubscribed = tier != SubscriptionTier.FREE

    Column(
        modifier = Modifier
            .width(DrawerWidth)
            .fillMaxHeight()
            .background(DrawerBg)
            .statusBarsPadding()
    ) {
        // Header: logo + app name — tapping the logo opens the sidebar customize screen (matches iOS).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { callbacks.onApps(); onCollapse() }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.logo_full),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.width(10.dp))
            Text(
                "Secure AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = DrawerText
            )
        }

        // Nav items (scrollable) — matches iOS: Apps + togglable items
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
        ) {
            if (Prefs.showChats(context)) {
                NavRow(
                    iconRes = R.drawable.ic_chat_bubbles,
                    label = stringResource(R.string.sidebar_chats),
                    onClick = { callbacks.onChats(); onCollapse() }
                )
            }

            if (Prefs.showProjects(context)) {
                NavRow(
                    iconRes = R.drawable.ic_folder,
                    label = stringResource(R.string.sidebar_projects),
                    locked = isAnonymous,
                    onClick = {
                        if (isAnonymous) callbacks.onSignIn("library")
                        else callbacks.onLibrary()
                        onCollapse()
                    }
                )
            }

            if (Prefs.showPhotos(context)) {
                NavRow(
                    iconRes = R.drawable.ic_photos,
                    label = stringResource(R.string.sidebar_photos),
                    locked = isAnonymous,
                    onClick = {
                        if (isAnonymous) callbacks.onSignIn("photos")
                        else callbacks.onPhotos()
                        onCollapse()
                    }
                )
            }

            if (Prefs.showNotes(context)) {
                NavRow(
                    iconRes = R.drawable.ic_document,
                    label = stringResource(R.string.sidebar_notes),
                    locked = isAnonymous,
                    onClick = {
                        if (isAnonymous) callbacks.onSignIn("notes")
                        else callbacks.onNotes()
                        onCollapse()
                    }
                )
            }

            if (Prefs.showVoiceMemos(context)) {
                NavRow(
                    iconRes = R.drawable.ic_mic,
                    label = stringResource(R.string.sidebar_voice),
                    locked = isAnonymous,
                    onClick = {
                        if (isAnonymous) callbacks.onSignIn("voice memos")
                        else callbacks.onVoiceMemos()
                        onCollapse()
                    }
                )
            }
        }

        // Bottom bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 24.dp, bottom = 32.dp)
        ) {
            if (!isSubscribed && !isAnonymous) {
                Surface(
                    onClick = { callbacks.onUpgrade(); onCollapse() },
                    shape = RoundedCornerShape(14.dp),
                    color = UpgradeGold,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_sparkle),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.upgrade),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Avatar + New Chat row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { callbacks.onProfile(); onCollapse() },
                    shape = CircleShape,
                    color = Color(0xFF3A3A3C),
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (profile?.profileImageURL != null) {
                            // Profile photo loaded via Coil or similar would go here
                            Text(
                                text = profile?.userInitials?.ifEmpty { "?" } ?: "?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DrawerText
                            )
                        } else {
                            Text(
                                text = profile?.userInitials?.ifEmpty { "?" } ?: "?",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DrawerText
                            )
                        }
                    }
                }

                Surface(
                    onClick = { callbacks.onNewChat(); onCollapse() },
                    shape = RoundedCornerShape(25.dp),
                    color = AccentBlue,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_plus),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.sidebar_new_chat),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavRow(
    iconRes: Int,
    label: String,
    locked: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = if (isActive) AccentBlue else DrawerText,
            modifier = Modifier.size(23.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) AccentBlue else DrawerText,
            modifier = Modifier.weight(1f)
        )
        if (locked) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = "Locked",
                tint = DrawerTextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

