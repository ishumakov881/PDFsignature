package com.pdfsignature.data.db.dao

import androidx.room.*
import com.pdfsignature.data.db.entity.PdfDocumentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfDocumentDao {
    @Query("SELECT * FROM pdf_documents ORDER BY addedDate DESC")
    fun getAllDocuments(): Flow<List<PdfDocumentEntity>>
    
    @Query("SELECT * FROM pdf_documents WHERE id = :id")
    suspend fun getDocumentById(id: String): PdfDocumentEntity?
    
    @Query("SELECT * FROM pdf_documents ORDER BY lastViewed DESC LIMIT 1")
    suspend fun getLastViewedDocument(): PdfDocumentEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: PdfDocumentEntity)
    
    @Update
    suspend fun updateDocument(document: PdfDocumentEntity)
    
    @Delete
    suspend fun deleteDocument(document: PdfDocumentEntity)
    
    @Query("UPDATE pdf_documents SET hasNotations = :hasNotations WHERE id = :documentId")
    suspend fun updateHasNotations(documentId: String, hasNotations: Boolean)
    
    @Query("UPDATE pdf_documents SET lastViewed = :timestamp WHERE id = :documentId")
    suspend fun updateLastViewed(documentId: String, timestamp: Long)
} 