package com.alley.digitalmemory

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import android.content.Intent
import androidx.core.content.FileProvider
import android.webkit.MimeTypeMap
import android.widget.Toast

// Helper class to hold file info
data class FileInfo(val name: String, val path: String)

// 1. Check Size & Copy
fun copyFileIfSizeValid(context: Context, uri: Uri, maxMb: Int = 20): String? {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            // Check Size
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            val size = it.getLong(sizeIndex)
            val sizeInMb = size / (1024 * 1024)

            if (sizeInMb > maxMb) {
                return null // File too big!
            }

            // Get Name
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val fileName = it.getString(nameIndex)

            // Copy File
            val file = File(context.filesDir, fileName)
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            return file.absolutePath
        }
    }
    return null
}

// 2. Get File Name from Path (For Display)
fun getFileNameFromPath(path: String): String {
    return File(path).name
}


fun openFile(context: Context, filePath: String) {
    val file = File(filePath)
    if (!file.exists()) {
        Toast.makeText(context, "File not found!", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        // 1. Get Secure URI using FileProvider
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider", // Must match Manifest authority
            file
        )

        // 2. Detect File Type (MimeType) e.g., "application/pdf"
        val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"

        // 3. Create Intent to View
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Give temporary permission
        }

        // 4. Launch App
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
    }
}

// ... existing imports ...

// Helper to check if file is Audio
fun isAudioFile(path: String): Boolean {
    val audioExtensions = listOf("m4a", "mp3", "wav", "aac", "ogg")
    val extension = File(path).extension.lowercase()
    return audioExtensions.contains(extension)
}

// Helper to check if file is Image (Optional, useful if needed later)
fun isImageFile(path: String): Boolean {
    val imageExtensions = listOf("jpg", "jpeg", "png", "webp", "gif")
    val extension = File(path).extension.lowercase()
    return imageExtensions.contains(extension)
}

// Helper to check if file is Video
fun isVideoFile(path: String): Boolean {
    val videoExtensions = listOf("mp4", "mkv", "webm", "3gp", "avi", "mov")
    val extension = File(path).extension.lowercase()
    return videoExtensions.contains(extension)
}