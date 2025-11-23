package com.alley.digitalmemory.presentation

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.alley.digitalmemory.AudioRecorder
import com.alley.digitalmemory.copyFileIfSizeValid
import com.alley.digitalmemory.copyUriToInternalStorage
import com.alley.digitalmemory.getFileNameFromPath
import com.alley.digitalmemory.isAudioFile
import com.alley.digitalmemory.isVideoFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Stack
import com.alley.digitalmemory.EditorStyledTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteScreen(
    navController: NavController,
    noteId: Int,
    viewModel: NoteViewModel,
    onSave: (String, String, String, List<String>, List<String>, Boolean, Long?) -> Unit
) {
    val context = LocalContext.current

    // --- STATE VARIABLES ---
    var title by remember { mutableStateOf("") }

    // Content State (TextFieldValue tracks cursor position)
    var content by remember { mutableStateOf(TextFieldValue("")) }

    var selectedCategory by remember { mutableStateOf("Personal") }
    var imagePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var filePaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var isSecret by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }

    // --- UNDO / REDO STACKS ---
    val undoStack = remember { Stack<TextFieldValue>() }
    val redoStack = remember { Stack<TextFieldValue>() }

    // Function to handle text changes
    fun onContentChange(newValue: TextFieldValue) {
        if (newValue.text != content.text) {
            undoStack.push(content) // Save history
            redoStack.clear()
        }
        content = newValue
    }

    // Function to apply Markdown Formatting
    // Corrected Logic for Backwards Selection
    fun applyFormat(startSymbol: String, endSymbol: String = startSymbol) {
        val text = content.text
        val selection = content.selection

        undoStack.push(content) // Save History

        // FIX: Always take the smaller index as start, larger as end
        val start = selection.min
        val end = selection.max

        if (selection.collapsed) {
            // Case 1: Cursor par insert karo
            val newText = text.substring(0, start) + startSymbol + endSymbol + text.substring(start)
            val newCursorPos = start + startSymbol.length
            content = TextFieldValue(text = newText, selection = TextRange(newCursorPos))
        } else {
            // Case 2: Selection ko wrap karo
            val before = text.substring(0, start)
            val selectedText = text.substring(start, end)
            val after = text.substring(end)

            val newText = before + startSymbol + selectedText + endSymbol + after

            // Selection wahi rakho jahan text tha
            val newSelection = TextRange(start + startSymbol.length, end + startSymbol.length)
            content = TextFieldValue(text = newText, selection = newSelection)
        }
    }

    // Voice Recording State
    var isRecording by remember { mutableStateOf(false) }
    val audioRecorder = remember { AudioRecorder(context) }
    var currentAudioFile by remember { mutableStateOf<File?>(null) }

    val categories = listOf("Personal", "Work", "Ideas")

    // Separate Files
    val audioFiles = filePaths.filter { com.alley.digitalmemory.isAudioFile(it) }
    val videoFiles = filePaths.filter { com.alley.digitalmemory.isVideoFile(it) }
    val docFiles = filePaths.filter { !com.alley.digitalmemory.isAudioFile(it) && !com.alley.digitalmemory.isVideoFile(it) }

    // --- PICKERS ---
    val calendar = Calendar.getInstance()
    val timePickerDialog = TimePickerDialog(context, { _, h, m -> calendar.set(Calendar.HOUR_OF_DAY, h); calendar.set(Calendar.MINUTE, m); calendar.set(Calendar.SECOND, 0); reminderTime = calendar.timeInMillis }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d -> calendar.set(Calendar.YEAR, y); calendar.set(Calendar.MONTH, m); calendar.set(Calendar.DAY_OF_MONTH, d); timePickerDialog.show() }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) datePickerDialog.show() }
    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if(it) { isRecording = true; audioRecorder.start(File(context.filesDir, "voice_${System.currentTimeMillis()}.m4a").also { f -> currentAudioFile = f }) } }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { imagePaths += it.mapNotNull { uri -> copyUriToInternalStorage(context, uri) } }
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { it.forEach { uri -> copyFileIfSizeValid(context, uri)?.let { path -> filePaths += path } } }

    // Load Data on Edit
    LaunchedEffect(key1 = true) {
        if (noteId != -1) {
            val note = viewModel.getNoteById(noteId)
            if (note != null) {
                title = note.title
                // Important: Set TextFieldValue with cursor at end
                content = TextFieldValue(text = note.content, selection = TextRange(note.content.length))
                selectedCategory = note.category; imagePaths = note.imagePaths; filePaths = note.filePaths; isSecret = note.isSecret; reminderTime = note.reminder
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isRecording) "Recording... 🔴" else "Edit Idea") },
                colors = if (isRecording) TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.errorContainer) else TopAppBarDefaults.topAppBarColors(),
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    IconButton(onClick = { isSecret = !isSecret; Toast.makeText(context, if(isSecret) "Locked 🔒" else "Unlocked 🔓", Toast.LENGTH_SHORT).show() }) { Icon(if (isSecret) Icons.Default.Lock else Icons.Default.LockOpen, "Lock") }
                    IconButton(onClick = { if (title.isNotBlank()) { onSave(title, content.text, selectedCategory, imagePaths, filePaths, isSecret, reminderTime); navController.popBackStack() } }) { Icon(Icons.Default.Check, "Save") }
                }
            )
        },
        bottomBar = {
            // SCROLLABLE TOOLBAR
            BottomAppBar(
                modifier = Modifier.imePadding(), // Move up with keyboard
                contentPadding = PaddingValues(0.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. UNDO
                    item {
                        IconButton(onClick = {
                            if (undoStack.isNotEmpty()) {
                                redoStack.push(content)
                                content = undoStack.pop()
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.Undo, "Undo") }
                    }

                    // 2. REDO
                    item {
                        IconButton(onClick = {
                            if (redoStack.isNotEmpty()) {
                                undoStack.push(content)
                                content = redoStack.pop()
                            }
                        }) { Icon(Icons.AutoMirrored.Filled.Redo, "Redo") }
                    }

                    item { Spacer(modifier = Modifier.width(8.dp).height(24.dp).background(Color.Gray.copy(0.3f))) }

                    // 3. FORMATTING
                    item { IconButton(onClick = { applyFormat("**") }) { Icon(Icons.Default.FormatBold, "Bold") } }
                    item { IconButton(onClick = { applyFormat("_") }) { Icon(Icons.Default.FormatItalic, "Italic") } }
                    item { IconButton(onClick = { applyFormat("~~") }) { Icon(Icons.Default.StrikethroughS, "Cross") } }

                    // 4. CHECKBOX
                    item {
                        IconButton(onClick = {
                            val text = content.text
                            val prefix = if (text.isNotEmpty() && !text.endsWith("\n")) "\n" else ""
                            val newText = text + prefix + "[ ] "
                            undoStack.push(content)
                            content = TextFieldValue(newText, TextRange(newText.length))
                        }) { Icon(Icons.Default.CheckBox, "Checkbox") }
                    }

                    item { Spacer(modifier = Modifier.width(8.dp).height(24.dp).background(Color.Gray.copy(0.3f))) }

                    // 5. ATTACHMENTS
                    item { IconButton(onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) { Icon(Icons.Default.Image, null) } }
                    item { IconButton(onClick = { filePickerLauncher.launch(arrayOf("*/*")) }) { Icon(Icons.Default.AttachFile, null) } }

                    // 6. MIC
                    item {
                        IconButton(
                            onClick = {
                                if (isRecording) {
                                    audioRecorder.stop(); isRecording = false
                                    currentAudioFile?.let { filePaths += it.absolutePath; Toast.makeText(context, "Saved!", Toast.LENGTH_SHORT).show() }
                                } else { micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                            },
                            colors = if (isRecording) IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.error) else IconButtonDefaults.iconButtonColors()
                        ) { Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null, tint = if (isRecording) Color.White else LocalContentColor.current) }
                    }

                    // 7. REMINDER
                    item {
                        IconButton(onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            else datePickerDialog.show()
                        }) { Icon(Icons.Default.Notifications, null, tint = if (reminderTime != null) MaterialTheme.colorScheme.primary else LocalContentColor.current) }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState())) {

            // REMINDER CHIP
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

            // SECTION 1: IMAGES
            if (imagePaths.isNotEmpty()) {
                Text("📸 Images", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(imagePaths) { path -> Box(modifier = Modifier.size(120.dp)) { Card(shape = MaterialTheme.shapes.medium) { AsyncImage(model = File(path), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }; IconButton(onClick = { imagePaths -= path }, modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(0.5f), CircleShape).size(24.dp)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.padding(4.dp)) } } } }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECTION 2: AUDIO
            if (audioFiles.isNotEmpty()) {
                Text("🎤 Recordings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                audioFiles.forEach { path -> InputChip(selected = false, onClick = { com.alley.digitalmemory.openFile(context, path) }, label = { Text("Audio Clip") }, leadingIcon = { Icon(Icons.Default.PlayCircle, null) }, trailingIcon = { Icon(Icons.Default.Close, "Remove", Modifier.clickable { filePaths -= path }) }, colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECTION 3: VIDEO
            if (videoFiles.isNotEmpty()) {
                Text("🎬 Videos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                videoFiles.forEach { path -> InputChip(selected = false, onClick = { com.alley.digitalmemory.openFile(context, path) }, label = { Text(getFileNameFromPath(path)) }, leadingIcon = { Icon(Icons.Default.PlayArrow, null) }, trailingIcon = { Icon(Icons.Default.Close, "Remove", Modifier.clickable { filePaths -= path }) }, colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECTION 4: DOCS
            if (docFiles.isNotEmpty()) {
                Text("📄 Documents", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                docFiles.forEach { path -> InputChip(selected = false, onClick = { com.alley.digitalmemory.openFile(context, path) }, label = { Text(getFileNameFromPath(path)) }, leadingIcon = { Icon(Icons.Default.Description, null) }, trailingIcon = { Icon(Icons.Default.Close, "Remove", Modifier.clickable { filePaths -= path }) }) }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // TITLE & CONTENT
            TextField(value = title, onValueChange = { title = it }, placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent))

            TextField(
                value = content,
                onValueChange = { onContentChange(it) },
                placeholder = { Text("Type your thoughts here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp),

                // --- NEW LINE ADDED HERE ---
                visualTransformation = EditorStyledTransformation(),
                // ---------------------------

                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
        }
    }
}