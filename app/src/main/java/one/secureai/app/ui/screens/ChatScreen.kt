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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import one.secureai.app.R
import one.secureai.app.auth.AuthManager
import one.secureai.app.auth.UserProfileManager
import one.secureai.app.chat.ChatMessage
import one.secureai.app.chat.ChatRole
import one.secureai.app.chat.ChatViewModel
import one.secureai.app.ui.components.SideMenuLayout
import one.secureai.app.ui.components.SidebarCallbacks
import java.util.Calendar

private val BubbleUser = Color(0xFF2563EB)
private val BubbleAssistant = Color(0xFF1C1C1E)
private val TextSecondary = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    onOpenTasks: () -> Unit = {},
    onOpenMemory: () -> Unit = {},
    onOpenSavedChats: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenPhotos: () -> Unit = {},
    onOpenDocuments: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenApps: () -> Unit = {},
) {
    val viewModel: ChatViewModel = viewModel()
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showSignIn by remember { mutableStateOf(false) }
    var signInFeature by remember { mutableStateOf("this") }
    var showSidebar by remember { mutableStateOf(false) }
    val profile by UserProfileManager.profile.collectAsState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(Unit) {
        AuthManager.signInAnonymouslyIfNeeded()
    }

    val sidebarCallbacks = SidebarCallbacks(
        onApps = onOpenApps,
        onChats = onOpenSavedChats,
        onHistory = onOpenSavedChats,
        onLibrary = onOpenLibrary,
        onPhotos = onOpenPhotos,
        onDocuments = onOpenDocuments,
        onMemories = onOpenMemory,
        onProfile = onOpenProfile,
        onNewChat = { viewModel.clearHistory() },
        onUpgrade = onOpenPaywall,
        onSignIn = { feature ->
            signInFeature = feature
            showSignIn = true
        },
    )

    SideMenuLayout(
        isExpanded = showSidebar,
        onToggle = { showSidebar = it },
        callbacks = sidebarCallbacks
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // Header — matches iOS: [sidebar toggle] [title] [avatar]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sidebar toggle (iOS: circle.fill icon)
                    IconButton(onClick = { showSidebar = true }) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground),
                        )
                    }

                    // Title
                    Text(
                        "Secure AI",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Avatar button (iOS: 38x38 circle)
                    IconButton(onClick = onOpenProfile) {
                        if (AuthManager.isAnonymous) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    profile?.userInitials?.ifEmpty { "?" } ?: "?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Error banner
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

                // Messages or empty state
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (messages.isEmpty()) {
                        EmptyState(
                            modifier = Modifier.fillMaxSize(),
                            onSuggestion = { prefill -> inputText = prefill }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = 12.dp,
                                bottom = 200.dp
                            )
                        ) {
                            items(messages, key = { it.id }) { message ->
                                MessageBubble(message)
                            }
                        }
                    }

                    // Bottom scrim gradient (iOS: 140pt fade)
                    if (messages.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.background
                                        )
                                    )
                                )
                        )
                    }
                }

                // Input bar — matches iOS: [+ menu] [text field] [mic/send/stop]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // + menu button (iOS: 50x50 circle with ultraThinMaterial)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { /* TODO: attachment menu */ }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_plus),
                                contentDescription = "Attachments",
                                tint = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Text field (iOS: 25dp corner radius, min 50dp height)
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Chat with Secure AI", color = TextSecondary, fontSize = 17.sp) },
                        shape = RoundedCornerShape(25.dp),
                        maxLines = 6,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            focusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                            cursorColor = BubbleUser
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    // Right button: stop (streaming) / send (has text) / mic (default)
                    when {
                        isStreaming -> {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(BubbleUser),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = { viewModel.stop() }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_stop),
                                        contentDescription = "Stop",
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                        inputText.isNotBlank() -> {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(BubbleUser),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = {
                                    viewModel.send(inputText)
                                    inputText = ""
                                }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = { /* TODO: start voice input */ }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_mic),
                                        contentDescription = "Voice input",
                                        tint = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
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

// Empty State with greeting + suggestion chips

private data class Suggestion(val icon: Int, val label: String, val prefill: String)

@Composable
private fun EmptyState(modifier: Modifier = Modifier, onSuggestion: (String) -> Unit) {
    val profile by UserProfileManager.profile.collectAsState()
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val firstName = profile?.userName?.split(" ")?.firstOrNull()?.ifEmpty { null }

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
            text = if (firstName != null) "$greeting, $firstName" else greeting,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
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
                            tint = BubbleUser,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            s.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(2f))
    }
}

// Message Bubble

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
                        .clip(RoundedCornerShape(18.dp))
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
        if (isUser) Spacer(Modifier.weight(1f, fill = false).widthIn(min = 48.dp))

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (isUser) {
                        Modifier.background(
                            color = BubbleUser,
                            shape = RoundedCornerShape(18.dp)
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content.ifEmpty { "…" },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp
            )
        }

        if (!isUser) Spacer(Modifier.weight(1f, fill = false).widthIn(min = 24.dp))
    }
}
