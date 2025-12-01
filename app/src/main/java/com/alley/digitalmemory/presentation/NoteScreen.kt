package com.alley.digitalmemory.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.alley.digitalmemory.BiometricHelper
import com.alley.digitalmemory.R
import com.alley.digitalmemory.data.Note
import java.io.File
// IMPORTS FOR RICH TEXT DISPLAY
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    state: List<Note>,
    searchText: String,
    onEvent: (NoteEvent) -> Unit,
    onAddNoteClick: () -> Unit,
    onNoteClick: (Note) -> Unit,
    onTrashClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    val context = LocalContext.current
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    // STATES FOR SEARCH & MENU
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // DELETE DIALOG
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Move to Trash?") },
            text = { Text("You can restore this note from the Recycle Bin.") },
            icon = { Icon(Icons.Default.Delete, contentDescription = null) },
            confirmButton = {
                Button(
                    onClick = { noteToDelete?.let { onEvent(NoteEvent.MoveToTrash(it)) }; noteToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Move to Trash") }
            },
            dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddNoteClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) { Icon(Icons.Default.Add, "Add Note", modifier = Modifier.size(28.dp)) }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // TOP BAR
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(50), shadowElevation = 4.dp, color = Color.White) {
                AnimatedContent(targetState = isSearchActive, label = "SearchBarAnimation") { active ->
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp).fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (active) {
                            IconButton(onClick = { isSearchActive = false; onEvent(NoteEvent.OnSearchQueryChange("")) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Gray) }
                            Box(modifier = Modifier.weight(1f)) {
                                if (searchText.isEmpty()) Text("Search...", color = Color.LightGray, fontSize = 16.sp)
                                BasicTextField(value = searchText, onValueChange = { onEvent(NoteEvent.OnSearchQueryChange(it)) }, singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, color = Color.Black), modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
                            }
                            if (searchText.isNotEmpty()) { IconButton(onClick = { onEvent(NoteEvent.OnSearchQueryChange("")) }) { Icon(Icons.Default.Close, "Clear", tint = Color.Gray) } }
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = "All Notes", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = Color.DarkGray), modifier = Modifier.weight(1f))
                            IconButton(onClick = { isSearchActive = true }) { Icon(Icons.Default.Search, "Search", tint = Color.Gray) }
                            Box {
                                IconButton(onClick = { showMenu = true }) { Icon(Icons.Default.MoreVert, "Menu", tint = Color.Gray) }
                                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color.White)) {
                                    DropdownMenuItem(text = { Text(if (isGridView) "List View" else "Grid View") }, leadingIcon = { Icon(if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView, null) }, onClick = { isGridView = !isGridView; showMenu = false })
                                    DropdownMenuItem(text = { Text("Recycle Bin") }, leadingIcon = { Icon(Icons.Default.DeleteSweep, null) }, onClick = { onTrashClick(); showMenu = false })
                                    DropdownMenuItem(text = { Text("Backup/Restore") }, leadingIcon = { Icon(Icons.Default.CloudUpload, null) }, onClick = { onBackupClick(); showMenu = false })
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(id = R.drawable.img_empty_state), contentDescription = null, modifier = Modifier.size(150.dp), tint = Color.Unspecified)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Start your journey", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(if (isGridView) 2 else 1),
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(state, key = { it.id }) { note ->
                        val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { if (it == SwipeToDismissBoxValue.EndToStart) { noteToDelete = note; return@rememberSwipeToDismissBoxState false }; false })
                        SwipeToDismissBox(state = dismissState, backgroundContent = { DeleteBackground(dismissState) }, enableDismissFromStartToEnd = false) {
                            NoteItem(note = note, isMinimal = !isGridView, onNoteClick = { if (note.isSecret) BiometricHelper.authenticate(context) { onNoteClick(note) } else onNoteClick(note) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteBackground(dismissState: SwipeToDismissBoxState) {
    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color(0xFFFFCDD2) else Color.Transparent
    Box(modifier = Modifier.fillMaxSize().background(color, shape = RoundedCornerShape(12.dp)).padding(16.dp), contentAlignment = Alignment.CenterEnd) { Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFD32F2F)) }
}

@Composable
fun NoteItem(
    note: Note,
    isMinimal: Boolean,
    onNoteClick: () -> Unit
) {
    val context = LocalContext.current // Context chahiye file open karne ke liye

    Card(
        onClick = onNoteClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (note.isSecret) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)) else null
    ) {
        Column {
            // 1. IMAGE SECTION
            if (note.isSecret) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Brush.linearGradient(colors = listOf(Color(0xFFE0EAFC), Color(0xFFCFDEF3)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked", modifier = Modifier.size(32.dp), tint = Color(0xFF546E7A))
                }
            } else if (!isMinimal && note.imagePaths.isNotEmpty()) {
                val firstPath = note.imagePaths.first()
                AsyncImage(
                    model = File(firstPath),
                    contentDescription = "Note Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // 2. CONTENT SECTION
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(6.dp))

                // CHIPS ROW
                if (note.category != "Uncategorized") {
                    Surface(color = getCategoryColor(note.category), shape = RoundedCornerShape(8.dp)) {
                        Text(text = note.category, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = Color.DarkGray)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isMinimal && !note.isSecret) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (note.imagePaths.isNotEmpty()) { Icon(Icons.Default.Image, null, Modifier.size(14.dp), tint = Color.Gray); Spacer(modifier = Modifier.width(6.dp)) }
                        if (note.filePaths.isNotEmpty()) { Icon(Icons.Default.AttachFile, null, Modifier.size(14.dp), tint = Color.Gray); Spacer(modifier = Modifier.width(6.dp)) }
                    }
                    if (note.imagePaths.isNotEmpty() || note.filePaths.isNotEmpty()) Spacer(modifier = Modifier.height(6.dp))
                }

                // 3. TEXT CONTENT (Fixed Link Handling)
                if (note.isSecret) {
                    Text(
                        text = "Unlock to view content",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                } else {
                    // RICH TEXT DISPLAY
                    val richState = rememberRichTextState()
                    LaunchedEffect(note.content) {
                        richState.setHtml(note.content)
                    }

                    // --- MAGIC FIX: INTERCEPT LINK CLICKS ---
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalUriHandler provides object : androidx.compose.ui.platform.UriHandler {
                        override fun openUri(uri: String) {
                            // Jab link par click ho, hamara function call karo
                            com.alley.digitalmemory.openFile(context, uri)
                        }
                    }) {
                        RichText(
                            state = richState,
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF424242), lineHeight = 20.sp),
                            maxLines = if (isMinimal) 2 else 6,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

fun getCategoryColor(category: String): Color {
    return when (category) {
        "Personal" -> Color(0xFFFFF9C4)
        "Work" -> Color(0xFFE1BEE7)
        "Ideas" -> Color(0xFFB2DFDB)
        else -> Color(0xFFF5F5F5)
    }
}