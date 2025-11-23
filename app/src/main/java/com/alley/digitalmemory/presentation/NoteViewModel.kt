package com.alley.digitalmemory.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.alley.digitalmemory.data.Note
import com.alley.digitalmemory.data.NoteDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(private val dao: NoteDao) : ViewModel() {

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    // 1. ACTIVE NOTES (Main Screen)
    @OptIn(ExperimentalCoroutinesApi::class)
    val state = _searchText.flatMapLatest { query ->
        if (query.isBlank()) {
            dao.getAllNotes()
        } else {
            dao.searchNotes(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    // 2. TRASHED NOTES (Recycle Bin) - Always full list, no search needed for now
    val trashState = dao.getTrashedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    suspend fun getNoteById(id: Int): Note? {
        return dao.getNoteById(id)
    }

    fun onEvent(event: NoteEvent) {
        when (event) {
            is NoteEvent.DeleteNote -> { // PERMANENT DELETE
                viewModelScope.launch { dao.deleteNote(event.note) }
            }
            is NoteEvent.SaveNote -> { // SAVE / UPDATE
                viewModelScope.launch { dao.upsertNote(event.note) }
            }
            is NoteEvent.OnSearchQueryChange -> {
                _searchText.value = event.query
            }
            // NEW EVENTS
            is NoteEvent.MoveToTrash -> {
                viewModelScope.launch {
                    // isDeleted = true karke update karo
                    dao.upsertNote(event.note.copy(isDeleted = true, reminder = null)) // Reminder bhi cancel karo
                }
            }
            is NoteEvent.RestoreNote -> {
                viewModelScope.launch {
                    // isDeleted = false karke update karo
                    dao.upsertNote(event.note.copy(isDeleted = false))
                }
            }
        }
    }
}

// UPDATE EVENTS INTERFACE
sealed interface NoteEvent {
    data class DeleteNote(val note: Note) : NoteEvent // Permanent
    data class SaveNote(val note: Note) : NoteEvent
    data class OnSearchQueryChange(val query: String) : NoteEvent

    // NEW
    data class MoveToTrash(val note: Note) : NoteEvent // Soft Delete
    data class RestoreNote(val note: Note) : NoteEvent
}

class NoteViewModelFactory(private val dao: NoteDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}