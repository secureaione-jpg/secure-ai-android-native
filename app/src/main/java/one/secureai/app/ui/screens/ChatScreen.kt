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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.res.stringResource
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
import one.secureai.app.data.ChatBackground
import one.secureai.app.data.Prefs
import one.secureai.app.data.store.ChatContextStore
import one.secureai.app.data.store.Memory
import one.secureai.app.data.store.MemoryStore
import one.secureai.app.data.store.ProjectStore
import one.secureai.app.network.TTSPlayer
import one.secureai.app.ui.components.CameraResultOverlay
import one.secureai.app.ui.components.QuickActionChipsRow
import one.secureai.app.ui.theme.Brand
import one.secureai.app.data.store.StoreManager
import one.secureai.app.data.model.SubscriptionTier
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
    onOpenSavedChats: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenPhotos: () -> Unit = {},
    onOpenPaywall: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenProjects: () -> Unit = {},
    onOpenNotes: () -> Unit = {},
) {
    val context = LocalContext.current
    val messages by viewModel.messages.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showSignIn by remember { mutableStateOf(false) }
    var signInFeature by remember { mutableStateOf("this") }
    var showPlusMenu by remember { mutableStateOf(false) }
    val profile by UserProfileManager.profile.collectAsState()
    val authUser by AuthManager.user.collectAsState()
    val isAnon = authUser?.isAnonymous ?: true
    val isIncognito = Prefs.isIncognito(context)
    val activeProject by ProjectStore.activeProject.collectAsState()
    val tier by StoreManager.currentTier.collectAsState()
    val isSubscribed = tier != SubscriptionTier.FREE
    val activeBg = ChatBackground.fromKey(Prefs.chatBackground(context))
    val contentColor = if (activeBg.usesLightText) Color.White else MaterialTheme.colorScheme.onBackground

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
                    viewModel.sendWithAttachment(inputText.ifBlank { context.getString(R.string.analyze_file) }, bytes, mime)
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

    // Quick-capture camera (top-left button) — separate from the attach-to-chat
    // camera above: this feeds the post-capture quick-action chips + overlay
    // result card instead of the chat thread.
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var quickResultLoading by remember { mutableStateOf(false) }
    var quickResultAnswer by remember { mutableStateOf<String?>(null) }
    var quickResultError by remember { mutableStateOf<String?>(null) }
    var showQuickResult by remember { mutableStateOf(false) }

    fun resetQuickCapture() {
        capturedBitmap = null
        showQuickResult = false
        quickResultLoading = false
        quickResultAnswer = null
        quickResultError = null
    }

    fun runQuickAction(action: one.secureai.app.ui.components.QuickAction) {
        val bmp = capturedBitmap ?: return
        val stream = java.io.ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
        showQuickResult = true
        quickResultLoading = true
        quickResultAnswer = null
        quickResultError = null
        viewModel.sendOneShotWithImage(action.prompt, stream.toByteArray(), "image/jpeg") { result ->
            quickResultLoading = false
            result.onSuccess { quickResultAnswer = it }
            result.onFailure { quickResultError = it.message ?: "Something went wrong. Try again." }
        }
    }

    val quickCameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            showQuickResult = false
            quickResultAnswer = null
            quickResultError = null
        }
    }

    val quickCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) quickCameraLauncher.launch(null)
    }

    // Tap-to-chat-context: a Note/Library item tapped elsewhere lands here as
    // a fresh chat pre-loaded with it as system context (consumed once).
    LaunchedEffect(Unit) {
        ChatContextStore.consume()?.let { viewModel.setPendingContext(it) }
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(Unit) {
        AuthManager.signInAnonymouslyIfNeeded()
        UserProfileManager.load()
        MemoryStore.load()
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

    fun requireSignIn(feature: String, action: () -> Unit) {
        if (AuthManager.isAnonymous) { signInFeature = feature; showSignIn = true } else action()
    }

    Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (activeBg.gradient.isNotEmpty()) Modifier.background(Brush.verticalGradient(activeBg.gradient))
                    else Modifier.background(MaterialTheme.colorScheme.background)
                )
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
                    // Top-left button is camera-first: opens the camera
                    // immediately. Everything the old side-drawer/dropdown
                    // menu had now lives in the "+" attach menu below.
                    IconButton(onClick = {
                        requireSignIn("camera") { quickCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA) }
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = "Camera",
                            tint = contentColor,
                            modifier = Modifier.size(22.dp)
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
                                color = contentColor,
                                maxLines = 1
                            )
                        }
                    } else {
                        Text(
                            "Secure AI",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = contentColor
                        )
                    }

                    if (isAnon) {
                        Surface(
                            onClick = {
                                signInFeature = "your account"
                                showSignIn = true
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                stringResource(R.string.sign_in),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    } else {
                        IconButton(onClick = onOpenProfile) {
                            Text(
                                profile?.userInitials?.ifEmpty { "?" } ?: "?",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor,
                                modifier = Modifier.size(38.dp).wrapContentSize(Alignment.Center)
                            )
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
                                stringResource(R.string.incognito_mode),
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
                            usesLightText = activeBg.usesLightText,
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
                        val scrimColor = if (activeBg.gradient.isNotEmpty()) activeBg.gradient.last() else MaterialTheme.colorScheme.background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            scrimColor
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
                                .background(contentColor.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { showPlusMenu = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_plus),
                                    contentDescription = "Attachments",
                                    tint = contentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showPlusMenu,
                            onDismissRequest = { showPlusMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.incognito)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_lock), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    Prefs.setIncognito(context, !Prefs.isIncognito(context))
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.new_chat)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_new_chat), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    viewModel.clearHistory()
                                }
                            )
                            if (messages.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_conversation)) },
                                    leadingIcon = { Icon(painterResource(R.drawable.ic_document), null, modifier = Modifier.size(20.dp)) },
                                    onClick = {
                                        showPlusMenu = false
                                        val text = messages
                                            .filter { it.content.isNotEmpty() }
                                            .joinToString("\n\n") { msg ->
                                                (if (msg.role == ChatRole.USER) "You: " else "Assistant: ") + msg.content
                                            }
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, text)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, null))
                                    }
                                )
                                HorizontalDivider()
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.files)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_document), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    if (AuthManager.isAnonymous) {
                                        signInFeature = "files"
                                        showSignIn = true
                                    } else {
                                        filePickerLauncher.launch("*/*")
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.photos)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_photos), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    if (AuthManager.isAnonymous) {
                                        signInFeature = "photos"
                                        showSignIn = true
                                    } else {
                                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.camera)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_camera), null, modifier = Modifier.size(20.dp)) },
                                onClick = {
                                    showPlusMenu = false
                                    if (AuthManager.isAnonymous) {
                                        signInFeature = "camera"
                                        showSignIn = true
                                    } else {
                                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                    }
                                }
                            )

                            HorizontalDivider()

                            // Folded in from the old top-left side-drawer/dropdown
                            // menu now that the top-left button is camera-first.
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sidebar_chats)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_chat_bubbles), null, modifier = Modifier.size(20.dp)) },
                                onClick = { showPlusMenu = false; onOpenSavedChats() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sidebar_projects)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_folder), null, modifier = Modifier.size(20.dp)) },
                                onClick = { showPlusMenu = false; requireSignIn("library", onOpenProjects) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sidebar_photos)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_photos), null, modifier = Modifier.size(20.dp)) },
                                onClick = { showPlusMenu = false; requireSignIn("photos", onOpenPhotos) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sidebar_notes)) },
                                leadingIcon = { Icon(painterResource(R.drawable.ic_document), null, modifier = Modifier.size(20.dp)) },
                                onClick = { showPlusMenu = false; requireSignIn("notes", onOpenNotes) }
                            )

                            HorizontalDivider()

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.settings)) },
                                leadingIcon = { Icon(Icons.Filled.Settings, null, modifier = Modifier.size(20.dp)) },
                                onClick = { showPlusMenu = false; onOpenProfile() }
                            )
                            if (!isSubscribed && !isAnon) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.upgrade)) },
                                    leadingIcon = { Icon(painterResource(R.drawable.ic_crown), null, tint = Color(0xFFD9A621)) },
                                    onClick = { showPlusMenu = false; onOpenPaywall() }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(context.getString(R.string.ask_secure_ai), color = if (activeBg.usesLightText) Color.White else Color.Gray, fontSize = 17.sp) },
                        shape = RoundedCornerShape(25.dp),
                        maxLines = 6,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 17.sp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = contentColor,
                            unfocusedTextColor = contentColor,
                            focusedContainerColor = contentColor.copy(alpha = 0.08f),
                            unfocusedContainerColor = contentColor.copy(alpha = 0.08f),
                            focusedBorderColor = contentColor.copy(alpha = 0.08f),
                            unfocusedBorderColor = contentColor.copy(alpha = 0.08f),
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
                                    .background(contentColor.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = { startVoiceInput() }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_mic),
                                        contentDescription = "Voice input",
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

    // Post-capture: photo preview + quick-action chips (Identify, Extract
    // Text, Scan Code, etc.) — shown before a chip is tapped.
    val pendingCapture = capturedBitmap
    if (pendingCapture != null && !showQuickResult) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp), contentAlignment = Alignment.CenterEnd) {
                    IconButton(onClick = { resetQuickCapture() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                Image(
                    bitmap = pendingCapture.asImageBitmap(),
                    contentDescription = "Captured photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp)
                )
                Text(
                    "Point your camera at something to ask",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                )
                QuickActionChipsRow(
                    modifier = Modifier.padding(bottom = 24.dp).navigationBarsPadding(),
                    enabled = true,
                    onSelect = { action -> runQuickAction(action) }
                )
            }
        }
    }

    // Post-answer overlay: photo + compact answer card + Copy/Ask More,
    // instead of opening the chat thread (matches iOS's polished result card).
    if (showQuickResult && pendingCapture != null) {
        CameraResultOverlay(
            imageBitmap = pendingCapture,
            isLoading = quickResultLoading,
            answer = quickResultAnswer,
            error = quickResultError,
            onCopy = {
                quickResultAnswer?.let {
                    val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clip.setPrimaryClip(ClipData.newPlainText("answer", it))
                }
            },
            onAskMore = {
                val stream = java.io.ByteArrayOutputStream()
                pendingCapture.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
                viewModel.sendWithAttachment("Tell me more", stream.toByteArray(), "image/jpeg")
                resetQuickCapture()
            },
            onDismiss = { resetQuickCapture() }
        )
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

@Composable
private fun EmptyState(modifier: Modifier = Modifier, usesLightText: Boolean = false, onSuggestion: (String) -> Unit) {
    val profile by UserProfileManager.profile.collectAsState()
    val context = LocalContext.current
    val memories by MemoryStore.memories.collectAsState()
    var nudgeDismissed by remember { mutableStateOf(false) }
    val dayKey = remember { MemoryStore.dayKey(java.util.Date()) }
    val nudge = remember(memories, nudgeDismissed) {
        if (nudgeDismissed) null
        else MemoryStore.todaysNudge { key -> Prefs.isNudgeDismissed(context, key) }
    }
    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> context.getString(R.string.greeting_morning)
            hour < 17 -> context.getString(R.string.greeting_afternoon)
            else -> context.getString(R.string.greeting_evening)
        }
    }
    val firstName = profile?.userName?.split(" ")?.firstOrNull()?.ifEmpty { null }
    val textColor = if (usesLightText) Color.White else MaterialTheme.colorScheme.onBackground

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(16.dp))
                .background(textColor.copy(alpha = 0.12f))
                .then(
                    Modifier.padding(1.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(R.drawable.logo_full),
                    contentDescription = "Secure AI",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (firstName != null) "$greeting, $firstName" else greeting,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }

        if (nudge != null) {
            Spacer(Modifier.height(12.dp))
            NudgeCard(
                text = nudgePhrasing(nudge, dayKey),
                onTap = { onSuggestion(nudgePhrasing(nudge, dayKey)) },
                onDismiss = {
                    Prefs.dismissNudge(context, dayKey)
                    nudgeDismissed = true
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        Spacer(Modifier.weight(2f))
    }
}

/** Turns a stored memory into something the assistant would say unprompted,
 * rotating phrasing by day so it doesn't feel scripted. Mirrors iOS's
 * `nudgePhrasing(for:)` template set exactly. */
private fun nudgePhrasing(memory: Memory, dayKey: String): String {
    val templates = listOf(
        "You mentioned: “${memory.content}” — how's that going?",
        "Following up on something you told me: ${memory.content}",
        "Just checking in — ${memory.content}",
    )
    val index = kotlin.math.abs(dayKey.hashCode()) % templates.size
    return templates[index]
}

/** A dismissible card surfacing a proactive nudge derived from memory — the
 * assistant bringing something up unprompted, instead of only responding to
 * what's typed. */
@Composable
private fun NudgeCard(text: String, onTap: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onTap,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Brand.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontSize = 14.sp,
                color = Brand,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Dismiss",
                tint = Brand,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onDismiss() }
            )
        }
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
        val context = LocalContext.current
        val bitmap = remember(message.id) {
            BitmapFactory.decodeByteArray(message.imageBytes, 0, message.imageBytes.size)?.asImageBitmap()
        }
        var showImageActions by remember { mutableStateOf(false) }
        var showFullscreen by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Generated image",
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .combinedClickable(
                            onClick = { showFullscreen = true },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showImageActions = true
                            }
                        )
                )
            }
            if (showImageActions) {
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
                        ActionButton(stringResource(R.string.save_image), R.drawable.ic_photos) {
                            saveImageToGallery(context, message.imageBytes)
                            showImageActions = false
                        }
                        ActionButton(stringResource(R.string.share), R.drawable.ic_share) {
                            shareImage(context, message.imageBytes)
                            showImageActions = false
                        }
                    }
                }
            }
        }

        if (showFullscreen && bitmap != null) {
            FullscreenImageDialog(
                bitmap = bitmap,
                onDismiss = { showFullscreen = false },
                onSave = { saveImageToGallery(context, message.imageBytes) },
                onShare = { shareImage(context, message.imageBytes) }
            )
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
                    ActionButton(stringResource(R.string.copy), R.drawable.ic_document) { onCopy(); showActions = false }
                    ActionButton(
                        if (playingId == message.id) stringResource(R.string.stop) else stringResource(R.string.speak),
                        R.drawable.ic_notification
                    ) { onSpeak(); showActions = false }
                    ActionButton("👍", tint = if (message.thumbsUp == true) Brand else null) { onThumbsUp(); showActions = false }
                    ActionButton("👎", tint = if (message.thumbsUp == false) Brand else null) { onThumbsDown(); showActions = false }
                    if (isLast) {
                        ActionButton(stringResource(R.string.redo), R.drawable.ic_history) { onRegenerate(); showActions = false }
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

private fun saveImageToGallery(context: Context, imageBytes: ByteArray?) {
    imageBytes ?: return
    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "SecureAI_${System.currentTimeMillis()}.png")
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Secure AI")
    }
    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { out -> out.write(imageBytes) }
        android.widget.Toast.makeText(context, "Image saved to gallery", android.widget.Toast.LENGTH_SHORT).show()
    }
}

private fun shareImage(context: Context, imageBytes: ByteArray?) {
    imageBytes ?: return
    val values = android.content.ContentValues().apply {
        put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, "SecureAI_share_${System.currentTimeMillis()}.png")
        put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Secure AI")
    }
    val uri = context.contentResolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
    context.contentResolver.openOutputStream(uri)?.use { out -> out.write(imageBytes) }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share image"))
}

@Composable
private fun FullscreenImageDialog(
    bitmap: androidx.compose.ui.graphics.ImageBitmap,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Generated image fullscreen",
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .clickable { }
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                IconButton(onClick = { onSave(); onDismiss() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_photos),
                        contentDescription = "Save",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { onShare(); onDismiss() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
