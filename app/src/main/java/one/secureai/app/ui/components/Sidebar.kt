package one.secureai.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import one.secureai.app.R
import one.secureai.app.auth.AuthManager
import one.secureai.app.data.store.StoreManager
import one.secureai.app.data.model.SubscriptionTier

private val UpgradeGold = Color(0xFFD9A621)

data class SidebarCallbacks(
    val onChats: () -> Unit = {},
    val onHistory: () -> Unit = {},
    val onLibrary: () -> Unit = {},
    val onPhotos: () -> Unit = {},
    val onNotes: () -> Unit = {},
    val onProjects: () -> Unit = {},
    val onProfile: () -> Unit = {},
    val onNewChat: () -> Unit = {},
    val onUpgrade: () -> Unit = {},
    val onSignIn: (String) -> Unit = {},
)

/// Top-left dropdown menu — replaces the old sliding side drawer. Same
/// options, same gating/customization logic, just presented as a native
/// Material dropdown anchored to the button instead of a full-screen drawer.
@Composable
fun TopLeftDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    callbacks: SidebarCallbacks
) {
    val isAnonymous = AuthManager.isAnonymous
    val tier by StoreManager.currentTier.collectAsState()
    val isSubscribed = tier != SubscriptionTier.FREE

    fun act(action: () -> Unit) {
        onDismiss()
        action()
    }

    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.sidebar_new_chat)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_plus), contentDescription = null) },
            onClick = { act(callbacks.onNewChat) }
        )

        DropdownMenuItem(
            text = { Text(stringResource(R.string.sidebar_chats)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_chat_bubbles), contentDescription = null) },
            onClick = { act(callbacks.onChats) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.sidebar_projects)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_folder), contentDescription = null) },
            onClick = {
                act {
                    if (isAnonymous) callbacks.onSignIn("library") else callbacks.onProjects()
                }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.sidebar_photos)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_photos), contentDescription = null) },
            onClick = {
                act {
                    if (isAnonymous) callbacks.onSignIn("photos") else callbacks.onPhotos()
                }
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.sidebar_notes)) },
            leadingIcon = { Icon(painterResource(R.drawable.ic_document), contentDescription = null) },
            onClick = {
                act {
                    if (isAnonymous) callbacks.onSignIn("notes") else callbacks.onNotes()
                }
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.settings)) },
            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
            onClick = { act(callbacks.onProfile) }
        )

        if (!isSubscribed && !isAnonymous) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.upgrade)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_crown), contentDescription = null, tint = UpgradeGold) },
                onClick = { act(callbacks.onUpgrade) }
            )
        }
    }
}
