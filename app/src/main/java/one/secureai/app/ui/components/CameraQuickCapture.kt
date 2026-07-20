package one.secureai.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.jeziellago.compose.markdowntext.MarkdownText

/** One quick-action chip shown after a camera capture — label + the prompt
 * sent to the AI alongside the photo. Mirrors iOS's quick-action set. */
data class QuickAction(val label: String, val prompt: String, val isScanCode: Boolean = false)

val CameraQuickActions = listOf(
    QuickAction("Identify", "Identify what's in this photo. Be specific and concise."),
    QuickAction("Extract Text", "Extract all text visible in this photo, exactly as written."),
    QuickAction("Translate", "Translate any text in this photo. Show the original and the translation."),
    QuickAction("Explain", "Explain what's shown in this photo in simple terms."),
    QuickAction("Analyze Food", "Identify this food and estimate its nutrition (calories, protein, carbs, fat)."),
    QuickAction("Identify Places", "Identify the location or landmark shown in this photo, if any."),
    QuickAction("Shopping", "Identify this product and describe what it is and roughly what it costs."),
    QuickAction("Solve & Explain", "Solve the problem shown in this photo and explain the steps."),
    QuickAction("Scan Code", "Read and report the value encoded in this barcode or QR code.", isScanCode = true),
)

@Composable
fun QuickActionChipsRow(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onSelect: (QuickAction) -> Unit
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(CameraQuickActions) { action ->
            Surface(
                onClick = { if (enabled) onSelect(action) },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(36.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    Text(action.label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Polished overlay shown after a quick action resolves — photo + compact
 * answer card + Copy/Ask More, instead of opening the full chat thread
 * (Android equivalent of iOS's CameraResultOverlayView). */
@Composable
fun CameraResultOverlay(
    imageBitmap: android.graphics.Bitmap,
    isLoading: Boolean,
    answer: String?,
    error: String?,
    onCopy: () -> Unit,
    onAskMore: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.CenterEnd) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Image(
                bitmap = imageBitmap.asImageBitmap(),
                contentDescription = "Captured photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp)
            )

            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    when {
                        isLoading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(10.dp))
                                Text("Thinking…", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        error != null -> {
                            Text(error, color = Color(0xFFFF6B6B), fontSize = 15.sp)
                        }
                        answer != null -> {
                            MarkdownText(
                                markdown = answer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .height(160.dp),
                                fontSize = 15.sp
                            )
                        }
                    }

                    if (!isLoading && (answer != null || error != null)) {
                        Spacer(Modifier.size(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(
                                onClick = onAskMore,
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFF2563EB),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                                    Text("Ask More", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                                }
                            }
                            if (answer != null) {
                                Surface(
                                    onClick = onCopy,
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
