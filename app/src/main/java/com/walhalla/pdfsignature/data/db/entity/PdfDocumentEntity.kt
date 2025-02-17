package com.walhalla.pdfsignature.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val path: String,
    val addedDate: Long,
    val hasNotations: Boolean = false,
    val lastViewed: Long? = null
) 