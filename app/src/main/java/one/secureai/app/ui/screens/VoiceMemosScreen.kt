package one.secureai.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.secureai.app.data.store.VoiceMemo
import one.secureai.app.data.store.VoiceMemoStore
import one.secureai.app.network.AIFeatureService
import one.secureai.app.ui.theme.Brand
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

private enum class FilterMode(val label: String) {
    ALL("All"),
    TRANSCRIBED("Transcribed"),
    SUMMARIZED("Summarized"),
    UNTRANSCRIBED("Not Transcribed"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceMemosScreen(onBack: () -> Unit, projectId: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allMemos by VoiceMemoStore.memos.collectAsState()
    val isLoading by VoiceMemoStore.isLoading.collectAsState()
    val isSaving by VoiceMemoStore.isSaving.collectAsState()
    val saveError by VoiceMemoStore.saveError.collectAsState()

    var isRecording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var micDenied by remember { mutableStateOf(false) }

    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }

    var transcribingId by remember { mutableStateOf<String?>(null) }
    var summarizingId by remember { mutableStateOf<String?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf(FilterMode.ALL) }
    var showMenu by remember { mutableStateOf(false) }

    fun stopPlayback() {
        player?.release()
        player = null
        playingId = null
    }

    fun playMemo(memo: VoiceMemo) {
        stopPlayback()
        val url = memo.downloadURL ?: return
        val mp = MediaPlayer()
        runCatching {
            mp.setDataSource(url)
            mp.setOnCompletionListener { stopPlayback() }
            mp.prepareAsync()
            mp.setOnPreparedListener { it.start() }
            player = mp
            playingId = memo.id
        }
    }

    @Suppress("DEPRECATION")
    fun beginRecording() {
        val file = File.createTempFile("memo_", ".m4a", context.cacheDir)
        val rec = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
        }
        runCatching {
            rec.prepare()
            rec.start()
            recorder = rec
            recordingFile = file
            isRecording = true
            elapsedMs = 0
        }.onFailure {
            rec.release()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) beginRecording() else micDenied = true
    }

    fun startRecording() {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (hasPermission) beginRecording() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun stopAndSave() {
        val rec = recorder ?: return
        val file = recordingFile
        val durationSec = elapsedMs / 1000.0
        runCatching { rec.stop() }
        rec.release()
        recorder = null
        isRecording = false
        if (file == null || durationSec < 0.5) {
            file?.delete()
            return
        }
        val title = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(java.util.Date())
        VoiceMemoStore.save(file, title, durationSec, projectId)
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(100)
            elapsedMs += 100
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.let { runCatching { it.stop() }; it.release() }
            player?.release()
        }
    }

    LaunchedEffect(Unit) { VoiceMemoStore.load() }

