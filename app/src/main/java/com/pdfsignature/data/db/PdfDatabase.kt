package com.pdfsignature.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.pdfsignature.data.db.dao.PdfDocumentDao
import com.pdfsignature.data.db.dao.SignatureNotationDao
import com.pdfsignature.data.db.entity.PdfDocumentEntity
import com.pdfsignature.data.db.entity.SignatureNotationEntity


@Database(
    entities = [
        PdfDocumentEntity::class,
        SignatureNotationEntity::class
    ],
    version = 1
)

abstract class PdfDatabase : RoomDatabase() {
    abstract fun pdfDocumentDao(): PdfDocumentDao
    abstract fun signatureNotationDao(): SignatureNotationDao
} 