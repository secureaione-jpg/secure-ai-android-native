package one.secureai.app.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import one.secureai.app.data.store.MemoryStore

private val BrandBlue = Color(0xFF2563EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(onBack: () -> Unit) {
    val entries by MemoryStore.entries.collectAsState()
    var newEntry by remember { mutableStateOf("") }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editingText by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { MemoryStore.load() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Memories", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text("Clear all", color = Color.Red, fontSize = 14.sp)
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No memories yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Things you tell Secure AI to remember appear here",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(entries, key = { index, _ -> index }) { index, entry ->
                        MemoryRow(
                            entry = entry,
                            onEdit = {
                                editingIndex = index
                                editingText = entry
                            },
                            onDelete = { MemoryStore.delete(index) }
                        )
                    }
                }
            }

            // Input bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newEntry,
                    onValueChange = { newEntry = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a memory…") },
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 4
                )
                Spacer(Modifier.width(8.dp))
                SmallFloatingActionButton(
                    onClick = {
                        MemoryStore.add(newEntry)
                        newEntry = ""
                    },
                    containerColor = if (newEntry.isBlank()) Color.Gray else MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Add", modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear all memories?") },
            text = { Text("This will permanently delete all your saved memories.") },
            confirmButton = {
                TextButton(onClick = { MemoryStore.clear(); showClearConfirm = false }) {
                    Text("Clear all", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (editingIndex != null) {
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("Edit memory") },
            text = {
                OutlinedTextField(
                    value = editingText,
                    onValueChange = { editingText = it },
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        editingIndex?.let { MemoryStore.update(it, editingText) }
                        editingIndex = null
                    },
                    enabled = editingText.isNotBlank()
                ) { Text("Save", color = BrandBlue) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        editingIndex?.let { MemoryStore.delete(it) }
                        editingIndex = null
                    }) { Text("Delete", color = Color.Red) }
                    TextButton(onClick = { editingIndex = null }) { Text("Cancel") }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoryRow(entry: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
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
                .clickable { onEdit() }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(BrandBlue)
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = entry,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

