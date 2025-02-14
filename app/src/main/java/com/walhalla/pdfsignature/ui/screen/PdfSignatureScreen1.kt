package com.walhalla.pdfsignature.ui.screen

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import com.walhalla.pdfsignature.SignatureCanvas


import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfSignatureScreen1() {
    val context = LocalContext.current
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var signatureCoordinates by remember { mutableStateOf<List<SignatureArea>>(emptyList()) }

    var showSignatureDialog by remember { mutableStateOf(false) }
    var pageIndex by remember { mutableStateOf(-1) }


    // Используем mutableStateMapOf, чтобы изменения подписей обновляли UI
    val signaturesByPage = remember { mutableStateMapOf<Int, Bitmap>() }

    val pdfLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                val tempFile = File(context.cacheDir, "selected.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                pdfFile = tempFile
                signatureCoordinates = loadSignatureCoordinates(context)
            }
        }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = { pdfLauncher.launch(arrayOf("application/pdf")) }) {
            Text("Select PDF")
        }
        pdfFile?.let { file ->
            PdfViewer(file, signatureCoordinates, signaturesByPage) { pageIndex_ ->
                pageIndex = pageIndex_
                println("Page: $pageIndex")
                showSignatureDialog = true
            }
        }

        if (showSignatureDialog) {
//            SignatureDialog(
//                onDismiss = { showSignatureDialog = false },
//                onSaveSignature = { bitmap ->
//                    val page = 0  // Здесь можно определить страницу динамически
//                    signaturesByPage[page] = bitmap  // Обновляем карту подписей
//                }
//            )

            val density = 1f//LocalContext.current.resources.displayMetrics.density

            pdfFile?.let { pdfFile ->
                signaturesByPage[pageIndex]?.let {
                    extractAreaFromBitmap(
                        sourceBitmap = it,
                        signatureCoordinates[pageIndex], density)
                }?.let {
                    SignatureDialog(
                        onDismiss = { showSignatureDialog = false },
                        onSaveSignature = { bitmap ->
                            signaturesByPage[pageIndex] = bitmap  // Обновляем карту подписей
                        },
                        previewBitmap= it
                    )
                }
            }
        }
    }
}


//fun extractArea(bitmap: Bitmap?, it: SignatureArea): RectF {
//    return RectF(it.x, it.y, it.x + it.width, it.y + it.height)
//}


data class SignatureArea(
    val page: Int,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

fun loadSignatureCoordinates(context: Context): List<SignatureArea> {
    val jsonString = """
        {
            "signatures": [
                { "page": 0, "x": 10, "y": 50, "width": 20, "height": 50 }
            ]
        }
    """
    val jsonObject = JSONObject(jsonString)
    val signaturesArray = jsonObject.getJSONArray("signatures")
    val coordinates = mutableListOf<SignatureArea>()
    for (i in 0 until signaturesArray.length()) {
        val obj = signaturesArray.getJSONObject(i)
        coordinates.add(
            SignatureArea(
                obj.getInt("page"),
                obj.getDouble("x").toFloat(),
                obj.getDouble("y").toFloat(),
                obj.getDouble("width").toFloat(),
                obj.getDouble("height").toFloat()
            )
        )
    }
    return coordinates
}

fun renderPage(renderer: PdfRenderer, pageIndex: Int): Bitmap {
    val page = renderer.openPage(pageIndex)

    // Создаём Bitmap для рендеринга страницы
    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Рисуем белый фон (чтобы избежать прозрачности)
    canvas.drawColor(Color.WHITE)

    // Рендерим страницу PDF в Bitmap
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    page.close()

    return bitmap
}

//@Composable
//fun SignatureDialog(onDismiss: () -> Unit, onSaveSignature: (Bitmap) -> Unit) {
//    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
//
//    AlertDialog(
//        onDismissRequest = onDismiss,
//        title = { Text("Sign Document") },
//        text = {
//            SignatureCanvas { bitmap ->
//                signatureBitmap = bitmap
//            }
//        },
//        confirmButton = {
//            Button(onClick = {
//                signatureBitmap?.let { onSaveSignature(it) }
//                onDismiss()
//            }) {
//                Text("Save")
//            }
//        }
//    )
//}
fun extractAreaFromBitmap(
    sourceBitmap: Bitmap,
    area: SignatureArea,
    density: Float
): Bitmap {
    // Преобразуем координаты в пиксели с учетом плотности экрана
    val left = (area.x * density).toInt()
    val top = (area.y * density).toInt()
    val right = ((area.x + area.width) * density).toInt()
    val bottom = ((area.y + area.height) * density).toInt()

    // Убедимся, что области не выходят за пределы исходного Bitmap
    val clampedLeft = left.coerceIn(0, sourceBitmap.width)
    val clampedTop = top.coerceIn(0, sourceBitmap.height)
    val clampedRight = right.coerceIn(clampedLeft, sourceBitmap.width)
    val clampedBottom = bottom.coerceIn(clampedTop, sourceBitmap.height)

    // Создаём новый Bitmap с обрезанным содержимым
    return Bitmap.createBitmap(sourceBitmap, clampedLeft, clampedTop, clampedRight - clampedLeft, clampedBottom - clampedTop)
}

@Composable
fun SignatureDialog(
    previewBitmap: Bitmap,

    onDismiss: () -> Unit,
    onSaveSignature: (Bitmap) -> Unit
) {
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign Document") },
        text = {
            if (previewBitmap != null) {
                Box(modifier = Modifier.size(300.dp, 150.dp)) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "PDF Preview",
                        modifier = Modifier.fillMaxSize()
                    )
                    SignatureCanvas { bitmap ->
                        signatureBitmap = bitmap
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                signatureBitmap?.let { onSaveSignature(it) }
                onDismiss()
            }) {
                Text("Save")
            }
        }
    )
}


fun extractPdfPageArea(
    page: PdfRenderer.Page,
    area: SignatureArea,
    density: Float
): Bitmap {

    val left = (area.x * density).toInt()
    val top = (area.y * density).toInt()
    val right = ((area.x + area.width) * density).toInt()
    val bottom = ((area.y + area.height) * density).toInt()

    // Создаем Bitmap для рендеринга области
    val width = right - left
    val height = bottom - top
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    // Рендерим только указанную область
    val canvas = Canvas(bitmap)
    page.render(
        bitmap,
        Rect(left, top, right, bottom),
        null,
        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
    )
    page.close()

    return bitmap
}


fun createSignatureBitmap(path: List<Offset>): Bitmap {
    val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { color = Color.BLACK; strokeWidth = 5f }
    for (i in 1 until path.size) {
        canvas.drawLine(path[i - 1].x, path[i - 1].y, path[i].x, path[i].y, paint)
    }
    return bitmap
}
