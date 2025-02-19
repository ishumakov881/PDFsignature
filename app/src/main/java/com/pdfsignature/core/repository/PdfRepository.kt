package com.pdfsignature.core.repository

import android.graphics.Bitmap
import java.io.File
import kotlinx.coroutines.flow.Flow

interface PdfRepository {
    suspend fun getPdfFromAssets(fileName: String): File
    suspend fun getPdfFromLocal(uri: String): File
    suspend fun getPdfFromRemote(url: String): File
    suspend fun savePdfWithSignature(file: File, signature: Bitmap, page: Int, x: Float, y: Float): File
    suspend fun getAllDocuments(): Flow<List<PdfDocument>>
    suspend fun getSignedDocuments(): Flow<List<PdfDocument>>
    suspend fun saveSignatureNotation(documentId: String, page: Int, x: Float, y: Float)
    suspend fun getSignatureNotations(documentId: String): Flow<List<SignatureNotation>>
    suspend fun removeSignatureFromPdf(file: File, page: Int, x: Float, y: Float): File
    suspend fun saveDocument(document: PdfDocument)
    suspend fun updateNotationPosition(
        documentId: String,
        page: Int,
        oldX: Float,
        oldY: Float,
        newX: Float,
        newY: Float
    )
    suspend fun deleteNotation(
        documentId: String,
        page: Int,
        x: Float,
        y: Float
    )
    suspend fun createSignedPdfCopy(
        file: File,
        notations: List<SignatureNotation>
    ): File
    suspend fun getSignedDocumentFile(fileName: String): File
    suspend fun resetSignatures(documentId: String)
}

data class PdfDocument(
    val id: String,
    val title: String,
    val addedDate: Long,
    val path: String
)

data class SignatureNotation(
    val documentId: String,
    val page: Int,
    val x: Float, // X в процентах (0-100)
    val y: Float,  // Y в процентах (0-100)
    val signatureBitmap: Bitmap?
) 