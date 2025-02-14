package com.walhalla.pdfsignature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

data class SignResult(
    val uri: Uri?,
    val message: String
)

data class PdfPageInfo(
    val pageCount: Int,
    val currentPage: Int,
    val bitmap: Bitmap
)

object PdfUtils {
    fun init(context: Context) {
        PDFBoxResourceLoader.init(context)
    }

    fun getPdfInfo(context: Context, uri: Uri): PdfPageInfo? {
        return try {
            val input = context.contentResolver.openFileDescriptor(uri, "r")
            input?.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                renderer.use { pdfRenderer ->
                    val pageCount = pdfRenderer.pageCount
                    val page = pdfRenderer.openPage(0)
                    page.use { currentPage ->
                        val bitmap = Bitmap.createBitmap(
                            currentPage.width,
                            currentPage.height,
                            Bitmap.Config.ARGB_8888
                        )
                        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        PdfPageInfo(pageCount, 0, bitmap)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): Bitmap? {
        return try {
            val input = context.contentResolver.openFileDescriptor(uri, "r")
            input?.use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                renderer.use { pdfRenderer ->
                    if (pageIndex >= pdfRenderer.pageCount) return null
                    
                    val page = pdfRenderer.openPage(pageIndex)
                    page.use { currentPage ->
                        val bitmap = Bitmap.createBitmap(
                            currentPage.width,
                            currentPage.height,
                            Bitmap.Config.ARGB_8888
                        )
                        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun signPdf(
        context: Context,
        sourceUri: Uri,
        signatureBitmap: Bitmap,
        x: Float,
        y: Float,
        pageIndex: Int = 0,
        onComplete: (SignResult) -> Unit
    ) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                if (pageIndex >= document.numberOfPages) {
                    onComplete(SignResult(null, "Ошибка: страница $pageIndex не существует в документе"))
                    return
                }

                val page = document.getPage(pageIndex)
                val pageHeight = page.mediaBox.height
                val pageWidth = page.mediaBox.width
                
                val scaleX = pageWidth / signatureBitmap.width.toFloat()
                val scaleY = pageHeight / signatureBitmap.height.toFloat()
                
                val signatureImage = JPEGFactory.createFromImage(document, signatureBitmap)
                
                PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, true, true).use { contentStream ->
                    val pdfX = x * scaleX
                    val pdfY = pageHeight - (y * scaleY) - signatureImage.height
                    
                    contentStream.drawImage(
                        signatureImage,
                        pdfX,
                        pdfY
                    )
                }

                val outputFile = File(context.cacheDir, "signed_${System.currentTimeMillis()}.pdf")
                document.save(outputFile)
                document.close()

                val resultUri = Uri.fromFile(outputFile)
                onComplete(SignResult(
                    resultUri,
                    """PDF успешно подписан
                    |Сохранен как: ${outputFile.name}
                    |Размер: ${outputFile.length() / 1024} KB
                    |Страница: ${pageIndex + 1} из ${document.numberOfPages}
                    |Размер страницы: ${pageWidth.toInt()}x${pageHeight.toInt()}
                    |Координаты подписи: x=pdfX, y=pdfY""".trimMargin()
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(SignResult(null, "Ошибка при подписании PDF: ${e.localizedMessage}"))
        }
    }

    fun pathToBitmap(path: Path, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(android.graphics.Color.TRANSPARENT)
        
        val paint = Paint().apply {
            color = android.graphics.Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 5f
            isAntiAlias = true
        }

        val androidPath = path.asAndroidPath()
        canvas.drawPath(androidPath, paint)

        return bitmap
    }
} 