package com.pdfsignature.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pdfsignature.core.viewer.PdfNotation
import com.pdfsignature.core.viewer.PdfViewer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewerImpl : PdfViewer {


    private fun drawMarker(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {


        //Render
        val point = radius * 10 / 12

        val width = point * 12
        val height = point * 9
        val left = centerX - width / 2
        val top = centerY - height / 2
        val right = centerX + width / 2
        val bottom = centerY + height / 2

        // Рисуем красный прямоугольник
        val rectPaint = Paint().apply {
            color = Color.RED
            alpha = 180
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(left, top, right, bottom, rectPaint)

        // Рисуем белую обводку
        val strokePaint = Paint().apply {
            color = Color.WHITE
            alpha = 255
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        canvas.drawRect(left, top, right, bottom, strokePaint)
//        // Если есть подпись, добавляем зеленый индикатор в центре
//        if (hasSignature) {
//            val indicatorPaint = Paint().apply {
//                color = Color.GREEN
//                alpha = 255
//                style = Paint.Style.FILL
//                isAntiAlias = true
//            }
//            canvas.drawCircle(centerX, centerY, 2.5f, indicatorPaint)
//        }else{
//
//        }
    }


    private fun drawMarkerDot(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        hasSignature: Boolean = false
    ) {
        // Рисуем красную точку
        val dotPaint = Paint().apply {
            color = Color.RED
            alpha = 180
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, radius, dotPaint)

        // Рисуем белую обводку
        val strokePaint = Paint().apply {
            color = Color.WHITE
            alpha = 255
            style = Paint.Style.STROKE
            strokeWidth = radius * 0.2f
            isAntiAlias = true
        }
        canvas.drawCircle(centerX, centerY, radius, strokePaint)

        // Если есть подпись, добавляем зеленый индикатор
        if (hasSignature) {
            val indicatorPaint = Paint().apply {
                color = Color.GREEN
                alpha = 255
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(centerX, centerY, radius * 0.3f, indicatorPaint)
        }
    }

    @Composable
    override fun DisplayPdf(
        file: File,
        onPageClick: ((page: Int, x: Float, y: Float, existingSignature: Bitmap?) -> Unit)?,
        notations: List<PdfNotation>,
        currentPage: Int,
        onPageChanged: ((Int) -> Unit)?
    ) {
        val context = LocalContext.current
        var pageCount by remember { mutableStateOf(1) }
        var pageBitmaps by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
        var imageSize by remember { mutableStateOf(Size(0f, 0f)) }
        val listState = rememberLazyListState()

        var hasSignature by remember { mutableStateOf(false) }

        // Загружаем и обновляем страницы PDF при изменении нотаций
        LaunchedEffect(file, notations) {
            withContext(Dispatchers.IO) {
                try {
                    val renderer = PdfRenderer(
                        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    )
                    pageCount = renderer.pageCount

                    val newBitmaps = mutableMapOf<Int, Bitmap>()
                    for (pageIndex in 0 until pageCount) {
                        val page = renderer.openPage(pageIndex)
                        val scale = 2 // Масштаб для лучшего качества
                        val bitmap = Bitmap.createBitmap(
                            page.width * scale,
                            page.height * scale,
                            Bitmap.Config.ARGB_8888
                        )

                        page.render(
                            bitmap,
                            null,
                            null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                        )

                        // Создаем новый битмап для рисования поверх PDF
                        bitmap.config?.let {
                            val overlayBitmap = bitmap.copy(bitmap.config!!, true)
                            bitmap.recycle() // Освобождаем оригинальный битмап

                            val canvas = Canvas(overlayBitmap)

                            // Рисуем нотации
                            notations.filter { it.page == pageIndex }.forEach { notation ->
                                println("DEBUG: PdfViewerImpl: Обработка нотации на странице ${notation.page}")

                                // Конвертируем проценты в пиксели
                                val centerX = (notation.x / 100f) * overlayBitmap.width
                                val centerY = (notation.y / 100f) * overlayBitmap.height
                                val radius = overlayBitmap.width * 0.015f

                                // Рисуем маркер
                                hasSignature = notation.signatureBitmap != null

                                withContext(Dispatchers.Main){
                                    Toast.makeText(context, "hs: $hasSignature", Toast.LENGTH_SHORT).show()
                                }

                                if (!hasSignature) {
                                    drawMarker(
                                        canvas = canvas,
                                        centerX = centerX,
                                        centerY = centerY,
                                        radius = radius
                                    )
                                }
                                // Если есть подпись, рисуем её поверх
                                notation.signatureBitmap?.let { signatureBitmap ->
                                    val signatureX = centerX - (signatureBitmap.width / 2f)
                                    val signatureY = centerY - (signatureBitmap.height / 2f)
                                    canvas.drawBitmap(signatureBitmap, signatureX, signatureY, null)
                                }
                            }

                            newBitmaps[pageIndex] = overlayBitmap
                        }

                        page.close()
                    }

                    // Освобождаем старые битмапы после создания новых
                    withContext(Dispatchers.Main) {
                        val oldBitmaps = pageBitmaps.toMap() // Создаем копию старых битмапов
                        pageBitmaps = newBitmaps // Обновляем ссылку на новые битмапы
                        // Освобождаем старые битмапы после обновления ссылки
                        oldBitmaps.values.forEach {
                            if (!it.isRecycled) {
                                it.recycle()
                            }
                        }
                    }

                    renderer.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Очищаем битмапы при уничтожении компонента
        DisposableEffect(Unit) {
            onDispose {
                val bitmapsToRecycle =
                    pageBitmaps.toMap() // Создаем копию для безопасного освобождения
                bitmapsToRecycle.values.forEach {
                    if (!it.isRecycled) {
                        it.recycle()
                    }
                }
            }
        }

        // Отслеживаем текущую страницу по скроллу
        LaunchedEffect(listState.firstVisibleItemIndex) {
            onPageChanged?.invoke(listState.firstVisibleItemIndex)
        }

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pageCount) { pageIndex ->
                    pageBitmaps[pageIndex]?.let { bitmap ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "PDF Page ${pageIndex + 1}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        imageSize =
                                            Size(size.width.toFloat(), size.height.toFloat())
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val xPercent = (offset.x / imageSize.width) * 100f
                                            val yPercent = (offset.y / imageSize.height) * 100f

                                            // Проверяем, есть ли подпись в точке клика
                                            val existingNotation = notations.find { notation ->
                                                val notationX =
                                                    (notation.x / 100f) * imageSize.width
                                                val notationY =
                                                    (notation.y / 100f) * imageSize.height
                                                val radius = imageSize.width * 0.015f

                                                val clickPoint = Offset(offset.x, offset.y)
                                                val notationPoint = Offset(notationX, notationY)

                                                // Проверяем, попал ли клик в область точки
                                                (clickPoint - notationPoint).getDistance() <= radius
                                            }

                                            onPageClick?.invoke(
                                                pageIndex,
                                                xPercent,
                                                yPercent,
                                                existingNotation?.signatureBitmap
                                            )
                                        }
                                    },
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }
            }
        }
    }
} 