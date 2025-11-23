package com.alley.digitalmemory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes_table")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val dateCreated: Long = System.currentTimeMillis(),
    val category: String = "Uncategorized",
    val imagePaths: List<String> = emptyList(),
    val filePaths: List<String> = emptyList(),
    val isSecret: Boolean = false,
    val reminder: Long? = null,

    // NEW: Soft Delete Flag
    val isDeleted: Boolean = false
)