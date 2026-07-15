package one.secureai.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import one.secureai.app.R
import one.secureai.app.data.store.Project
import one.secureai.app.data.store.ProjectStore
import one.secureai.app.ui.theme.Brand

private val PRESET_EMOJIS = listOf(
    "📁", "💼", "🎯", "🚀", "💡", "🔬", "📊", "🎨",
    "📝", "🏗️", "🌐", "🤖", "📚", "🎮", "🏠", "❤️",
    "⚡", "🔥", "🌟", "🎵", "🏋️", "🍳", "✈️", "🎓"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onBack: () -> Unit,
    onSelectProject: (Project) -> Unit
) {
    val projects by ProjectStore.projects.collectAsState()
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }
    var editingProject by remember { mutableStateOf<Project?>(null) }

    LaunchedEffect(Unit) { ProjectStore.load() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Projects", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
                    editingProject = null
                    showSheet = true
                },
                containerColor = Brand
            ) {
                Icon(Icons.Default.Add, contentDescription = "New project", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("No projects yet", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.projects_empty),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    Surface(
                        onClick = { onSelectProject(project) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(project.emoji, fontSize = 28.sp)
                            androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(project.name, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                if (project.systemPrompt.isNotBlank()) {
                                    Text(
                                        project.systemPrompt.take(60),
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                            IconButton(onClick = {
                                editingProject = project
                                showSheet = true
                            }) {
                                Text("✏️", fontSize = 16.sp)
                            }
                            IconButton(onClick = {
                                ProjectStore.delete(project)
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        ProjectFormSheet(
            project = editingProject,
            onDismiss = { showSheet = false },
            onSave = { name, emoji, prompt ->
                if (editingProject != null) {
                    ProjectStore.update(editingProject!!.copy(name = name, emoji = emoji, systemPrompt = prompt))
                } else {
                    val p = ProjectStore.add(name, emoji, prompt)
                    onSelectProject(p)
                }
                showSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProjectFormSheet(
    project: Project?,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, systemPrompt: String) -> Unit
) {
    var name by remember(project) { mutableStateOf(project?.name ?: "") }
    var emoji by remember(project) { mutableStateOf(project?.emoji ?: "📁") }
    var systemPrompt by remember(project) { mutableStateOf(project?.systemPrompt ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                if (project != null) stringResource(R.string.edit_project) else stringResource(R.string.new_project),
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 56.sp)
            }

            Spacer(Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PRESET_EMOJIS.forEach { e ->
                    Surface(
                        onClick = { emoji = e },
                        shape = RoundedCornerShape(8.dp),
                        color = if (e == emoji)
                            Brand.copy(alpha = 0.2f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            e,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(100) },
                label = { Text("Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = systemPrompt,
                onValueChange = { systemPrompt = it.take(10_000) },
                label = { Text("System Prompt (optional)") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 6
            )

            Spacer(Modifier.height(20.dp))

            Surface(
                onClick = {
                    if (name.isNotBlank()) onSave(name, emoji, systemPrompt)
                },
                shape = RoundedCornerShape(14.dp),
                color = if (name.isNotBlank()) Brand else Brand.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        if (project != null) "Save" else "Create",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
