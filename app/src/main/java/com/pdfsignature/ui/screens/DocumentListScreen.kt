package com.pdfsignature.ui.screens

import android.annotation.SuppressLint
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.pdfsignature.ui.navigation.Screen
import com.pdfsignature.ui.viewmodels.DocumentListViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("Range")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentListScreen(
    viewModel: DocumentListViewModel = koinViewModel(),
    navController: NavController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showError by remember { mutableStateOf<String?>(null) }
    
    // Launcher для выбора PDF файла
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            try {
                // Получаем имя файла
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                        // Копируем URI в строку, так как URI может стать недействительным
                        val uriString = uri.toString()
                        // Обрабатываем выбранный файл
                        viewModel.handleSelectedPdf(uriString, displayName)
                    }
                }
            } catch (e: Exception) {
                showError = "Ошибка при выборе файла: ${e.message}"
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
            TopAppBar(
                title = { Text("Документы") },
                actions = {
                    IconButton(onClick = { viewModel.importPdfFromStorage() }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Импорт PDF"
                        )
                    }
                }
            )
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
                            text = "Нет доступных документов",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите на кнопку импорта чтобы добавить PDF",
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
                                    .padding(8.dp)
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
                                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
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
                                                viewModel.onDocumentSelected(document)
                                                navController.navigate(Screen.CurrentDocument.route)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.RemoveRedEye,
                                                contentDescription = "Просмотр",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        IconButton(
                                            onClick = { 
                                                viewModel.onDocumentSelected(document)
                                                navController.navigate(Screen.NotationEditor.route)
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Редактировать",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Показываем ошибку если она есть
            if (showError != null) {
                AlertDialog(
                    onDismissRequest = { showError = null },
                    title = { Text("Ошибка") },
                    text = { Text(showError!!) },
                    confirmButton = {
                        TextButton(onClick = { showError = null }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
} 