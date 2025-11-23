package com.alley.digitalmemory

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.alley.digitalmemory.data.Note

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm") // Permission added in Manifest
    fun schedule(note: Note) {
        if (note.reminder == null) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("noteId", note.id)
            putExtra("title", note.title)
            putExtra("message", note.content.take(50)) // Show start of content
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.id, // Unique ID per note
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set Exact Alarm
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                note.reminder,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancel(note: Note) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            note.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}