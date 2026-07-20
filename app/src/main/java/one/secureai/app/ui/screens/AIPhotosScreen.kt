package one.secureai.app.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import one.secureai.app.data.store.LibraryStore
import one.secureai.app.network.ImageService
import one.secureai.app.R
import one.secureai.app.ui.theme.Brand

private data class AIStyle(val id: String, val name: String, val colors: List<Color>, val promptPrefix: String)

private val Styles = listOf(
    AIStyle("photo", "Photorealistic", listOf(Color(0xFF0F2027), Color(0xFF2C5364)), "A photorealistic, ultra-detailed photograph of"),
    AIStyle("anime", "Anime", listOf(Color(0xFFFC466B), Color(0xFF3F5EFB)), "An anime-style illustration of"),
    AIStyle("watercolor", "Watercolor", listOf(Color(0xFF11998E), Color(0xFF38EF7D)), "A beautiful watercolor painting of"),
    AIStyle("oil", "Oil Painting", listOf(Color(0xFFF12711), Color(0xFFF5AF19)), "A classical oil painting of"),
    AIStyle("digital", "Digital Art", listOf(Color(0xFF6441A5), Color(0xFF2A0845)), "A vibrant digital art illustration of"),
    AIStyle("sketch", "Pencil Sketch", listOf(Color(0xFF373B44), Color(0xFF4286F4)), "A detailed pencil sketch drawing of"),
    AIStyle("cyber", "Cyberpunk", listOf(Color(0xFF00D2FF), Color(0xFF7B2FF7)), "A cyberpunk neon-lit scene of"),
    AIStyle("fantasy", "Fantasy", listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)), "An epic fantasy art painting of"),
    AIStyle("3d", "3D Render", listOf(Color(0xFF314755), Color(0xFF26A0DA)), "A clean 3D rendered scene of"),
    AIStyle("pixel", "Pixel Art", listOf(Color(0xFFE44D26), Color(0xFFF16529)), "16-bit pixel art of"),
    AIStyle("minimal", "Minimalist", listOf(Color(0xFF2C3E50), Color(0xFF3498DB)), "A clean minimalist illustration of"),
    AIStyle("pop", "Pop Art", listOf(Color(0xFFF7971E), Color(0xFFFFD200)), "A bold pop art style image of"),
)

private enum class PhotoFilter(val label: String) {
    ALL("All"), AI_GENERATED("AI Generated"), UPLOADED("Uploaded"),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AIPhotosScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allItems by LibraryStore.items.collectAsState()
    val hasLoaded by LibraryStore.hasLoaded.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showCreateSheet by remember { mutableStateOf(false) }
    var filterMode by remember { mutableStateOf(PhotoFilter.ALL) }
    var editSourceUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { LibraryStore.load() }

