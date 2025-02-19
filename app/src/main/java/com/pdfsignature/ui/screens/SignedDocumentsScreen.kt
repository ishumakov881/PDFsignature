package com.pdfsignature.ui.screens

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

import com.pdfsignature.ui.navigation.Screen
import com.pdfsignature.ui.viewmodels.SignedDocumentsViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.core.content.FileProvider
import com.pdfsignature.ui.viewmodels.SortType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignedDocumentsScreen(
    viewModel: SignedDocumentsViewModel = koinViewModel(),
    navController: NavController
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSortMenu by remember { mutableStateOf(false) }
    var showItemMenu by remember { mutableStateOf<String?>(null) }

    // Обработка события шаринга
    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться PDF"))
        }
    }

    // Показываем Snackbar при появлении ошибки
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Подписанные документы") },
                actions = {
                    // Кнопка сортировки
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, "Сортировка")
                    }
                    // Меню сортировки
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Новые сверху") },
                            onClick = {
                                viewModel.setSortType(SortType.DATE_DESC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Старые сверху") },
                            onClick = {
                                viewModel.setSortType(SortType.DATE_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("По имени (А-Я)") },
                            onClick = {
                                viewModel.setSortType(SortType.NAME_ASC)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("По имени (Я-А)") },
                            onClick = {
                                viewModel.setSortType(SortType.NAME_DESC)
                                showSortMenu = false
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.documents.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Нет подписанных документов",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Подпишите документ и нажмите кнопку сохранения",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn {
                        items(uiState.documents) { document ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                onClick = {
                                    navController.navigate(Screen.HistoryViewer.createRoute(document.id))
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = document.title,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = SimpleDateFormat(
                                                "dd/MM/yyyy HH:mm",
                                                Locale.getDefault()
                                            ).format(Date(document.addedDate)),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    
                                    // Меню документа
                                    Box {
                                        IconButton(onClick = { showItemMenu = document.id }) {
                                            Icon(Icons.Default.MoreVert, "Опции")
                                        }
                                        DropdownMenu(
                                            expanded = showItemMenu == document.id,
                                            onDismissRequest = { showItemMenu = null }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Поделиться") },
                                                leadingIcon = { 
                                                    Icon(Icons.Default.Share, "Поделиться")
                                                },
                                                onClick = {
                                                    viewModel.shareDocument(document.id)
                                                    showItemMenu = null
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Удалить") },
                                                leadingIcon = { 
                                                    Icon(
                                                        Icons.Default.Delete,
                                                        "Удалить",
                                                        //tint = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.deleteDocument(document.id)
                                                    showItemMenu = null
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 