package one.secureai.app.ui.screens

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import kotlinx.coroutines.launch
import one.secureai.app.R
import one.secureai.app.data.store.Note
import one.secureai.app.data.store.NoteStore
import one.secureai.app.network.AIFeatureService
import one.secureai.app.ui.theme.Brand
import java.text.DateFormat
import java.util.Locale

private enum class SortMode(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    ALPHABETICAL("A–Z"),
}

private val CardColors = listOf(
    Color(0xFFFFCC00), Color(0xFFFF9500), Color(0xFF00C7BE),
    Color(0xFF32ADE6), Color(0xFF5856D6), Color(0xFFFF2D55),
    Color(0xFF34C759), Color(0xFFAF52DE)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(onBack: () -> Unit, projectId: String? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allNotes by NoteStore.notes.collectAsState()
    val isLoading by NoteStore.isLoading.collectAsState()

    var editingNote by remember { mutableStateOf<Note?>(null) }
    var showAIWrite by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(SortMode.NEWEST) }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { NoteStore.load() }

    val notes = remember(allNotes, projectId, searchText, sortMode) {
        var result = allNotes
        if (projectId != null) result = result.filter { it.projectId == projectId }
        if (searchText.isNotEmpty()) {
            val q = searchText.lowercase()
            result = result.filter { it.title.lowercase().contains(q) || it.body.lowercase().contains(q) }
        }
        when (sortMode) {
            SortMode.NEWEST -> result.sortedByDescending { it.updatedAt }
            SortMode.OLDEST -> result.sortedBy { it.updatedAt }
            SortMode.ALPHABETICAL -> result.sortedBy { (it.title.ifEmpty { "Untitled" }).lowercase() }
        }
    }

    if (editingNote != null) {
        NoteEditor(
            note = editingNote!!,
            onDone = { editingNote = null }
        )
        return
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notes", fontWeight = FontWeight.SemiBold) },
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
                            SortMode.entries.forEach { mode ->
                                DropdownMenuItem(text = { Text(mode.label) }, onClick = {
                                    sortMode = mode
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingNote = Note(projectId = projectId)
                },
                containerColor = Brand
            ) {
                Icon(Icons.Default.Add, contentDescription = "New note", tint = Color.White)
            }
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
                        placeholder = { Text("Search notes...") },
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

            if (sortMode != SortMode.NEWEST) {
                Surface(
                    onClick = { sortMode = SortMode.NEWEST },
                    shape = RoundedCornerShape(50),
                    color = Brand.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sortMode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Close, contentDescription = "Clear sort", tint = Brand, modifier = Modifier.size(13.dp))
                    }
                }
            }

            when {
                isLoading && notes.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                notes.isEmpty() -> {
                    EmptyNotesState(onWrite = { showAIWrite = true })
                }
                else -> {
                    Column {
                        Text(
                            "${notes.size} ${if (notes.size == 1) "NOTE" else "NOTES"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(notes, key = { it.id }) { note ->
                                val index = notes.indexOf(note)
                                NoteCard(
                                    note = note,
                                    accentColor = CardColors[index % CardColors.size],
                                    onTap = { editingNote = note },
                                    onDelete = { NoteStore.delete(note) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAIWrite) {
        AIWriteSheet(
            projectId = projectId,
            onDismiss = { showAIWrite = false },
            onCreated = { note ->
                showAIWrite = false
                editingNote = note
            }
        )
    }
}

@Composable
private fun EmptyNotesState(onWrite: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp).background(Brand.copy(alpha = 0.1f), RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Brand, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("No notes yet", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Capture your ideas or generate one automatically.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(16.dp))
        Surface(onClick = onWrite, shape = RoundedCornerShape(50), color = Brand) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NoteCard(note: Note, accentColor: Color, onTap: () -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    Surface(
        onClick = onTap,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor, RoundedCornerShape(4.dp))
            )
            Column(modifier = Modifier.padding(horizontal = 14.dp).fillMaxWidth()) {
                Text(
                    note.title.ifEmpty { "Untitled" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    note.preview,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DateFormat.getDateInstance(DateFormat.SHORT).format(note.updatedAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {
                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, "${note.title.ifEmpty { "Untitled" }}\n\n${note.body}")
                        }
                        startActivity(context, android.content.Intent.createChooser(shareIntent, null), null)
                    }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(note: Note, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var title by remember { mutableStateOf(note.title) }
    var body by remember { mutableStateOf(note.body) }
    var isSummarizing by remember { mutableStateOf(false) }
    var isDictating by remember { mutableStateOf(false) }
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var dictationBase by remember { mutableStateOf("") }

    fun save() {
        if (title.trim().isEmpty() && body.trim().isEmpty()) return
        NoteStore.save(note.copy(title = title, body = body))
    }

    fun dictationIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }

    fun stopDictation() {
        isDictating = false
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun startDictation() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return
        dictationBase = body
        speechRecognizer?.destroy()
        val sr = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = sr
        sr.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { isDictating = false }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!partial.isNullOrBlank()) {
                    body = if (dictationBase.isBlank()) partial else "$dictationBase $partial"
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    dictationBase = if (dictationBase.isBlank()) text else "$dictationBase $text"
                    body = dictationBase
                }
                if (isDictating) sr.startListening(dictationIntent())
            }
        })
        sr.startListening(dictationIntent())
        isDictating = true
    }

    DisposableEffect(Unit) {
        onDispose { speechRecognizer?.destroy() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { save(); onDone() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (body.trim().isNotEmpty()) {
                        val context = LocalContext.current
                        IconButton(onClick = {
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "$title\n\n$body")
                            }
                            startActivity(context, android.content.Intent.createChooser(shareIntent, null), null)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(
                            onClick = {
                                isSummarizing = true
                                scope.launch {
                                    val result = AIFeatureService.summarizeNote(body)
                                    if (result != null) {
                                        NoteStore.save(
                                            Note(
                                                title = "Summary: ${result.title}",
                                                body = result.body,
                                                projectId = note.projectId
                                            )
                                        )
                                    }
                                    isSummarizing = false
                                }
                            },
                            enabled = !isSummarizing
                        ) {
                            if (isSummarizing) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Default.AutoAwesome, contentDescription = "Summarize")
                        }
                    }
                    TextButton(onClick = { save(); onDone() }) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (isDictating) stopDictation() else startDictation() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_mic),
                            contentDescription = if (isDictating) "Stop dictation" else "Dictate",
                            tint = if (isDictating) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isDictating) "Listening…" else "Voice",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDictating) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title") },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            TextField(
                value = body,
                onValueChange = { body = it },
                placeholder = { Text("") },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp),
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIWriteSheet(projectId: String?, onDismiss: () -> Unit, onCreated: (Note) -> Unit) {
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val examples = listOf(
        "Packing list for a weekend trip",
        "Meeting notes template",
        "Pros and cons of remote work"
    )

    fun generate() {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        isGenerating = true
        error = null
        scope.launch {
            val result = AIFeatureService.generateNote(trimmed)
            if (result == null) {
                error = "Couldn't generate a note. Try a different topic."
                isGenerating = false
                return@launch
            }
            val note = Note(title = result.title, body = result.body, projectId = projectId)
            NoteStore.save(note)
            isGenerating = false
            onCreated(note)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Write", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Write a packing list for a beach trip...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions.Default
                )
                Spacer(Modifier.width(8.dp))
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    IconButton(onClick = { generate() }, enabled = prompt.trim().isNotEmpty()) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Generate", tint = if (prompt.trim().isNotEmpty()) Brand else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "EXAMPLES",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            examples.forEach { example ->
                Surface(
                    onClick = { prompt = example },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(example, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
