package com.pdfsignature.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsignature.core.repository.PdfDocument
import com.pdfsignature.core.repository.PdfRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class HistoryPdfViewerState(
    val isLoading: Boolean = false,
    val title: String = "",
    val pdfFile: File? = null,
    val error: String? = null
)

class HistoryPdfViewerViewModel(
    private val repository: PdfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryPdfViewerState())
    val uiState: StateFlow<HistoryPdfViewerState> = _uiState.asStateFlow()

    fun loadDocument(documentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Получаем файл напрямую из директории с подписанными документами
                val file = repository.getSignedDocumentFile(documentId)
                if (file.exists()) {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            title = documentId,
                            pdfFile = file
                        )
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Документ не найден"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }
} 