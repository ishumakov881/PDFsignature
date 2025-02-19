package com.walhalla.pdfsignature.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
import com.pdfsignature.data.preferences.AppPreferences
import com.pdfsignature.ui.navigation.Screen
import com.pdfsignature.ui.screens.*
import com.walhalla.pdfsignature.ui.theme.PDFsignatureTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val preferences: AppPreferences by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme by preferences.isDarkTheme.collectAsState(initial = false)
            PDFsignatureTheme(darkTheme = isDarkTheme) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("PDF Signature") },
//                actions = {
//                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
//                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
//                    }
//                }
//            )
//        },
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
                                Screen.CurrentDocument -> Icon(
                                    Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = "Current Document"
                                )
                                Screen.DocumentList -> Icon(
                                    Icons.AutoMirrored.Filled.List,
                                    contentDescription = "Document List"
                                )
                                Screen.NotationEditor -> Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editor"
                                )
                                Screen.SignedDocuments -> Icon(
                                    Icons.Default.History,
                                    contentDescription = "History"
                                )
                                else -> {}
                            }
                        },
                        label = {
                            Text(
                                when (screen) {
                                    Screen.CurrentDocument -> "Текущий"
                                    Screen.DocumentList -> "Документы"
                                    Screen.NotationEditor -> "Редактор"
                                    Screen.SignedDocuments -> "История"
                                    else -> ""
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
                CurrentDocumentScreen(onSettingsClick = { navController.navigate(Screen.Settings.route) })
            }
            composable(Screen.DocumentList.route) {
                DocumentListScreen(navController = navController)
            }
            composable(Screen.NotationEditor.route) {
                NotationEditorScreen()
            }
            composable(Screen.SignedDocuments.route) {
                SignedDocumentsScreen(navController = navController)
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
} 