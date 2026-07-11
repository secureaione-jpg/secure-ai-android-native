package one.secureai.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.secureai.app.R
import one.secureai.app.data.Prefs

private val AccentBlue = Color(0xFF2563EB)

private data class SidebarItem(
    val iconRes: Int,
    val label: String,
    val getVisible: (android.content.Context) -> Boolean,
    val setVisible: (android.content.Context, Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SidebarCustomizeScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    val items = remember {
        listOf(
            SidebarItem(R.drawable.ic_chat_bubbles, "Chats", Prefs::showChats, Prefs::setShowChats),
            SidebarItem(R.drawable.ic_history, "History", Prefs::showHistory, Prefs::setShowHistory),
            SidebarItem(R.drawable.ic_folder, "Library", Prefs::showProjects, Prefs::setShowProjects),
            SidebarItem(R.drawable.ic_photos, "Photos", Prefs::showPhotos, Prefs::setShowPhotos),
            SidebarItem(R.drawable.ic_document, "Documents", Prefs::showDocuments, Prefs::setShowDocuments),
            SidebarItem(R.drawable.ic_memories, "Memories", Prefs::showMemories, Prefs::setShowMemories),
            SidebarItem(R.drawable.ic_briefcase, "Projects", Prefs::showProjectsNav, Prefs::setShowProjectsNav),
            SidebarItem(R.drawable.ic_team, "Team", Prefs::showTeam, Prefs::setShowTeam),
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Apps", fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            items.forEach { item ->
                var isVisible by remember { mutableStateOf(item.getVisible(context)) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = item.label,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = isVisible,
                        onCheckedChange = { checked ->
                            isVisible = checked
                            item.setVisible(context, checked)
                        },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = AccentBlue
                        )
                    )
                }
            }
        }
    }
}
