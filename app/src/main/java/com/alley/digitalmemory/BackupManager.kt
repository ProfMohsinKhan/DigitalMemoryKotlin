package com.alley.digitalmemory

import android.content.Context
import android.net.Uri
import com.alley.digitalmemory.data.Note
import com.alley.digitalmemory.data.NoteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private val gson = Gson()

    // --- EXPORT (BACKUP) ---
    suspend fun exportBackup(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            val db = NoteDatabase.getDatabase(context)
            // 1. Get all notes (including deleted ones)
            // Hum raw query use kar rahe hain taaki sab kuch aa jaye
            val notes = db.noteDao.getAllNotes().first() // Active notes
            val trash = db.noteDao.getTrashedNotes().first() // Trash notes
            val allNotes = notes + trash

            // 2. Prepare Notes for Backup (Convert Absolute Paths to Relative Filenames)
            // Phone A ka path (/data/user/0/...) Phone B par kaam nahi karega.
            // Isliye hum sirf filename save karenge (e.g., "image.jpg")
            val cleanNotes = allNotes.map { note ->
                note.copy(
                    imagePaths = note.imagePaths.map { File(it).name },
                    filePaths = note.filePaths.map { File(it).name }
                )
            }

            val jsonString = gson.toJson(cleanNotes)

            // 3. Create ZIP
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->

                    // A. Add JSON Data
                    val jsonEntry = ZipEntry("digital_memory_data.json")
                    zipOut.putNextEntry(jsonEntry)
                    zipOut.write(jsonString.toByteArray())
                    zipOut.closeEntry()

                    // B. Add Media Files
                    val allFiles = allNotes.flatMap { it.imagePaths + it.filePaths }
                    allFiles.forEach { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val entry = ZipEntry("files/${file.name}") // Store inside 'files/' folder
                            zipOut.putNextEntry(entry)
                            FileInputStream(file).use { input ->
                                input.copyTo(zipOut)
                            }
                            zipOut.closeEntry()
                        }
                    }
                }
            }
        }
    }

    // --- IMPORT (RESTORE) ---
    suspend fun importBackup(context: Context, uri: Uri) {
        withContext(Dispatchers.IO) {
            val db = NoteDatabase.getDatabase(context)
            var importedNotes: List<Note> = emptyList()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "digital_memory_data.json") {
                            // 1. Read JSON
                            val jsonString = zipIn.bufferedReader().readText()
                            val type = object : TypeToken<List<Note>>() {}.type
                            importedNotes = gson.fromJson(jsonString, type)
                        } else if (entry.name.startsWith("files/")) {
                            // 2. Restore File
                            val fileName = File(entry.name).name
                            val targetFile = File(context.filesDir, fileName)
                            FileOutputStream(targetFile).use { output ->
                                zipIn.copyTo(output)
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            // 3. Fix Paths (Convert Filenames back to Absolute Paths for this phone)
            val fixedNotes = importedNotes.map { note ->
                note.copy(
                    imagePaths = note.imagePaths.map { File(context.filesDir, it).absolutePath },
                    filePaths = note.filePaths.map { File(context.filesDir, it).absolutePath }
                )
            }

            // 4. Update Database
            db.noteDao.clearAllNotes() // Wipe old data (Warning: Dangerous but standard for restore)
            db.noteDao.insertAll(fixedNotes)
        }
    }
}