package com.walhalla.pdfsignature.ui.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walhalla.pdfsignature.core.repository.PdfDocument
import com.walhalla.pdfsignature.core.repository.PdfRepository
import com.walhalla.pdfsignature.core.repository.SignatureNotation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class CurrentDocumentState(
    val isLoading: Boolean = false,
    val currentDocument: PdfDocument? = null,
    val pdfFile: File? = null,
    val notations: List<SignatureNotation> = emptyList(),
    val error: String? = null
)

class CurrentDocumentViewModel(
    private val repository: PdfRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CurrentDocumentState())
    val uiState: StateFlow<CurrentDocumentState> = _uiState.asStateFlow()

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

    fun addSignature(signature: Bitmap, page: Int, x: Float, y: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                println("DEBUG: ViewModel: Добавляем подпись на странице $page в точке ($x, $y)")
                val currentState = _uiState.value
                val document = currentState.currentDocument
                val file = currentState.pdfFile
                
                if (document != null && file != null) {
                    val signedFile = repository.savePdfWithSignature(file, signature, page, x, y)
                    println("DEBUG: ViewModel: Подпись добавлена, обновляем UI")
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            pdfFile = signedFile
                        )
                    }
                }
            } catch (e: Exception) {
                println("DEBUG: ViewModel: Ошибка при добавлении подписи: ${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun removeSignature(page: Int, x: Float, y: Float) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val currentState = _uiState.value
                val document = currentState.currentDocument
                val file = currentState.pdfFile
                
                if (document != null && file != null) {
                    val updatedFile = repository.removeSignatureFromPdf(file, page, x, y)
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            pdfFile = updatedFile
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