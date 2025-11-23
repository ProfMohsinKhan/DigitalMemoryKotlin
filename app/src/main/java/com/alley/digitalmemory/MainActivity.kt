package com.alley.digitalmemory

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity // Important for Biometric
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alley.digitalmemory.data.Note
import com.alley.digitalmemory.presentation.AddNoteScreen
import com.alley.digitalmemory.presentation.BackupScreen
import com.alley.digitalmemory.presentation.NoteEvent
import com.alley.digitalmemory.presentation.NoteScreen
import com.alley.digitalmemory.presentation.NoteViewModel
import com.alley.digitalmemory.presentation.NoteViewModelFactory
import com.alley.digitalmemory.presentation.ScreenRoutes
import com.alley.digitalmemory.ui.theme.DigitalMemoryTheme
import com.alley.digitalmemory.presentation.TrashScreen

class MainActivity : FragmentActivity() { // FragmentActivity zaroori hai

    private val database by lazy {
        (application as DigitalMemoryApp).database
    }

    private val viewModel by viewModels<NoteViewModel>(
        factoryProducer = {
            NoteViewModelFactory(database.noteDao)
        }
    )

    // FIX IS HERE: Use "by lazy"
    private val alarmScheduler by lazy {
        AlarmScheduler(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DigitalMemoryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val state by viewModel.state.collectAsState()
                    val searchText by viewModel.searchText.collectAsState()

                    NavHost(
                        navController = navController,
                        startDestination = ScreenRoutes.NoteList.route
                    ) {

                        // SCREEN 1: LIST
                        composable(route = ScreenRoutes.NoteList.route) {
                            NoteScreen(
                                state = state,
                                searchText = searchText,
                                onEvent = viewModel::onEvent,
                                onAddNoteClick = {
                                    navController.navigate(ScreenRoutes.AddNote.route)
                                },
                                onNoteClick = { note ->
                                    navController.navigate(ScreenRoutes.AddNote.passId(note.id))
                                },
                                // NEW: GO TO TRASH
                                onTrashClick = {
                                    navController.navigate(ScreenRoutes.TrashList.route)
                                },
                                onBackupClick = {
                                    navController.navigate(ScreenRoutes.Backup.route)
                                }
                            )
                        }
                        // NEW SCREEN: TRASH LIST
                        composable(route = ScreenRoutes.TrashList.route) {
                            // Get Trash Data from ViewModel
                            val trashState by viewModel.trashState.collectAsState()

                            TrashScreen(
                                navController = navController,
                                state = trashState,
                                onEvent = viewModel::onEvent
                            )
                        }
                        composable(route = ScreenRoutes.Backup.route) {
                            BackupScreen(navController = navController)
                        }

                        // SCREEN 2: ADD/EDIT
                        composable(
                            route = ScreenRoutes.AddNote.route,
                            arguments = listOf(
                                navArgument("noteId") {
                                    type = NavType.IntType
                                    defaultValue = -1
                                }
                            )
                        ) { backStackEntry ->
                            val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1

                            AddNoteScreen(
                                navController = navController,
                                noteId = noteId,
                                viewModel = viewModel,
                                onSave = { title, content, category, imagePaths, filePaths, isSecret, reminderTime ->

                                    val note = Note(
                                        id = if (noteId != -1) noteId else 0,
                                        title = title,
                                        content = content,
                                        category = category,
                                        imagePaths = imagePaths,
                                        filePaths = filePaths,
                                        isSecret = isSecret,
                                        reminder = reminderTime
                                    )

                                    // 1. Save Note
                                    viewModel.onEvent(NoteEvent.SaveNote(note))

                                    // 2. Set/Cancel Alarm
                                    if (reminderTime != null) {
                                        alarmScheduler.schedule(note)
                                    } else {
                                        alarmScheduler.cancel(note)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}