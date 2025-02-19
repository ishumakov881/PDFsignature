package com.walhalla.pdfsignature.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import android.content.Intent
import androidx.navigation.NavController
import com.pdfsignature.core.viewer.PdfViewer
import com.pdfsignature.ui.viewmodels.HistoryPdfViewerViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPdfViewerScreen(
    documentId: String,
    navController: NavController,
    viewModel: HistoryPdfViewerViewModel = koinViewModel(),
    pdfViewer: PdfViewer = koinInject()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var currentPage by remember { mutableIntStateOf(0) }

    // Загружаем документ при первом запуске
    LaunchedEffect(documentId) {
        viewModel.loadDocument(documentId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
            },
            title = { Text(uiState.title.ifEmpty { "Просмотр документа" }) },
            actions = {
                IconButton(
                    onClick = {
                        uiState.pdfFile?.let { file ->
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
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Поделиться")
                }
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Ошибка загрузки документа",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.pdfFile == null -> {
                    Text(
                        text = "Документ не найден",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    uiState.pdfFile?.let { file ->
                        pdfViewer.DisplayPdf(
                            file = file,
                            onPageClick = null, // Отключаем клики
                            notations = emptyList(), // Не показываем области для подписи
                            currentPage = currentPage,
                            onPageChanged = { page -> currentPage = page }
                        )
                    }
                }
            }
        }
    }
}