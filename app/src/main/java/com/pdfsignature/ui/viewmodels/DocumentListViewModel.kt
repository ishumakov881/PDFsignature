package com.pdfsignature.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsignature.core.repository.PdfDocument
import com.pdfsignature.core.repository.PdfRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

data class DocumentListState(
    val isLoading: Boolean = false,
    val documents: List<PdfDocument> = emptyList(),
    val error: String? = null
)

class DocumentListViewModel(
    private val repository: PdfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentListState())
    val uiState: StateFlow<DocumentListState> = _uiState.asStateFlow()

    // Событие для запуска выбора файла
    private val _importPdfEvent = MutableSharedFlow<Unit>()
    val importPdfEvent = _importPdfEvent.asSharedFlow()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllDocuments()
                    .collect { documents ->
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                documents = documents
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

    fun importPdfFromStorage() {
        viewModelScope.launch {
            _importPdfEvent.emit(Unit)
        }
    }

    // Обработка результата выбора файла
    fun handleSelectedPdf(uri: String, fileName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val file = repository.getPdfFromLocal(uri)
                val document = PdfDocument(
                    id = UUID.randomUUID().toString(),
                    title = fileName,
                    addedDate = System.currentTimeMillis(),
                    path = file.path
                )
                repository.saveDocument(document)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Ошибка при импорте файла: ${e.message}"
                    )
                }
            }
        }
    }

    fun onDocumentSelected(document: PdfDocument) {
        viewModelScope.launch {
            try {
                // Обновляем время последнего просмотра в базе данных
                repository.getPdfFromAssets("sample.pdf") // Загружаем файл для кэширования
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message)
                }
            }
        }
    }
} 