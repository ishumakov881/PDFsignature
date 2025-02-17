package com.walhalla.pdfsignature.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walhalla.pdfsignature.core.repository.PdfDocument
import com.walhalla.pdfsignature.core.repository.PdfRepository
import com.walhalla.pdfsignature.core.repository.SignatureNotation
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
} 