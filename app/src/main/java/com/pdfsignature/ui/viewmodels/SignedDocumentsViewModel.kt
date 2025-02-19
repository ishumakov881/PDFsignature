package com.pdfsignature.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsignature.core.repository.PdfDocument
import com.pdfsignature.core.repository.PdfRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

enum class SortType {
    DATE_DESC, // По дате (новые сверху)
    DATE_ASC,  // По дате (старые сверху)
    NAME_ASC,  // По имени (А-Я)
    NAME_DESC  // По имени (Я-А)
}

data class SignedDocumentsState(
    val isLoading: Boolean = false,
    val documents: List<PdfDocument> = emptyList(),
    val sortType: SortType = SortType.DATE_DESC,
    val error: String? = null
)

class SignedDocumentsViewModel(
    private val repository: PdfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignedDocumentsState())
    val uiState: StateFlow<SignedDocumentsState> = _uiState.asStateFlow()

    init {
        loadDocuments()
    }

    private fun loadDocuments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getSignedDocuments()
                    .collect { documents ->
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                documents = sortDocuments(documents, state.sortType)
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

    fun setSortType(type: SortType) {
        _uiState.update { state ->
            state.copy(
                documents = sortDocuments(state.documents, type),
                sortType = type
            )
        }
    }

    private fun sortDocuments(documents: List<PdfDocument>, sortType: SortType): List<PdfDocument> {
        return when (sortType) {
            SortType.DATE_DESC -> documents.sortedByDescending { it.addedDate }
            SortType.DATE_ASC -> documents.sortedBy { it.addedDate }
            SortType.NAME_ASC -> documents.sortedBy { it.title }
            SortType.NAME_DESC -> documents.sortedByDescending { it.title }
        }
    }

    fun deleteDocument(documentId: String) {
        viewModelScope.launch {
            try {
                // Удаляем файл
                repository.getSignedDocumentFile(documentId).delete()
                // Обновляем список
                loadDocuments()
                _uiState.update { it.copy(error = "Документ удален") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при удалении: ${e.message}") }
            }
        }
    }

    fun shareDocument(documentId: String) {
        viewModelScope.launch {
            try {
                val file = repository.getSignedDocumentFile(documentId)
                // Эмитим событие для шаринга
                _shareEvent.emit(file)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Ошибка при шаринге: ${e.message}") }
            }
        }
    }

    // Событие для шаринга
    private val _shareEvent = MutableSharedFlow<File>()
    val shareEvent = _shareEvent.asSharedFlow()
} 