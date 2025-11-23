package com.alley.digitalmemory.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep // Trash Icon
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.alley.digitalmemory.BiometricHelper
import com.alley.digitalmemory.data.Note
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    state: List<Note>,
    searchText: String,
    onEvent: (NoteEvent) -> Unit,
    onAddNoteClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onTrashClick: () -> Unit ,// NEW CALLBACK
    onBackupClick: () -> Unit
) {
    val context = LocalContext.current
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    // SOFT DELETE DIALOG
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(text = "Move to Trash?") },
            text = { Text("You can restore this note from the Recycle Bin.") },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // UPDATED: Use MoveToTrash instead of DeleteNote
                        noteToDelete?.let { onEvent(NoteEvent.MoveToTrash(it)) }
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Move to Trash") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNoteClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Note")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ROW: SEARCH + VIEW TOGGLE + TRASH ICON
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { onEvent(NoteEvent.OnSearchQueryChange(it)) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                // View Toggle
                FilledTonalIconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        imageVector = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                        contentDescription = "Toggle View"
                    )
                }

                // NEW: TRASH BUTTON
                FilledTonalIconButton(
                    onClick = onTrashClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Trash",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                // 4. NEW: BACKUP BUTTON (Add this block)
                FilledTonalIconButton(
                    onClick = onBackupClick,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Backup",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalItemSpacing = 12.dp,
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state, key = { it.id }) { note ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = {
                            if (it == SwipeToDismissBoxValue.EndToStart) {
                                noteToDelete = note
                                return@rememberSwipeToDismissBoxState false
                            }
                            false
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = { DeleteBackground(dismissState) },
                        enableDismissFromStartToEnd = false
                    ) {
                        NoteItem(
                            note = note,
                            isMinimal = !isGridView,
                            onNoteClick = {
                                if (note.isSecret) {
                                    BiometricHelper.authenticate(context) { onNoteClick(note) }
                                } else {
                                    onNoteClick(note)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ... DeleteBackground and NoteItem are same as before ...
// (Add them here to complete the file, or keep existing ones)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        Color.Transparent
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, shape = MaterialTheme.shapes.medium)
            .padding(16.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
fun NoteItem(
    note: Note,
    isMinimal: Boolean,
    onNoteClick: () -> Unit
) {
    Card(
        onClick = onNoteClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            if (note.isSecret) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            } else if (!isMinimal && note.imagePaths.isNotEmpty()) {
                val firstPath = note.imagePaths.first()
                AsyncImage(
                    model = File(firstPath),
                    contentDescription = "Note Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(
                        onClick = { },
                        label = {
                            Text(text = note.category, style = MaterialTheme.typography.labelSmall)
                        },
                        modifier = Modifier.height(24.dp)
                    )

                    if (isMinimal && !note.isSecret) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (note.imagePaths.isNotEmpty()) {
                            Icon(Icons.Default.Image, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (note.filePaths.isNotEmpty()) {
                            Icon(Icons.Default.AttachFile, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Import parseMarkdown at top

                if (note.isSecret) {
                    Text(
                        text = "• • • • • • • •",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                } else {
                    // USE MARKDOWN PARSER
                    Text(
                        text = com.alley.digitalmemory.parseMarkdown(note.content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isMinimal) 2 else 4
                    )
                }
            }
        }
    }
}