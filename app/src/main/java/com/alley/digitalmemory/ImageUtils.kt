package com.alley.digitalmemory

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

fun copyUriToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        // 1. Open the image from the Gallery
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

        // 2. Create a unique file name (using timestamp)
        val fileName = "note_image_${System.currentTimeMillis()}.jpg"

        // 3. Create the file in our App's private storage
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        // 4. Copy the data
        inputStream?.copyTo(outputStream)

        // 5. Close streams to save memory
        inputStream?.close()
        outputStream.close()

        // 6. Return the path of the new file
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}