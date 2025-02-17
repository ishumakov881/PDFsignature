package com.walhalla.pdfsignature.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.walhalla.pdfsignature.ui.navigation.Screen
import com.walhalla.pdfsignature.ui.screens.CurrentDocumentScreen
import com.walhalla.pdfsignature.ui.screens.DocumentListScreen
import com.walhalla.pdfsignature.ui.screens.NotationEditorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = { 
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            when (screen) {
                                Screen.CurrentDocument -> Icon(Icons.Default.InsertDriveFile, contentDescription = "Current Document")
                                Screen.DocumentList -> Icon(Icons.Default.List, contentDescription = "Document List")
                                Screen.NotationEditor -> Icon(Icons.Default.Edit, contentDescription = "Editor")
                            }
                        },
                        label = {
                            Text(
                                when (screen) {
                                    Screen.CurrentDocument -> "Текущий"
                                    Screen.DocumentList -> "Документы"
                                    Screen.NotationEditor -> "Редактор"
                                }
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.CurrentDocument.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.CurrentDocument.route) {
                CurrentDocumentScreen()
            }
            composable(Screen.DocumentList.route) {
                DocumentListScreen(navController = navController)
            }
            composable(Screen.NotationEditor.route) {
                NotationEditorScreen()
            }
        }
    }
} 