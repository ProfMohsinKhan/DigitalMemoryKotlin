package com.alley.digitalmemory.presentation

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alley.digitalmemory.BackupManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // EXPORT LAUNCHER (Create File)
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            isLoading = true
            scope.launch {
                try {
                    BackupManager.exportBackup(context, it)
                    Toast.makeText(context, "Backup Successful!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // IMPORT LAUNCHER (Open File)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            isLoading = true
            scope.launch {
                try {
                    BackupManager.importBackup(context, it)
                    Toast.makeText(context, "Restore Successful!", Toast.LENGTH_LONG).show()
                    // Restart app logic or Navigate Home usually better
                } catch (e: Exception) {
                    Toast.makeText(context, "Import Failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Processing... Please wait")
            } else {
                // EXPORT CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Backup Data", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Save all your notes, images, and recordings to a ZIP file (Google Drive / Local).")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            val fileName = "DigitalMemory_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.zip"
                            exportLauncher.launch(fileName)
                        }) {
                            Text("Create Backup")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // IMPORT CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudDownload, null, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Restore Data", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Warning: This will replace your current notes with the backup data.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            importLauncher.launch(arrayOf("application/zip"))
                        }) {
                            Text("Select Backup File")
                        }
                    }
                }
            }
        }
    }
}