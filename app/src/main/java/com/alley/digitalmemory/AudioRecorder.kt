package com.alley.digitalmemory

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    // Android version ke hisaab se recorder create karo
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
    }

    fun start(outputFile: File) {
        createRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)

            try {
                prepare()
                start()
                recorder = this
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        recorder?.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                // Kabhi kabhi turant stop karne par crash hota hai, usse ignore karo
            }
            reset()
            release()
        }
        recorder = null
    }
}