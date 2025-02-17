package com.walhalla.pdfsignature.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import com.walhalla.pdfsignature.core.viewer.PdfViewer
import com.walhalla.pdfsignature.ui.components.SignaturePadDialog
import com.walhalla.pdfsignature.ui.viewmodels.CurrentDocumentViewModel

import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun CurrentDocumentScreen(
    viewModel: CurrentDocumentViewModel = koinViewModel(),
    pdfViewer: PdfViewer = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSignaturePad by remember { mutableStateOf(false) }
    var selectedNotation by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteCoordinates by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            uiState.currentDocument == null -> {
                Text(
                    text = "Нет выбранного документа",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                uiState.pdfFile?.let { file ->
                    pdfViewer.DisplayPdf(
                        file = file,
                        onPageClick = { page, x, y, bitmap ->
                            println("DEBUG: Клик по странице $page в точке ($x, $y)")
                            println("DEBUG: Существующая подпись: ${bitmap != null}")
                            
                            // Проверяем, есть ли нотация в этой точке
                            val notation = uiState.notations.find { notation ->
                                notation.page == page && 
                                kotlin.math.abs(notation.x - x) < 5 && // Допуск в 5%
                                kotlin.math.abs(notation.y - y) < 5     // Допуск в 5%
                            }
                            
                            if (notation != null) {
                                // Если есть нотация и подпись - показываем диалог удаления
                                if (bitmap != null) {
                                    deleteCoordinates = Triple(page, x, y)
                                    showDeleteDialog = true
                                } 
                                // Если есть нотация, но нет подписи - показываем диалог подписи
                                else {
                                    selectedNotation = Triple(page, x, y)
                                    showSignaturePad = true
                                }
                            }
                            // Если нет нотации - ничего не делаем
                        },
                        notations = uiState.notations.map { notation ->
                            println("DEBUG: Нотация на странице ${notation.page} в точке (${notation.x}, ${notation.y})")
                            com.walhalla.pdfsignature.core.viewer.PdfNotation(
                                page = notation.page,
                                x = notation.x,
                                y = notation.y,
                                signatureBitmap = notation.signatureBitmap
                            )
                        },
                        currentPage = currentPage,
                        onPageChanged = { page -> currentPage = page }
                    )
                }
            }
        }
    }
    
    if (showSignaturePad) {
        SignaturePadDialog(
            onSignatureComplete = { bitmap ->
                selectedNotation?.let { (page, x, y) ->
                    println("DEBUG: Получена подпись размером: ${bitmap.width}x${bitmap.height}")
                    viewModel.addSignature(bitmap, page, x, y)
                }
                showSignaturePad = false
                selectedNotation = null
            },
            onDismiss = {
                showSignaturePad = false
                selectedNotation = null
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false 
                deleteCoordinates = null
            },
            title = { Text("Удалить подпись?") },
            text = { Text("Вы действительно хотите удалить подпись?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCoordinates?.let { (page, x, y) ->
                            viewModel.removeSignature(page, x, y)
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