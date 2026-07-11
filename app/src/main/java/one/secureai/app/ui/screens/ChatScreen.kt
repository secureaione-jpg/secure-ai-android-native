package one.secureai.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText
import one.secureai.app.R
import one.secureai.app.auth.AuthManager
import one.secureai.app.auth.UserProfileManager
import one.secureai.app.chat.ChatMessage
import one.secureai.app.chat.ChatRole
import one.secureai.app.chat.ChatViewModel
import one.secureai.app.data.Prefs
import one.secureai.app.data.store.ProjectStore
import one.secureai.app.network.TTSPlayer
import one.secureai.app.ui.components.SideMenuLayout
import one.secureai.app.ui.components.SidebarCallbacks
import one.secureai.app.ui.theme.Brand
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

private val BubbleUser = Color(0xFF2563EB)
private val TextSecondary = Color(0xFF8E8E93)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
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
    onOpenProjects: () -> Unit = {},
    onOpenTeam: () -> Unit = {},
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showSignIn by remember { mutableStateOf(false) }
    var signInFeature by remember { mutableStateOf("this") }
    var showSidebar by remember { mutableStateOf(false) }
    var showPlusMenu by remember { mutableStateOf(false) }
    val profile by UserProfileManager.profile.collectAsState()
    val isIncognito = Prefs.isIncognito(context)
    val activeProject by ProjectStore.activeProject.collectAsState()

    // Voice input state
    var isRecording by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    // Photo picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                if (bytes != null) {
                    viewModel.sendWithAttachment(inputText.ifBlank { "What's in this image?" }, bytes, "image/jpeg")
                    inputText = ""
                }
            } catch (_: Exception) {}
        }
    }

    // File picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                if (bytes != null) {
                    viewModel.sendWithAttachment(inputText.ifBlank { "Analyze this file" }, bytes, mime)
                    inputText = ""
                }
            } catch (_: Exception) {}
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
            viewModel.sendWithAttachment(inputText.ifBlank { "What's in this image?" }, stream.toByteArray(), "image/jpeg")
            inputText = ""
        }
    }

    // Camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(Unit) {
        AuthManager.signInAnonymouslyIfNeeded()
    }

    fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        speechRecognizer?.destroy()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isRecording = false }
            override fun onError(error: Int) { isRecording = false }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) inputText = partial
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                isRecording = false
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) inputText = text
            }
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        sr.startListening(intent)
        isRecording = true
    }

    val sidebarCallbacks = SidebarCallbacks(
        onApps = onOpenApps,
        onChats = onOpenSavedChats,
        onHistory = onOpenSavedChats,
        onLibrary = onOpenLibrary,
        onPhotos = onOpenPhotos,
        onDocuments = onOpenDocuments,
        onMemories = onOpenMemory,
        onProjects = onOpenProjects,
        onTeam = onOpenTeam,
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
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showSidebar = true }) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                        )
                    }

                    // Title: shows active project or "Secure AI"
                    if (activeProject != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(activeProject!!.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                activeProject!!.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1
                            )
                        }
                    } else {
                        Text(
                            "Secure AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

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

                // Incognito banner
                if (isIncognito) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lock),
                                contentDescription = null,
                                tint = Brand,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Incognito Mode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
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
                            contentPadding = PaddingValues(top = 12.dp, bottom = 200.dp),
                            verticalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            items(messages, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    isLast = message.id == messages.lastOrNull { it.role == ChatRole.ASSISTANT }?.id,
                                    onCopy = {
                                        val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clip.setPrimaryClip(ClipData.newPlainText("message", message.content))
                                    },
                                    onSpeak = {
                                        kotlinx.coroutines.MainScope().launch {
                                            TTSPlayer.togglePlay(message.id, message.content, context.cacheDir)
                                        }
                                    },
                                    onThumbsUp = { viewModel.setFeedback(message.id, true) },
                                    onThumbsDown = { viewModel.setFeedback(message.id, false) },
                                    onRegenerate = { viewModel.regenerate() },
                                    onSave = { viewModel.toggleSaved(message.id) }
                                )
                            }
                        }
                    }

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

                // Input bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // + menu button
                    Box {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { showPlusMenu = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_plus),
                                    contentDescription = "Attachments",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { showPlusMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Camera") },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_camera), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Photos") },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_photos), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Files") },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_document), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    filePickerLauncher.launch("*/*")
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("New chat") },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_new_chat), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    viewModel.clearHistory()
                                }
                            )
                        }
                    }

                    Spacer(Modifier.width(8.dp))

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
                        isRecording -> {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = {
                                    speechRecognizer?.stopListening()
                                    isRecording = false
                                }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_mic),
                                        contentDescription = "Stop recording",
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
                                IconButton(onClick = { startVoiceInput() }) {
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

// Empty State

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
                    color = MaterialTheme.colorScheme.surfaceVariant,
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

// Message Bubble with long-press actions and markdown

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isLast: Boolean = false,
    onCopy: () -> Unit = {},
    onSpeak: () -> Unit = {},
    onThumbsUp: () -> Unit = {},
    onThumbsDown: () -> Unit = {},
    onRegenerate: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    val isUser = message.role == ChatRole.USER
    val haptic = LocalHapticFeedback.current
    var showActions by remember { mutableStateOf(false) }
    val playingId by TTSPlayer.playingMessageId.collectAsState()

    if (message.imageBytes != null) {
        val bitmap = remember(message.id) {
            BitmapFactory.decodeByteArray(message.imageBytes, 0, message.imageBytes.size)?.asImageBitmap()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (isUser) Spacer(Modifier.weight(1f, fill = false).widthIn(min = 48.dp))

            Box(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .then(
                        if (isUser) {
                            Modifier.background(BubbleUser, RoundedCornerShape(18.dp))
                        } else Modifier
                    )
                    .combinedClickable(
                        onClick = {},
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showActions = true
                        }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (isUser || message.isStreaming) {
                    Text(
                        text = message.content.ifEmpty { "…" },
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    )
                } else {
                    MarkdownText(
                        markdown = message.content.ifEmpty { "…" },
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp,
                    )
                }
            }

            if (!isUser) Spacer(Modifier.weight(1f, fill = false).widthIn(min = 24.dp))
        }

        // Action menu
        if (showActions && !isUser) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 4.dp,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ActionButton("Copy", R.drawable.ic_document) { onCopy(); showActions = false }
                    ActionButton(
                        if (playingId == message.id) "Stop" else "Speak",
                        R.drawable.ic_notification
                    ) { onSpeak(); showActions = false }
                    ActionButton("👍", tint = if (message.thumbsUp == true) Brand else null) { onThumbsUp(); showActions = false }
                    ActionButton("👎", tint = if (message.thumbsUp == false) Brand else null) { onThumbsDown(); showActions = false }
                    if (isLast) {
                        ActionButton("Redo", R.drawable.ic_history) { onRegenerate(); showActions = false }
                    }
                    ActionButton(if (message.saved) "★" else "☆") { onSave(); showActions = false }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    iconRes: Int? = null,
    tint: Color? = null,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = tint ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Text(label, fontSize = 16.sp, color = tint ?: MaterialTheme.colorScheme.onSurface)
        }
    }
}
