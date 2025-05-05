package com.example.note

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.note.ui.screens.AddEditNoteScreen
import com.example.note.ui.screens.NoteListScreen
import com.example.note.ui.theme.NoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteAppNavigation()
                }
            }
        }
    }
}

@Composable
fun NoteAppNavigation() {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = "noteList") {
        composable("noteList") {
            NoteListScreen(
                onNoteClick = { noteId ->
                    navController.navigate("noteDetail/$noteId")
                },
                onAddNoteClick = {
                    navController.navigate("addNote")
                }
            )
        }
        composable("addNote") {
            AddEditNoteScreen(
                noteId = null,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "noteDetail/{noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.IntType })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getInt("noteId")
            if (noteId != null) {
                AddEditNoteScreen(
                    noteId = noteId,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                navController.popBackStack()
            }
        }
    }
}