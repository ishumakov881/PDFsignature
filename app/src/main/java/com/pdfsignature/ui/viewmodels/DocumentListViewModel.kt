package com.pdfsignature.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsignature.core.repository.PdfDocument
import com.pdfsignature.core.repository.PdfRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

data class DocumentListState(
    val isLoading: Boolean = false,
    val documents: List<PdfDocument> = emptyList(),
    val error: String? = null,
    val searchQuery: String = ""
)

class DocumentListViewModel(
    private val repository: PdfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(DocumentListState())
    val uiState: StateFlow<DocumentListState> = _uiState.asStateFlow()

    // Событие для запуска выбора файла
    private val _importPdfEvent = MutableSharedFlow<Unit>()
    val importPdfEvent = _importPdfEvent.asSharedFlow()

    private val _selectedDocument = MutableStateFlow<PdfDocument?>(null)
    val selectedDocument = _selectedDocument.asStateFlow()

    init {
        loadDocuments()
    }

    fun setSearchQuery(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query) }
            loadDocuments()
        }
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllDocuments()
                    .collect { documents ->
                        val filteredDocuments = if (_uiState.value.searchQuery.isNotEmpty()) {
                            documents.filter { doc ->
                                doc.title.contains(_uiState.value.searchQuery, ignoreCase = true)
                            }
                        } else {
                            documents
                        }
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                documents = filteredDocuments
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
                // Создаем документ с путем к локальной копии
                val document = PdfDocument(
                    id = UUID.randomUUID().toString(),
                    title = fileName,
                    addedDate = System.currentTimeMillis(),
                    path = uri // URI будет обработан в репозитории
                )
                
                // Сохраняем информацию о документе
                repository.saveDocument(document)
                
                // Автоматически выбираем новый документ
                onDocumentSelected(document)
                
                _uiState.update { it.copy(
                    isLoading = false,
                    error = "Документ успешно импортирован"
                ) }
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
            _selectedDocument.emit(document)
        }
    }

    fun deleteDocument(document: PdfDocument) {
        viewModelScope.launch {
            try {
                // Удаляем документ через репозиторий
                repository.deleteDocument0(document)
                // Обновляем список
                loadDocuments()
                _uiState.update { it.copy(error = "Документ удален") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при удалении: ${e.message}") }
            }
        }
    }

    fun setSelectedDocument(document: PdfDocument) {
        _selectedDocument.value = document
    }
} 