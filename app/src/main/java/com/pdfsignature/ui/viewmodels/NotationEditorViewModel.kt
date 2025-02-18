package com.pdfsignature.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsignature.core.repository.PdfDocument
import com.pdfsignature.core.repository.PdfRepository
import com.pdfsignature.core.repository.SignatureNotation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class NotationEditorState(
    val isLoading: Boolean = false,
    val currentDocument: PdfDocument? = null,
    val pdfFile: File? = null,
    val notations: List<SignatureNotation> = emptyList(),
    val error: String? = null
)

class NotationEditorViewModel(
    private val repository: PdfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NotationEditorState())
    val uiState: StateFlow<NotationEditorState> = _uiState.asStateFlow()

    init {
        loadLastDocument()
    }

    private fun loadLastDocument() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getAllDocuments()
                    .map { documents -> documents.firstOrNull() }
                    .collect { document ->
                        if (document != null) {
                            val file = repository.getPdfFromAssets("sample.pdf")
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    currentDocument = document,
                                    pdfFile = file
                                )
                            }
                            loadNotations(document.id)
                        } else {
                            _uiState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = "Нет доступных документов"
                                )
                            }
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

    private fun loadNotations(documentId: String) {
        viewModelScope.launch {
            repository.getSignatureNotations(documentId)
                .collect { notations ->
                    _uiState.update { it.copy(notations = notations) }
                }
        }
    }

    fun addNotation(page: Int, x: Float, y: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val document = _uiState.value.currentDocument
                if (document != null) {
                    repository.saveSignatureNotation(document.id, page, x, y)
                    _uiState.update { it.copy(isLoading = false) }
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

    fun setCurrentDocument(document: PdfDocument) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    currentDocument = document,
                    isLoading = true
                )
            }
            try {
                val file = repository.getPdfFromAssets("sample.pdf")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        pdfFile = file
                    )
                }
                loadNotations(document.id)
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

    fun updateNotationPosition(page: Int, oldX: Float, oldY: Float, newX: Float, newY: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val document = _uiState.value.currentDocument
                if (document != null) {
                    // Находим нотацию по старым координатам
                    val notation = _uiState.value.notations.find { 
                        it.page == page && 
                        kotlin.math.abs(it.x - oldX) < 0.1f && 
                        kotlin.math.abs(it.y - oldY) < 0.1f 
                    }
                    
                    if (notation != null) {
                        // Обновляем позицию в базе данных
                        repository.updateNotationPosition(
                            documentId = document.id,
                            page = page,
                            oldX = oldX,
                            oldY = oldY,
                            newX = newX,
                            newY = newY
                        )
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Ошибка при обновлении позиции: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteNotation(page: Int, x: Float, y: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val document = _uiState.value.currentDocument
                if (document != null) {
                    // Находим нотацию по координатам
                    val notation = _uiState.value.notations.find { 
                        it.page == page && 
                        kotlin.math.abs(it.x - x) < 0.1f && 
                        kotlin.math.abs(it.y - y) < 0.1f 
                    }
                    
                    if (notation != null) {
                        repository.deleteNotation(
                            documentId = document.id,
                            page = page,
                            x = x,
                            y = y
                        )
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Ошибка при удалении нотации: ${e.message}"
                    )
                }
            }
        }
    }
} 