    val memos = remember(allMemos, projectId, filterMode, searchText) {
        var result = allMemos
        if (projectId != null) result = result.filter { it.projectId == projectId }
        result = when (filterMode) {
            FilterMode.ALL -> result
            FilterMode.TRANSCRIBED -> result.filter { it.transcript != null }
            FilterMode.SUMMARIZED -> result.filter { it.summary != null }
            FilterMode.UNTRANSCRIBED -> result.filter { it.transcript == null }
        }
        if (searchText.isNotEmpty()) {
            val q = searchText.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) ||
                    (it.transcript?.lowercase()?.contains(q) ?: false) ||
                    (it.summary?.lowercase()?.contains(q) ?: false)
            }
        }
        result
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Voice", fontWeight = FontWeight.SemiBold) },
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
                            DropdownMenuItem(text = { Text("Search") }, onClick = {
                                isSearching = !isSearching
                                showMenu = false
                            })
                            FilterMode.entries.forEach { mode ->
                                DropdownMenuItem(text = { Text(mode.label) }, onClick = {
                                    filterMode = mode
                                    showMenu = false
                                })
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
            if (isSearching) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search memos...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        colors = TextFieldDefaults.colors(
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { isSearching = false; searchText = "" }) { Text("Cancel") }
                }
            }

            if (filterMode != FilterMode.ALL) {
                Surface(
                    onClick = { filterMode = FilterMode.ALL },
                    shape = RoundedCornerShape(50),
                    color = Brand.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading && memos.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    memos.isEmpty() && !isRecording && !isSaving -> {
                        if (searchText.isEmpty() && filterMode == FilterMode.ALL) {
                            EmptyVoiceState()
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.height(8.dp))
                                    Text("No results", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Try a different search or filter", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isSaving) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                                            .padding(16.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Text("Saving...", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            items(memos, key = { it.id }) { memo ->
                                MemoCard(
                                    memo = memo,
                                    isPlaying = playingId == memo.id,
                                    isExpanded = expandedId == memo.id,
                                    isTranscribing = transcribingId == memo.id,
                                    isSummarizing = summarizingId == memo.id,
                                    onTogglePlay = {
                                        if (playingId == memo.id) stopPlayback() else playMemo(memo)
                                    },
                                    onToggleExpand = {
                                        expandedId = if (expandedId == memo.id) null else memo.id
                                    },
                                    onTranscribe = {
                                        if (memo.storagePath.isNotEmpty() || memo.downloadURL != null) {
                                            transcribingId = memo.id
                                            scope.launch {
                                                val bytes = withContext(Dispatchers.IO) {
                                                    runCatching {
                                                        java.net.URL(memo.downloadURL).readBytes()
                                                    }.getOrNull()
                                                }
                                                if (bytes != null) {
                                                    val transcript = AIFeatureService.transcribe(bytes, "${memo.id}.m4a")
                                                    if (transcript != null) {
                                                        VoiceMemoStore.updateTranscript(memo.id, transcript)
                                                        expandedId = memo.id
                                                    }
                                                }
                                                transcribingId = null
                                            }
                                        }
                                    },
                                    onSummarize = {
                                        val transcript = memo.transcript
                                        if (transcript != null) {
                                            summarizingId = memo.id
                                            scope.launch {
                                                val summary = AIFeatureService.summarizeText(transcript)
                                                if (summary != null) VoiceMemoStore.updateSummary(memo.id, summary)
                                                summarizingId = null
                                            }
                                        }
                                    },
                                    onShare = {
                                        val parts = mutableListOf(memo.title)
                                        memo.transcript?.let { parts.add("\n\nTranscript:\n$it") }
                                        memo.summary?.let { parts.add("\n\nSummary:\n$it") }
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, parts.joinToString(""))
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, null))
                                    },
                                    onDelete = { VoiceMemoStore.delete(memo) }
                                )
                            }
                        }
                    }
                }
            }

            if (isRecording) {
                RecordingBar(elapsedMs = elapsedMs, onStop = { stopAndSave() })
            } else {
                RecordButton(onClick = { startRecording() })
            }
        }
    }

    if (micDenied) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { micDenied = false },
            title = { Text("Microphone Access Required") },
            text = { Text("Enable microphone access in Settings to record voice memos.") },
            confirmButton = {
                TextButton(onClick = {
                    micDenied = false
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { micDenied = false }) { Text("Cancel") }
            }
        )
    }

    if (saveError != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { VoiceMemoStore.clearSaveError() },
            title = { Text("Error") },
            text = { Text(saveError ?: "") },
            confirmButton = {
                TextButton(onClick = { VoiceMemoStore.clearSaveError() }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun RecordButton(onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).background(Color.Red, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Record", tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text("Tap to Record", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RecordingBar(elapsedMs: Long, onStop: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(Color.Red.copy(alpha = 2f - pulseScale), CircleShape)
        )
        Spacer(Modifier.width(16.dp))
        val totalSec = elapsedMs / 1000
        Text(
            "%d:%02d".format(totalSec / 60, totalSec % 60),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Spacer(Modifier.weight(1f))
        IconButton(
            onClick = onStop,
            modifier = Modifier.size(44.dp).background(Color.Red, RoundedCornerShape(12.dp))
        ) {
            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
        }
    }
}

@Composable
private fun EmptyVoiceState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(80.dp).background(Brand.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Brand, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("No voice memos yet", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text("Tap record below to get started", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MemoCard(
    memo: VoiceMemo,
    isPlaying: Boolean,
    isExpanded: Boolean,
    isTranscribing: Boolean,
    isSummarizing: Boolean,
    onTogglePlay: () -> Unit,
    onToggleExpand: () -> Unit,
    onTranscribe: () -> Unit,
    onSummarize: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onTogglePlay) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (isPlaying) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Stop" else "Play",
                            tint = if (isPlaying) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(memo.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Row {
                        Text(
                            memo.formattedDuration,
                            fontSize = 13.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(" · ", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            SimpleDateFormat("MMM d", Locale.getDefault()).format(memo.createdAt),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (memo.transcript != null) {
                    IconButton(onClick = onToggleExpand) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle transcript",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (memo.transcript == null || memo.summary == null) {
                AiActionBar(
                    memo = memo,
                    isTranscribing = isTranscribing,
                    isSummarizing = isSummarizing,
                    onTranscribe = onTranscribe,
                    onSummarize = onSummarize,
                    onShare = onShare
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onShare, modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp))
                    }
                }
            }

            if (isExpanded) {
                Column(modifier = Modifier.padding(14.dp)) {
                    memo.transcript?.let {
                        Text("TRANSCRIPT", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(it, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                    }
                    memo.summary?.let {
                        Text("SUMMARY", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(it, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AiActionBar(
    memo: VoiceMemo,
    isTranscribing: Boolean,
    isSummarizing: Boolean,
    onTranscribe: () -> Unit,
    onSummarize: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (memo.transcript == null) {
            Surface(
                onClick = onTranscribe,
                shape = RoundedCornerShape(50),
                color = Brand.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = Brand)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Transcribe", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand)
                }
            }
        } else if (memo.summary == null) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color(0xFF34C759))
            Spacer(Modifier.width(4.dp))
            Text("Transcribed", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Surface(
                onClick = onSummarize,
                shape = RoundedCornerShape(50),
                color = Brand.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isSummarizing) {
                        CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(13.dp), tint = Brand)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Summarize", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onShare, modifier = Modifier.size(34.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)) {
            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
        }
    }
}
