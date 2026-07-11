package one.secureai.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

data class SidebarCallbacks(
    val onApps: () -> Unit = {},
    val onChats: () -> Unit = {},
    val onHistory: () -> Unit = {},
    val onLibrary: () -> Unit = {},
    val onPhotos: () -> Unit = {},
    val onDocuments: () -> Unit = {},
    val onMemories: () -> Unit = {},
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
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header: logo + app name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_splash),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Secure AI",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Nav items (scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
        ) {
            // 1. Apps (always visible)
            NavRow(
                iconRes = R.drawable.ic_apps_grid,
                label = "Apps",
                onClick = { callbacks.onApps(); onCollapse() }
            )

            // 2. Chats
            if (Prefs.showChats(context)) {
                NavRow(
                    iconRes = R.drawable.ic_chat_bubbles,
                    label = "Chats",
                    onClick = { callbacks.onChats(); onCollapse() }
                )
            }

            // 3. History
            if (Prefs.showHistory(context)) {
                NavRow(
                    iconRes = R.drawable.ic_history,
                    label = "History",
                    onClick = { callbacks.onHistory(); onCollapse() }
                )
            }

            // 4. Library
            if (Prefs.showProjects(context)) {
                NavRow(
                    iconRes = R.drawable.ic_folder,
                    label = "Library",
                    locked = isAnonymous,
                    onClick = {
                        if (isAnonymous) callbacks.onSignIn("library")
                        else callbacks.onLibrary()
                        onCollapse()
                    }
                )
            }

            // 5. Photos
            if (Prefs.showPhotos(context)) {
                NavRow(
                    iconRes = R.drawable.ic_photos,
                    label = "Photos",
                    locked = isAnonymous,
                    onClick = {
                        if (isAnonymous) callbacks.onSignIn("photos")
                        else callbacks.onPhotos()
                        onCollapse()
                    }
                )
            }

            // 6. Documents
            if (Prefs.showDocuments(context)) {
                NavRow(
                    iconRes = R.drawable.ic_document,
                    label = "Documents",
                    locked = isAnonymous,
                    onClick = {
                        if (isAnonymous) callbacks.onSignIn("documents")
                        else callbacks.onDocuments()
                        onCollapse()
                    }
                )
            }

            // 7. Memories
            if (Prefs.showMemories(context)) {
                NavRow(
                    iconRes = R.drawable.ic_memories,
                    label = "Memories",
                    locked = isAnonymous,
                    onClick = {
                        if (isAnonymous) callbacks.onSignIn("memories")
                        else callbacks.onMemories()
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
            // Upgrade button (only for non-anonymous, non-subscribed)
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
                            "Upgrade",
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
                // Avatar
                Surface(
                    onClick = { callbacks.onProfile(); onCollapse() },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = profile?.userInitials?.ifEmpty { "?" } ?: "?",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // New Chat button
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
                            "New Chat",
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
            tint = if (isActive) AccentBlue else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(23.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) AccentBlue else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (locked) {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

