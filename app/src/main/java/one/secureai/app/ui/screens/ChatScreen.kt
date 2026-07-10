package one.secureai.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import one.secureai.app.R
import one.secureai.app.auth.AuthManager
import one.secureai.app.chat.ChatMessage
import one.secureai.app.chat.ChatRole
import one.secureai.app.chat.ChatViewModel
import java.util.Calendar

private val BrandBlue = Color(0xFF2563EB)
private val SurfaceDark = Color(0xFF0B0B0C)
private val BubbleUser = Color(0xFF2563EB)
private val BubbleAssistant = Color(0xFF1C1C1E)
private val TextPrimary = Color(0xFFF5F5F7)
private val TextSecondary = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    onOpenTasks: () -> Unit = {},
    onOpenMemory: () -> Unit = {},
    onOpenSavedChats: () -> Unit = {}
) {
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showMenu by remember { mutableStateOf(false) }
    var showSignIn by remember { mutableStateOf(false) }
    var signInFeature by remember { mutableStateOf("this") }
    val isAnonymous by remember { AuthManager.user }.collectAsState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    // Anonymous sign-in on launch
    LaunchedEffect(Unit) {
        AuthManager.signInAnonymouslyIfNeeded()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SurfaceDark) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Secure AI", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // New chat button
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_new_chat),
                            contentDescription = "New chat",
                            tint = BrandBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Avatar / menu
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            // Simple gear icon for guests, initials circle for signed-in
                            if (AuthManager.isAnonymous) {
                                Icon(Icons.Default.Settings, contentDescription = "Menu", tint = TextPrimary)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3A3A3C)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("S", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (AuthManager.isAnonymous) {
                                DropdownMenuItem(
                                    text = { Text("Sign in to unlock") },
                                    onClick = {
                                        showMenu = false
                                        signInFeature = "your account"
                                        showSignIn = true
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Tasks") },
                                onClick = {
                                    showMenu = false
                                    if (AuthManager.isAnonymous) {
                                        signInFeature = "tasks"
                                        showSignIn = true
                                    } else onOpenTasks()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Memories") },
                                onClick = {
                                    showMenu = false
                                    if (AuthManager.isAnonymous) {
                                        signInFeature = "memories"
                                        showSignIn = true
                                    } else onOpenMemory()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Saved chats") },
                                onClick = { showMenu = false; onOpenSavedChats() }
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                onClick = { showMenu = false; onOpenSettings() }
                            )
                        }
                    }
                }
            }

            // ── Error banner ──
            errorMessage?.let { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = Color(0xFF3A1F1F),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFFF8A8A),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // ── Messages or empty state ──
            if (messages.isEmpty()) {
                EmptyState(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    onSuggestion = { prefill -> inputText = prefill }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message)
                    }
                }
            }

            // ── Input bar ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Chat with Secure AI", color = TextSecondary) },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BubbleAssistant,
                        unfocusedContainerColor = BubbleAssistant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = BubbleUser
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (isStreaming) {
                    IconButton(onClick = { viewModel.stop() }) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp), color = BubbleUser, strokeWidth = 2.dp)
                    }
                } else {
                    IconButton(
                        onClick = {
                            viewModel.send(inputText)
                            inputText = ""
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = BubbleUser)
                    }
                }
            }
        }
    }

    // Sign-in bottom sheet
    if (showSignIn) {
        ModalBottomSheet(
            onDismissRequest = { showSignIn = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SignInPromptScreen(
                feature = signInFeature,
                onDismiss = { showSignIn = false }
            )
        }
    }
}

// ── Empty State with greeting + suggestion chips ──

private data class Suggestion(val icon: Int, val label: String, val prefill: String)

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onSuggestion: (String) -> Unit) {
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val suggestions = remember {
        listOf(
            Suggestion(R.drawable.ic_new_chat, "Ask", "Ask: "),
            Suggestion(R.drawable.ic_notification, "Remind", "Remind me to "),
            Suggestion(R.drawable.ic_tasks, "Task", "Create a task: "),
            Suggestion(R.drawable.ic_memories, "Note", "Create a note: "),
        )
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        Text(
            text = greeting,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Your private AI assistant",
            fontSize = 15.sp,
            color = TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            items(suggestions) { s ->
                Surface(
                    onClick = { onSuggestion(s.prefill) },
                    shape = RoundedCornerShape(14.dp),
                    color = BubbleAssistant,
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(s.icon),
                            contentDescription = null,
                            tint = BrandBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(s.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    }
                }
            }
        }

        Spacer(Modifier.weight(2f))
    }
}

// ── Message Bubble ──

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER

    if (message.imageBytes != null) {
        val bitmap = remember(message.id) {
            BitmapFactory.decodeByteArray(message.imageBytes, 0, message.imageBytes.size)?.asImageBitmap()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Generated image",
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (isUser) BubbleUser else BubbleAssistant,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content.ifEmpty { "…" },
                color = TextPrimary,
                fontSize = 15.sp
            )
        }
    }
}
