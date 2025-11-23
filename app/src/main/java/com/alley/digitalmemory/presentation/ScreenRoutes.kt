package com.alley.digitalmemory.presentation

sealed class ScreenRoutes(val route: String) {
    object NoteList : ScreenRoutes("note_list")
    object AddNote : ScreenRoutes("add_note?noteId={noteId}") {
        fun passId(id: Int): String {
            return "add_note?noteId=$id"
        }
    }
    // NEW ROUTE
    object TrashList : ScreenRoutes("trash_list")

    object Backup : ScreenRoutes("backup_screen") // Add this line
}