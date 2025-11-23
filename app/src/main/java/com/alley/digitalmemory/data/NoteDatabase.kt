package com.alley.digitalmemory.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters // <--- Import this

@Database(
    entities = [Note::class],
    version = 1
)
@TypeConverters(Converters::class) // <--- ADD THIS LINE
abstract class NoteDatabase : RoomDatabase() {

    abstract val noteDao: NoteDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        fun getDatabase(context: Context): NoteDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "note_database.db"
                )
                    // .fallbackToDestructiveMigration() // Optional: Agar crash ho to purana DB delete kar de
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}