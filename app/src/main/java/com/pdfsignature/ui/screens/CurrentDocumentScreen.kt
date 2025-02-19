package com.pdfsignature.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.pdfsignature.core.repository.SignatureNotation
import com.pdfsignature.core.viewer.PdfNotation

import com.pdfsignature.core.viewer.PdfViewer
import com.pdfsignature.ui.components.SignaturePadDialog
import com.pdfsignature.ui.viewmodels.CurrentDocumentViewModel

import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentDocumentScreen(
    viewModel: CurrentDocumentViewModel = koinViewModel(),
    pdfViewer: PdfViewer = koinInject(),
    onSettingsClick: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showSignaturePad by remember { mutableStateOf(false) }
    var selectedNotationTriple by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteCoordinates by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }

    var currentNotation: SignatureNotation? by remember { mutableStateOf(null) }


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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Текущий документ") },
                actions = {

                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }


                    // Кнопка сохранения PDF с подписями
                    IconButton(
                        onClick = { viewModel.savePdfWithSignatures() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Сохранить PDF с подписями"
                        )
                    }
                    // Кнопка шаринга
                    IconButton(
                        onClick = { viewModel.sharePdfWithSignatures() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться PDF"
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

                                        println("Selected Notation: ${notation}")
                                        currentNotation = notation


                                        selectedNotationTriple = Triple(page, x, y)
                                        showSignaturePad = true
                                    }
                                }
                                // Если нет нотации - ничего не делаем
                            },
                            notations = uiState.notations.map { notation ->
                                println("DEBUG: Нотация на странице ${notation.page} в точке (${notation.x}, ${notation.y})")
                                PdfNotation(
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
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onSignatureComplete = { bitmap ->
                selectedNotationTriple?.let { (page, x, y) ->
                    println("DEBUG: Получена подпись размером: ${bitmap.width}x${bitmap.height}, notation: ${currentNotation}")
                    viewModel.addSignature0(bitmap, page, x, y)
                }
                showSignaturePad = false
                selectedNotationTriple = null
            },
            onDismiss = {
                showSignaturePad = false
                selectedNotationTriple = null
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