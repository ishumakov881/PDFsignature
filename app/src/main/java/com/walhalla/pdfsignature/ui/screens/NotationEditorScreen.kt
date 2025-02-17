package com.walhalla.pdfsignature.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walhalla.pdfsignature.core.viewer.PdfViewer
import com.walhalla.pdfsignature.ui.viewmodels.NotationEditorViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotationEditorScreen(
    viewModel: NotationEditorViewModel = koinViewModel(),
    pdfViewer: PdfViewer = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCoordinatesDialog by remember { mutableStateOf(false) }
    var clickedCoordinates by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.currentDocument == null -> {
                Text(
                    text = "Выберите документ для добавления нотаций",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column {
                    // Заголовок с инструкцией
                    Surface(
                        tonalElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Нажмите в любом месте документа для добавления области подписи",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    // PDF просмотрщик
                    uiState.pdfFile?.let { file ->
                        pdfViewer.DisplayPdf(
                            file = file,
                            onPageClick = { page, x, y, bitmap ->
                                clickedCoordinates = Triple(page, x, y)
                                showCoordinatesDialog = true
                            },
                            notations = uiState.notations.map {
                                com.walhalla.pdfsignature.core.viewer.PdfNotation(
                                    page = it.page,
                                    x = it.x,
                                    y = it.y
                                )
                            },
                            currentPage = currentPage,
                            onPageChanged = { page -> currentPage = page }
                        )
                    }
                }
            }
        }
    }
    
    // Диалог подтверждения добавления нотации
    if (showCoordinatesDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCoordinatesDialog = false
                clickedCoordinates = null
            },
            title = { Text("Добавить область для подписи?") },
            text = {
                clickedCoordinates?.let { (page, x, y) ->
                    Column {
                        Text("Страница: ${page + 1}")
                        Text("Координата X: ${String.format("%.2f", x)}")
                        Text("Координата Y: ${String.format("%.2f", y)}")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clickedCoordinates?.let { (page, x, y) ->
                            viewModel.addNotation(page, x, y)
                        }
                        showCoordinatesDialog = false
                        clickedCoordinates = null
                    }
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showCoordinatesDialog = false
                        clickedCoordinates = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
} 