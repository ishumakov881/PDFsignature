package com.walhalla.pdfsignature.data.db.dao

import androidx.room.*
import com.walhalla.pdfsignature.data.db.entity.SignatureNotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SignatureNotationDao {
    @Query("SELECT * FROM signature_notations WHERE documentId = :documentId")
    fun getNotationsForDocument(documentId: String): Flow<List<SignatureNotationEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotation(notation: SignatureNotationEntity): Long
    
    @Update
    suspend fun updateNotation(notation: SignatureNotationEntity)
    
    @Delete
    suspend fun deleteNotation(notation: SignatureNotationEntity)
    
    @Query("UPDATE signature_notations SET signatureImage = :signatureImage WHERE id = :notationId")
    suspend fun updateSignature(notationId: Long, signatureImage: String)
    
    @Query("DELETE FROM signature_notations WHERE documentId = :documentId")
    suspend fun deleteAllNotationsForDocument(documentId: String)
} 