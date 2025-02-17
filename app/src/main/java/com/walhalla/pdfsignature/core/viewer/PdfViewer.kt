package com.walhalla.pdfsignature.core.viewer

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import java.io.File

interface PdfViewer {
    @Composable
    fun DisplayPdf(
        file: File,
        onPageClick: ((page: Int, x: Float, y: Float, existingSignature: Bitmap?) -> Unit)?,
        notations: List<PdfNotation>,
        currentPage: Int,
        onPageChanged: ((Int) -> Unit)?
    )
}

data class PdfNotation(
    val page: Int,
    val x: Float, // X в процентах (0-100)
    val y: Float, // Y в процентах (0-100)
    val width: Float = 20f, // Ширина в процентах от ширины страницы
    val height: Float = 10f, // Высота в процентах от высоты страницы
    val signatureBitmap: Bitmap? = null
) 