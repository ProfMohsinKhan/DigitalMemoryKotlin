package com.alley.digitalmemory.presentation

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alley.digitalmemory.AudioRecorder
import com.alley.digitalmemory.copyFileIfSizeValid
import com.alley.digitalmemory.copyUriToInternalStorage
import com.alley.digitalmemory.getFileNameFromPath
import com.alley.digitalmemory.isAudioFile
import com.alley.digitalmemory.isVideoFile
// Library Imports
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.mohamedrejeb.richeditor.ui.material3.RichText
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    navController: NavController,
    noteId: Int,
    viewModel: NoteViewModel,
    onSave: (String, String, String, List<String>, List<String>, Boolean, Long?) -> Unit
) {
    val context = LocalContext.current

    // STATES
    var title by remember { mutableStateOf("") }
    val richTextState = rememberRichTextState()
    var isReadOnly by remember { mutableStateOf(false) }

    // Metadata
    var selectedCategory by remember { mutableStateOf("Personal") }
    var imagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var filePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSecret by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }

    // Dialogs
    var showLinkDialog by remember { mutableStateOf(false) }

    // Recording
    var isRecording by remember { mutableStateOf(false) }
    val audioRecorder = remember { AudioRecorder(context) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }

    // Delete Confirmation
    var attachmentToDelete by remember { mutableStateOf<String?>(null) }

    // Helpers
    val categories = listOf("Personal", "Work", "Ideas")
    val audioFiles = filePaths.filter { com.alley.digitalmemory.isAudioFile(it) }
    val videoFiles = filePaths.filter { com.alley.digitalmemory.isVideoFile(it) }
    val docFiles = filePaths.filter { !com.alley.digitalmemory.isAudioFile(it) && !com.alley.digitalmemory.isVideoFile(it) }

    // --- SMART DELETE ATTACHMENT ---
    fun deleteAttachment(path: String) {
        imagePaths = imagePaths - path
        filePaths = filePaths - path

        // Remove Link from Text
        val currentHtml = richTextState.toHtml()
        val regex = Regex("<a\\s+(?:[^>]*?\\s+)?href=\"${Regex.escape(path)}\".*?>.*?</a>", RegexOption.IGNORE_CASE)
        val newHtml = currentHtml.replace(regex, "")
        richTextState.setHtml(newHtml)

        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
    }

    // --- DIALOGS ---
    if (attachmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { attachmentToDelete = null },
            title = { Text("Delete Attachment?") },
            text = { Text("This will remove the file and its link from the note.") },
            icon = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            confirmButton = { Button(onClick = { attachmentToDelete?.let { deleteAttachment(it) }; attachmentToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { attachmentToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showLinkDialog) {
        var linkText by remember { mutableStateOf("") }
        var linkUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Insert External Link") },
            text = { Column { OutlinedTextField(value = linkText, onValueChange = { linkText = it }, label = { Text("Text") }); Spacer(modifier = Modifier.height(8.dp)); OutlinedTextField(value = linkUrl, onValueChange = { linkUrl = it }, label = { Text("URL") }) } },
            confirmButton = { Button(onClick = { if (linkText.isNotEmpty() && linkUrl.isNotEmpty()) { richTextState.addLink(linkText, linkUrl); showLinkDialog = false } }) { Text("Insert") } },
            dismissButton = { TextButton(onClick = { showLinkDialog = false }) { Text("Cancel") } }
        )
    }

    // --- LAUNCHERS ---
    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(context, { _, h, m -> calendar.set(Calendar.HOUR_OF_DAY, h); calendar.set(Calendar.MINUTE, m); calendar.set(Calendar.SECOND, 0); reminderTime = calendar.timeInMillis }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> calendar.set(Calendar.YEAR, y); calendar.set(Calendar.MONTH, m); calendar.set(Calendar.DAY_OF_MONTH, d); timePickerDialog.show() }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) datePickerDialog.show() }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) { isRecording = true; audioRecorder.start(File(context.filesDir, "voice_${System.currentTimeMillis()}.m4a").also { f -> currentAudioFile = f }) } }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        val newPaths = uris.mapNotNull { copyUriToInternalStorage(context, it) }
        imagePaths += newPaths
        newPaths.forEach { path -> richTextState.addLink(" 📸 ${File(path).name} ", path) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            val path = copyFileIfSizeValid(context, uri)
            if (path != null) {
                filePaths += path
                val icon = if (com.alley.digitalmemory.isVideoFile(path)) "🎬" else "📄"
                richTextState.addLink(" $icon ${File(path).name} ", path)
            }
        }
    }

    LaunchedEffect(key1 = true) {
        if (noteId != -1) {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                title = note.title
                richTextState.setHtml(note.content)
                selectedCategory = note.category; imagePaths = note.imagePaths; filePaths = note.filePaths; isSecret = note.isSecret; reminderTime = note.reminder
                isReadOnly = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRecording) "Recording..." else if (isReadOnly) "Preview" else "Edit Idea") },
                colors = if (isRecording) TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.errorContainer) else TopAppBarDefaults.topAppBarColors(),
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { isReadOnly = !isReadOnly }) { Icon(if (isReadOnly) Icons.Default.Edit else Icons.Default.Visibility, "Toggle Mode", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = { isSecret = !isSecret; Toast.makeText(context, if(isSecret) "Locked 🔒" else "Unlocked 🔓", Toast.LENGTH_SHORT).show() }) { Icon(if (isSecret) Icons.Default.Lock else Icons.Default.LockOpen, "Lock") }
                    if (!isReadOnly) { IconButton(onClick = { if (title.isNotBlank()) { onSave(title, richTextState.toHtml(), selectedCategory, imagePaths, filePaths, isSecret, reminderTime); navController.popBackStack() } }) { Icon(Icons.Default.Check, "Save") } }
                }
            )
        },
        bottomBar = {
            if (!isReadOnly) {
                BottomAppBar(modifier = Modifier.imePadding(), contentPadding = PaddingValues(0.dp)) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // --- ALIGNMENT (Highlighted when active) ---


                        item {
                            IconButton(
                                onClick = { richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Left)) },
                                colors = if (richTextState.currentParagraphStyle.textAlign == TextAlign.Left) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                            ) { Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, "Left") }
                        }
                        item {
                            IconButton(
                                onClick = { richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Center)) },
                                colors = if (richTextState.currentParagraphStyle.textAlign == TextAlign.Center) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                            ) { Icon(Icons.Default.FormatAlignCenter, "Center") }
                        }
                        item {
                            IconButton(
                                onClick = { richTextState.toggleParagraphStyle(ParagraphStyle(textAlign = TextAlign.Right)) },
                                colors = if (richTextState.currentParagraphStyle.textAlign == TextAlign.Right) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                            ) { Icon(Icons.AutoMirrored.Filled.FormatAlignRight, "Right") }
                        }

                        item { VerticalDivider() }

                        // --- STYLING (Highlighted when active) ---
                        item {
                            IconButton(
                                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                                colors = if (richTextState.currentSpanStyle.fontWeight == FontWeight.Bold) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                            ) { Icon(Icons.Default.FormatBold, "Bold") }
                        }
                        item {
                            IconButton(
                                onClick = { richTextState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                                colors = if (richTextState.currentSpanStyle.fontStyle == FontStyle.Italic) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                            ) { Icon(Icons.Default.FormatItalic, "Italic") }
                        }
                        item {
                            IconButton(
                                onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                                colors = if (richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.Underline) == true) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                            ) { Icon(Icons.Default.FormatUnderlined, "Underline") }
                        }
                        item {
                            IconButton(
                                onClick = { richTextState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) },
                                colors = if (richTextState.currentSpanStyle.textDecoration?.contains(TextDecoration.LineThrough) == true) IconButtonDefaults.filledIconButtonColors() else IconButtonDefaults.iconButtonColors()
                            ) { Icon(Icons.Default.StrikethroughS, "Strike") }
                        }

                        item { VerticalDivider() }

                        // --- COLORS & CODE ---
                        item { IconButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(background = Color(0xFFFFFF00))) }) { Icon(Icons.Default.FormatPaint, "Highlight", tint = Color.Yellow) } }
                        item { IconButton(onClick = { richTextState.toggleSpanStyle(SpanStyle(color = Color(0xFFD32F2F))) }) { Icon(Icons.Default.Palette, "Red Text", tint = Color.Red) } }
                        item {
                            IconButton(
                                onClick = { richTextState.toggleCodeSpan() },
                                colors = IconButtonDefaults.iconButtonColors() // Code span check is complex, keeping simpler
                            ) { Icon(Icons.Default.Code, "Code") }
                        }

                        item { VerticalDivider() }

                        // --- LISTS ---
                        item { IconButton(onClick = { richTextState.toggleUnorderedList() }) { Icon(Icons.Default.FormatListBulleted, "Bullet") } }
                        item { IconButton(onClick = { richTextState.toggleOrderedList() }) { Icon(Icons.Default.FormatListNumbered, "Number") } }
                        item { IconButton(onClick = { val current = richTextState.toHtml(); richTextState.setHtml(current + " &#9898; ") }) { Icon(Icons.Default.Circle, "Task", tint = Color.Gray) } }

                        item { VerticalDivider() }

                        // --- LINK & MEDIA ---
                        item { IconButton(onClick = { showLinkDialog = true }) { Icon(Icons.Default.Link, "Link") } }
                        item { IconButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Default.Image, null) } }
                        item { IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) { Icon(Icons.Default.AttachFile, null) } }

                        item {
                            IconButton(
                                onClick = {
                                    if (isRecording) { audioRecorder.stop(); isRecording = false; currentAudioFile?.let { file -> filePaths += file.absolutePath; richTextState.addLink(" 🎤 Audio ", file.absolutePath); Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() } }
                                    else { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                                },
                                colors = if (isRecording) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error) else IconButtonDefaults.iconButtonColors()
                            ) { Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null, tint = if (isRecording) Color.White else LocalContentColor.current) }
                        }

                        item { IconButton(onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) else datePickerDialog.show() }) { Icon(Icons.Default.Notifications, null, tint = if (reminderTime != null) MaterialTheme.colorScheme.primary else LocalContentColor.current) } }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 20.dp).verticalScroll(rememberScrollState())) {

            // REMINDER
            if (reminderTime != null) {
                val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                InputChip(selected = true, onClick = { datePickerDialog.show() }, label = { Text("Reminder: ${formatter.format(reminderTime!!)}") }, leadingIcon = { Icon(Icons.Default.Alarm, null) }, trailingIcon = { Icon(Icons.Default.Close, "Remove", Modifier.clickable { reminderTime = null }) })
                Spacer(modifier = Modifier.height(8.dp))
            }

            // CATEGORIES
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { category -> FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = category }, label = { Text(category) }, leadingIcon = if (selectedCategory == category) { { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) } } else null) }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // --- COLLAPSIBLE SECTIONS ---

            if (imagePaths.isNotEmpty()) {
                CollapsibleSection(title = "📸 Images", count = imagePaths.size) {
                    LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(imagePaths) { path ->
                            Box(modifier = Modifier.size(120.dp)) {
                                Card(shape = MaterialTheme.shapes.medium) { AsyncImage(model = File(path), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                                if(!isReadOnly) {
                                    IconButton(onClick = { attachmentToDelete = path }, modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(0.5f), CircleShape).size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp)) }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (audioFiles.isNotEmpty()) {
                CollapsibleSection(title = "🎤 Recordings", count = audioFiles.size) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        audioFiles.forEach { path ->
                            InputChip(
                                selected = false,
                                onClick = { if(isReadOnly) com.alley.digitalmemory.openFile(context, path) },
                                label = { Text("Audio Clip") },
                                leadingIcon = { Icon(Icons.Default.PlayCircle, null) },
                                trailingIcon = if(!isReadOnly) { { Icon(Icons.Default.Close, "Remove", Modifier.clickable { attachmentToDelete = path }) } } else null,
                                colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (videoFiles.isNotEmpty()) {
                CollapsibleSection(title = "🎬 Videos", count = videoFiles.size) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        videoFiles.forEach { path -> InputChip(selected = false, onClick = { if(isReadOnly) com.alley.digitalmemory.openFile(context, path) }, label = { Text(getFileNameFromPath(path)) }, leadingIcon = { Icon(Icons.Default.PlayArrow, null) }, trailingIcon = if(!isReadOnly) { { Icon(Icons.Default.Close, "Remove", Modifier.clickable { attachmentToDelete = path }) } } else null, colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (docFiles.isNotEmpty()) {
                CollapsibleSection(title = "📄 Documents", count = docFiles.size) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        docFiles.forEach { path -> InputChip(selected = false, onClick = { if(isReadOnly) com.alley.digitalmemory.openFile(context, path) }, label = { Text(getFileNameFromPath(path)) }, leadingIcon = { Icon(Icons.Default.Description, null) }, trailingIcon = if(!isReadOnly) { { Icon(Icons.Default.Close, "Remove", Modifier.clickable { attachmentToDelete = path }) } } else null) }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // TITLE
            TextField(value = title, onValueChange = { title = it }, placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium) }, modifier = Modifier.fillMaxWidth(), readOnly = isReadOnly, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))

            Spacer(modifier = Modifier.height(8.dp))

            // RICH TEXT
            if (isReadOnly) {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalUriHandler provides object : androidx.compose.ui.platform.UriHandler {
                    override fun openUri(uri: String) { com.alley.digitalmemory.openFile(context, uri) }
                }) {
                    RichText(state = richTextState, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp), modifier = Modifier.fillMaxWidth())
                }
            } else {
                RichTextEditor(
                    state = richTextState,
                    placeholder = { Text("Start typing here...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                    colors = RichTextEditorDefaults.richTextEditorColors(containerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp)
                )
            }
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// HELPERS
@Composable
fun VerticalDivider() { Spacer(modifier = Modifier.width(4.dp).height(24.dp).background(Color.Gray.copy(0.3f))) }

@Composable
fun CollapsibleSection(title: String, count: Int, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(true) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "($count)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Spacer(modifier = Modifier.weight(1f))
            Icon(imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
        }
        AnimatedVisibility(visible = expanded) { content() }
    }
}