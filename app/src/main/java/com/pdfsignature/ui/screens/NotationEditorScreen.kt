package com.pdfsignature.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pdfsignature.core.viewer.PdfNotation
import com.pdfsignature.core.viewer.PdfViewer
import com.pdfsignature.ui.viewmodels.NotationEditorViewModel
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var clickedCoordinates by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var deleteCoordinates by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    var draggedNotation by remember { mutableStateOf<PdfNotation?>(null) }
    
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
                            text = "Нажмите в любом месте документа для добавления области подписи\nПеретащите маркер для изменения его положения\nНажмите на маркер для его удаления",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    
                    // PDF просмотрщик
                    uiState.pdfFile?.let { file ->
                        pdfViewer.DisplayPdf(
                            file = file,
                            onPageClick = { page, x, y, _ ->
                                // Проверяем, есть ли нотация в этой точке
                                val existingNotation = uiState.notations.find { notation ->
                                    notation.page == page && 
                                    kotlin.math.abs(notation.x - x) < 5 && 
                                    kotlin.math.abs(notation.y - y) < 5
                                }
                                
                                if (existingNotation != null) {
                                    // Если кликнули по существующей нотации - показываем диалог удаления
                                    deleteCoordinates = Triple(page, existingNotation.x, existingNotation.y)
                                    showDeleteDialog = true
                                } else {
                                    // Если кликнули в пустое место - показываем диалог добавления
                                    clickedCoordinates = Triple(page, x, y)
                                    showCoordinatesDialog = true
                                }
                            },
                            notations = uiState.notations.map {
                                PdfNotation(
                                    page = it.page,
                                    x = it.x,
                                    y = it.y,
                                    isDraggable = true,
                                    onDragStart = { notation ->
                                        draggedNotation = notation
                                    },
                                    onDragEnd = { notation, newX, newY ->
                                        viewModel.updateNotationPosition(
                                            page = notation.page,
                                            oldX = notation.x,
                                            oldY = notation.y,
                                            newX = newX,
                                            newY = newY
                                        )
                                        draggedNotation = null
                                    }
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

    // Диалог подтверждения удаления нотации
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                deleteCoordinates = null
            },
            title = { Text("Удалить область для подписи?") },
            text = {
                deleteCoordinates?.let { (page, x, y) ->
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
                        deleteCoordinates?.let { (page, x, y) ->
                            viewModel.deleteNotation(page, x, y)
                        }
                        showDeleteDialog = false
                        deleteCoordinates = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        deleteCoordinates = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
} 