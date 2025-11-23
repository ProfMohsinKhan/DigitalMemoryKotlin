package com.alley.digitalmemory.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Upsert
    suspend fun upsertNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note) // Ye "Permanent Delete" ke liye use hoga

    // UPDATED: Sirf wo notes lao jo DELETED NAHI hain
    @Query("SELECT * FROM notes_table WHERE isDeleted = 0 ORDER BY dateCreated DESC")
    fun getAllNotes(): Flow<List<Note>>

    // NEW: Sirf DELETED notes lao (Trash Bin ke liye)
    @Query("SELECT * FROM notes_table WHERE isDeleted = 1 ORDER BY dateCreated DESC")
    fun getTrashedNotes(): Flow<List<Note>>

    // UPDATED: Search bhi sirf active notes mein hona chahiye
    @Query("SELECT * FROM notes_table WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY dateCreated DESC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes_table WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    // Purana sab delete karne ke liye
    @Query("DELETE FROM notes_table")
    suspend fun clearAllNotes()

    // Backup se aayi hui list ko insert karne ke liye
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<Note>)
}