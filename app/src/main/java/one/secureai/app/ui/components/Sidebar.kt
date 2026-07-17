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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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

// Matches iOS SideMenu.sideBarWidth (140pt) — a narrow icon rail, not a full-width drawer.
private val DrawerWidth = 140.dp
private val UpgradeGold = Color(0xFFD9A621)
private val AccentBlue = Color(0xFF2563EB)

data class SidebarCallbacks(
    val onChats: () -> Unit = {},
    val onHistory: () -> Unit = {},
    val onLibrary: () -> Unit = {},
    val onPhotos: () -> Unit = {},
    val onNotes: () -> Unit = {},
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

// Matches iOS SideBar.swift: a narrow vertical rail of icon-over-label buttons,
// centered, on the system background — not a full-width list of rows.
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
    val textColor = MaterialTheme.colorScheme.onBackground
    val secondaryTextColor = textColor.copy(alpha = 0.6f)

    Column(
        modifier = Modifier
            .width(DrawerWidth)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(top = 80.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Logo — tapping opens the sidebar customize screen (matches iOS).
        Image(
            painter = painterResource(R.drawable.logo_full),
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { callbacks.onApps(); onCollapse() }
        )

        if (Prefs.showChats(context)) {
            RailIconButton(
                iconRes = R.drawable.ic_chat_bubbles,
                label = stringResource(R.string.sidebar_chats),
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                onClick = { callbacks.onChats(); onCollapse() }
            )
        }

        if (Prefs.showProjects(context)) {
            RailIconButton(
                iconRes = R.drawable.ic_folder,
                label = stringResource(R.string.sidebar_projects),
                locked = isAnonymous,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                onClick = {
                    if (isAnonymous) callbacks.onSignIn("library")
                    else callbacks.onProjects()
                    onCollapse()
                }
            )
        }

        if (Prefs.showPhotos(context)) {
            RailIconButton(
                iconRes = R.drawable.ic_photos,
                label = stringResource(R.string.sidebar_photos),
                locked = isAnonymous,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                onClick = {
                    if (isAnonymous) callbacks.onSignIn("photos")
                    else callbacks.onPhotos()
                    onCollapse()
                }
            )
        }

        if (Prefs.showNotes(context)) {
            RailIconButton(
                iconRes = R.drawable.ic_document,
                label = stringResource(R.string.sidebar_notes),
                locked = isAnonymous,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                onClick = {
                    if (isAnonymous) callbacks.onSignIn("notes")
                    else callbacks.onNotes()
                    onCollapse()
                }
            )
        }

        if (!isSubscribed && !isAnonymous) {
            RailIconButton(
                iconRes = R.drawable.ic_crown,
                label = stringResource(R.string.upgrade),
                iconColor = UpgradeGold,
                textColor = textColor,
                secondaryTextColor = secondaryTextColor,
                onClick = { callbacks.onUpgrade(); onCollapse() }
            )
        }

        RailIconButton(
            iconRes = R.drawable.ic_plus,
            label = stringResource(R.string.sidebar_new_chat),
            textColor = textColor,
            secondaryTextColor = secondaryTextColor,
            onClick = { callbacks.onNewChat(); onCollapse() }
        )

        // Profile avatar — rounded square, matches iOS.
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { callbacks.onProfile(); onCollapse() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile?.userInitials?.ifEmpty { "?" } ?: "?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = secondaryTextColor
            )
        }
    }
}

@Composable
private fun RailIconButton(
    iconRes: Int,
    label: String,
    locked: Boolean = false,
    iconColor: Color? = null,
    textColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 52.dp, height = 44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = iconColor ?: textColor,
                modifier = Modifier.size(28.dp)
            )
            if (locked) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lock),
                        contentDescription = "Locked",
                        tint = secondaryTextColor,
                        modifier = Modifier
                            .size(11.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = secondaryTextColor,
            maxLines = 1
        )
    }
}

