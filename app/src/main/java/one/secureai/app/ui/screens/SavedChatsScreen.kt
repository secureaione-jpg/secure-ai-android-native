package one.secureai.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import one.secureai.app.R
import one.secureai.app.data.store.ConversationStore
import one.secureai.app.data.store.StoredConversation
import one.secureai.app.ui.theme.Brand
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

private enum class ChatFilter(val label: String) {
    ALL("All"),
    PINNED("Pinned"),
    THIS_WEEK("This Week"),
    OLDER("Older")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedChatsScreen(onBack: () -> Unit, onSelectConversation: (String) -> Unit, onNewChat: () -> Unit = {}) {
    val conversations by ConversationStore.conversations.collectAsState()
    val isLoading by ConversationStore.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    var showMenu by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf(ChatFilter.ALL) }

    LaunchedEffect(Unit) { ConversationStore.load() }

    fun filtered(list: List<StoredConversation>): List<StoredConversation> {
        val weekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
        return when (filterMode) {
            ChatFilter.ALL -> list
            ChatFilter.PINNED -> list.filter { it.favorited }
            ChatFilter.THIS_WEEK -> list.filter { it.updatedAt >= weekAgo }
            ChatFilter.OLDER -> list.filter { it.updatedAt < weekAgo }
        }
    }

    val pinned = filtered(conversations.filter { it.favorited })
    val unpinned = filtered(conversations.filter { !it.favorited })
    val allRows = pinned + unpinned

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Saved chats", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("New Chat") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                onClick = { showMenu = false; onNewChat() }
                            )
                            HorizontalDivider()
                            ChatFilter.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.label, fontWeight = if (filterMode == mode) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = { filterMode = mode; showMenu = false }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (filterMode != ChatFilter.ALL) {
                Surface(
                    onClick = { filterMode = ChatFilter.ALL },
                    shape = RoundedCornerShape(50),
                    color = Brand.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(filterMode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Close, contentDescription = "Clear filter", tint = Brand, modifier = Modifier.size(13.dp))
                    }
                }
            }

            when {
                isLoading && conversations.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                conversations.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No saved chats", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.conversations_empty),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                allRows.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("No results", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Try a different filter",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        items(allRows, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                onClick = {
                                    onSelectConversation(conversation.id)
                                    onBack()
                                },
                                onDelete = { scope.launch { ConversationStore.deleteConversation(conversation.id) } },
                                onToggleFavorite = { scope.launch { ConversationStore.toggleFavorite(conversation.id) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConversationRow(
    conversation: StoredConversation,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (conversation.favorited) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD60A),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(conversation.updatedAt),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
