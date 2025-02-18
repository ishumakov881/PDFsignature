package com.pdfsignature.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey



@Entity(tableName = "pdf_documents")
data class PdfDocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val path: String,
    val addedDate: Long,
    val hasNotations: Boolean = false,
    val lastViewed: Long? = null,
    //val _id: Long
)

@Entity(
    tableName = "signature_notations",
    foreignKeys = [
        ForeignKey(
            entity = PdfDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["documentId"])
    ]
)

data class SignatureNotationEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long? = null,
    val documentId: String,
    val page: Int,
    val xPercent: Float, // Позиция X в процентах от ширины страницы (0-100)
    val yPercent: Float, // Позиция Y в процентах от высоты страницы (0-100)
    val widthPercent: Float = 20f,  // Ширина в процентах от ширины страницы
    val heightPercent: Float = 10f,  // Высота в процентах от высоты страницы
    val signatureImage: String? = null
) 