package com.walhalla.pdfsignature.ui.screen

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.io.File


@Composable
fun PdfViewer(
    pdfFile: File,
    signatureAreas: List<SignatureArea>,
    signaturesByPage: Map<Int, Bitmap>,
    onSignatureAreaClick: (pageIndex: Int) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density

    val renderer = remember(pdfFile) {
        PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
    }
    val pageCount = renderer.pageCount
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        for (pageIndex in 0 until pageCount) {
            val bitmap = remember { renderPage(renderer, pageIndex) }
            val pageWidth = bitmap.width
            val pageHeight = bitmap.height

            val finalBitmap = remember(bitmap) {
                val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutableBitmap)

                // Рисуем подпись, если она есть
                signaturesByPage[pageIndex]?.let { signatureBitmap ->
                    signatureAreas.filter { it.page == pageIndex }.forEach { area ->
                        val xInPx = (area.x * density).toInt()
                        val yInPx = (area.y * density).toInt()
                        val widthInPx = (area.width * density).toInt()
                        val heightInPx = (area.height * density).toInt()

                        // Нарисовать подпись
                        canvas.drawBitmap(
                            signatureBitmap,
                            Rect(0, 0, signatureBitmap.width, signatureBitmap.height),
                            Rect(xInPx, yInPx, xInPx + widthInPx, yInPx + heightInPx),
                            null
                        )
                    }
                }

                mutableBitmap // Возвращаем обновленный Bitmap
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    bitmap = finalBitmap.asImageBitmap(),
                    contentDescription = "PDF Page",
                    modifier = Modifier.fillMaxWidth()
                )

                // Область для клика, вызывающая окно подписи
                signatureAreas.filter { it.page == pageIndex }.forEach { area ->
                    val xInPx = (area.x * density).toInt()
                    val yInPx = (area.y * density).toInt()
                    val widthInPx = (area.width * density).toInt()
                    val heightInPx = (area.height * density).toInt()

                    Box(
                        modifier = Modifier
                            .offset(x = xInPx.dp, y = yInPx.dp)
                            .size(widthInPx.dp, heightInPx.dp)
                            .background(Color.Red.copy(alpha = 0.3f))
                            .pointerInput(Unit) {
                                detectTapGestures { onSignatureAreaClick(pageIndex) }
                            }
                    )
                }
            }
        }
    }
}


//@Composable
//fun PdfViewer(
//    pdfFile: File,
//    signatureAreas: List<SignatureArea>,
//    signaturesByPage: Map<Int, Bitmap>,
//    onSignatureAreaClick: (pageIndex: Int) -> Unit
//) {
//    var context = LocalContext.current
//
//    val renderer = remember(pdfFile) {
//        PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
//    }
//    val pageCount = renderer.pageCount
//    val scrollState = rememberScrollState()
//
//    Column(modifier = Modifier.verticalScroll(scrollState)) {
//        for (pageIndex in 0 until pageCount) {
//            val bitmap = remember { renderPage(renderer, pageIndex) }
//            Box(modifier = Modifier.fillMaxWidth()) {
//                Image(
//                    bitmap = bitmap.asImageBitmap(),
//                    contentDescription = "PDF Page",
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                // Render the signature for this page
//                signaturesByPage[pageIndex]?.let { signatureBitmap ->
//                    signatureAreas.filter { it.page == pageIndex }.forEach { area ->
//                        Box(
//                            modifier = Modifier
//                                .offset(x = area.x.dp, y = area.y.dp)
//                                .size(area.width.dp, area.height.dp)
//                        ) {
//                            Image(
//                                bitmap = signatureBitmap.asImageBitmap(),
//                                contentDescription = "Signature",
//                                modifier = Modifier.fillMaxSize()
//                            )
//                        }
//                    }
//                }
//
//                // Область для клика, вызывающая окно подписи
//                signatureAreas.filter { it.page == pageIndex }.forEach { area ->
//                    Box(
//                        modifier = Modifier
//                            .offset(x = area.x.dp, y = area.y.dp)
//                            .size(area.width.dp, area.height.dp)
//                            .background(Color.Red.copy(alpha = 0.3f))
//                            .pointerInput(Unit) {
//                                detectTapGestures { onSignatureAreaClick(pageIndex) }
//                            }
//                    )
//                }
//            }
//        }
//    }
//}