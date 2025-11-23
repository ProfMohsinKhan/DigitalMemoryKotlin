package com.alley.digitalmemory

import android.app.Application
import com.alley.digitalmemory.data.NoteDatabase

class DigitalMemoryApp : Application() {

    // "by lazy" means: Don't create the database immediately when the app starts.
    // Create it only when we actually need it (First time we ask for data).
    // This speeds up the App Launch time.
    val database by lazy {
        NoteDatabase.getDatabase(this)
    }
}