    val photos = remember(allItems, filterMode) {
        var result = allItems.filter { it.isImage }
        result = when (filterMode) {
            PhotoFilter.ALL -> result
            PhotoFilter.AI_GENERATED -> result.filter { it.tags.contains("ai-generated") }
            PhotoFilter.UPLOADED -> result.filter { !it.tags.contains("ai-generated") }
        }
        result
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris ->
        uris.forEach { uri ->
            scope.launch {
                val bytes = runCatching { context.contentResolver.openInputStream(uri)?.readBytes() }.getOrNull()
                if (bytes != null) {
                    LibraryStore.add("photo_${System.currentTimeMillis()}.jpg", "image/jpeg", bytes)
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            scope.launch {
                LibraryStore.add("photo_${System.currentTimeMillis()}.jpg", "image/jpeg", stream.toByteArray())
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) cameraLauncher.launch(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Photos", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Photo Library") },
                                leadingIcon = { Icon(Icons.Default.Photo, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Take Photo") },
                                leadingIcon = { Icon(Icons.Default.Camera, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Create Image") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    showCreateSheet = true
                                }
                            )
                            androidx.compose.material3.HorizontalDivider()
                            PhotoFilter.entries.forEach { mode ->
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
            if (filterMode != PhotoFilter.ALL) {
                Surface(
                    onClick = { filterMode = PhotoFilter.ALL },
                    shape = RoundedCornerShape(50),
                    color = Brand.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(filterMode.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Brand)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Close, contentDescription = "Clear filter", tint = Brand, modifier = Modifier.size(13.dp))
                    }
                }
            }

            if (!hasLoaded) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (photos.isEmpty()) {
                EmptyPhotosState(onCreate = { showCreateSheet = true })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(photos, key = { it.id }) { photo ->
                        var showPhotoMenu by remember { mutableStateOf(false) }
                        Box {
                            AsyncImage(
                                model = photo.downloadURL,
                                contentDescription = photo.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = { showPhotoMenu = true }
                                    )
                            )
                            DropdownMenu(expanded = showPhotoMenu, onDismissRequest = { showPhotoMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.edit_image)) },
                                    leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                    onClick = {
                                        showPhotoMenu = false
                                        editSourceUrl = photo.downloadURL
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.delete)) },
                                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color.Red) },
                                    onClick = {
                                        showPhotoMenu = false
                                        scope.launch { LibraryStore.delete(photo) }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet || editSourceUrl != null) {
        CreateImageSheet(
            editSourceUrl = editSourceUrl,
            onDismiss = { showCreateSheet = false; editSourceUrl = null },
            onSaved = { showCreateSheet = false; editSourceUrl = null }
        )
    }
}

@Composable
private fun EmptyPhotosState(onCreate: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier.size(80.dp).background(Brand.copy(alpha = 0.1f), RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Photo, contentDescription = null, tint = Brand, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("No photos yet", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Upload, capture, or generate one with AI.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Surface(onClick = onCreate, shape = RoundedCornerShape(50), color = Brand) {
                Row(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Create", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateImageSheet(editSourceUrl: String? = null, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var prompt by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf<AIStyle?>(null) }
    var isGenerating by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val imageService = remember { ImageService() }
    val isEdit = editSourceUrl != null

    fun generate() {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return
        isGenerating = true
        error = null
        scope.launch {
            if (isEdit) {
                val result = runCatching {
                    val imgBytes = okhttp3.OkHttpClient().newCall(
                        okhttp3.Request.Builder().url(editSourceUrl!!).build()
                    ).execute().use { it.body?.bytes() ?: throw ImageService.ImageError("Failed to load image") }
                    val styleClause = selectedStyle?.let { " Render the result as ${it.name.lowercase()}." } ?: ""
                    imageService.edit(imgBytes, "$trimmed.$styleClause", context)
                }
                result.onSuccess { bytes ->
                    LibraryStore.add("edit_${System.currentTimeMillis()}.png", "image/png", bytes, tags = listOf("ai-generated"))
                    isGenerating = false
                    onSaved()
                }.onFailure {
                    error = it.message ?: "Couldn't edit the image. Try again."
                    isGenerating = false
                }
            } else {
                val fullPrompt = selectedStyle?.let { "${it.promptPrefix} $trimmed" } ?: trimmed
                val result = runCatching { imageService.generate(fullPrompt, context) }
                result.onSuccess { bytes ->
                    LibraryStore.add("ai_${System.currentTimeMillis()}.png", "image/png", bytes, tags = listOf("ai-generated"))
                    isGenerating = false
                    onSaved()
                }.onFailure {
                    error = it.message ?: "Couldn't generate an image. Try again."
                    isGenerating = false
                }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text(
                if (isEdit) stringResource(R.string.edit_image) else "Create Image",
                fontSize = 20.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(16.dp))

            if (isEdit) {
                AsyncImage(
                    model = editSourceUrl,
                    contentDescription = "Source image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.height(16.dp))
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.height(220.dp)
            ) {
                items(Styles, key = { it.id }) { style ->
                    val isSelected = selectedStyle?.id == style.id
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(Brush.linearGradient(style.colors), RoundedCornerShape(12.dp))
                            .then(
                                if (isSelected) Modifier.background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                else Modifier
                            )
                    ) {
                        Surface(
                            onClick = { selectedStyle = if (isSelected) null else style },
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text(
                                    style.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text(if (isEdit) stringResource(R.string.describe_edit) else "Describe what you want to see...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
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
        }
    }
}
