package com.pdfsignature.ui.screens

import android.annotation.SuppressLint
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material.DismissDirection

import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController


import com.pdfsignature.ui.navigation.Screen
import com.pdfsignature.ui.viewmodels.DocumentListViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.ExperimentalMaterial3Api
import com.walhalla.pdfsignature.R

@SuppressLint("Range")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DocumentListScreen(
    viewModel: DocumentListViewModel = koinViewModel(),
    navController: NavController
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showError by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Launcher для выбора PDF файла
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                // Получаем имя файла
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayName =
                            cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        // Копируем URI в строку, так как URI может стать недействительным
                        val uriString = uri.toString()
                        // Обрабатываем выбранный файл
                        viewModel.handleSelectedPdf(uriString, displayName)
                    }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: ""
                showError = "error"
            }
        }
    }

    // Отслеживаем событие для запуска выбора файла
    LaunchedEffect(Unit) {
        viewModel.importPdfEvent.collect {
            pdfPickerLauncher.launch(arrayOf("application/pdf"))
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.documents)) },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }

                        IconButton(onClick = { viewModel.importPdfFromStorage() }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = stringResource(R.string.import_pdf)
                            )
                        }
                    }
                )
                // Поле поиска
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text(stringResource(R.string.search_documents)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_documents)) },
                    singleLine = true
                )
            }
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
                            text = if (uiState.searchQuery.isEmpty())
                                stringResource(R.string.no_documents)
                            else
                                stringResource(R.string.nothing_found),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (uiState.searchQuery.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.click_import),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn {
                        items(
                            items = uiState.documents,
                            key = { it.id }
                        ) { document ->
                            val dismissState = rememberDismissState(
                                confirmStateChange = { dismissValue ->
                                    if (dismissValue == DismissValue.DismissedToEnd) {
                                        viewModel.deleteDocument(document)
                                        true
                                    } else true
                                }
                            )

                            SwipeToDismiss(
                                state = dismissState,
                                background = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 8.dp)
                                            //.background(MaterialTheme.colorScheme.errorContainer)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            modifier = Modifier
                                                .align(Alignment.CenterStart)
                                                .padding(start = 16.dp),
                                            //tint = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                },
                                dismissContent = {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = 4.dp
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth()
                                        ) {
                                            Text(
                                                text = document.title,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = SimpleDateFormat(
                                                    "dd/MM/yyyy HH:mm",
                                                    Locale.getDefault()
                                                )
                                                    .format(Date(document.addedDate)),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        viewModel.setSelectedDocument(document)
                                                        navController.navigate(
                                                            Screen.DocumentViewer.createRoute(
                                                                document.id
                                                            )
                                                        )
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.RemoveRedEye,
                                                        contentDescription = stringResource(R.string.view),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                IconButton(
                                                    onClick = {
                                                        viewModel.setSelectedDocument(document)
                                                        navController.navigate(
                                                            Screen.DocumentEditor.createRoute(
                                                                document.id
                                                            )
                                                        )
                                                    }
                                                ) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = stringResource(R.string.edit),
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                directions = setOf(DismissDirection.StartToEnd)
                            )
                        }
                    }
                }
            }

            // Показываем ошибку если она есть
            if (showError != null) {
                AlertDialog(
                    onDismissRequest = { showError = null },
                    title = { Text(stringResource(R.string.error)) },
                    text = { Text(stringResource(R.string.file_selection_error, errorMessage)) },
                    confirmButton = {
                        TextButton(onClick = { showError = null }) {
                            Text(stringResource(R.string.ok))
                        }
                    }
                )
            }
        }
    }
} 