package com.walhalla.pdfsignature.ui.screen

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
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

    // Используем mutableStateMapOf, чтобы изменения подписей обновляли UI
    val signaturesByPage = remember { mutableStateMapOf<Int, Bitmap>() }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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
            PdfViewer(file, signatureCoordinates, signaturesByPage) { showSignatureDialog = true }
        }
        if (showSignatureDialog) {
            SignatureDialog(
                onDismiss = { showSignatureDialog = false },
                onSaveSignature = { bitmap ->
                    val page = 0  // Здесь можно определить страницу динамически
                    signaturesByPage[page] = bitmap  // Обновляем карту подписей
                }
            )
        }
    }
}


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
                { "page": 0, "x": 10, "y": 50, "width": 200, "height": 50 }
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

@Composable
fun PdfViewer(
    pdfFile: File,
    signatureAreas: List<SignatureArea>,
    signaturesByPage: Map<Int, Bitmap>,
    onSignatureAreaClick: () -> Unit
) {
    var context = LocalContext.current

    val renderer = remember(pdfFile) {
        PdfRenderer(ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY))
    }
    val pageCount = renderer.pageCount
    val scrollState = rememberScrollState()

    Column(modifier = Modifier.verticalScroll(scrollState)) {
        for (pageIndex in 0 until pageCount) {
            val bitmap = remember { renderPage(renderer, pageIndex) }
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "PDF Page", modifier = Modifier.fillMaxWidth())

                // Render the signature for this page
                signaturesByPage[pageIndex]?.let { signatureBitmap ->
                    signatureAreas.filter { it.page == pageIndex }.forEach { area ->
                        Box(
                            modifier = Modifier
                                .offset(x = area.x.dp, y = area.y.dp)
                                .size(area.width.dp, area.height.dp)
                        ) {
                            Image(bitmap = signatureBitmap.asImageBitmap(), contentDescription = "Signature", modifier = Modifier.fillMaxSize())
                        }
                    }
                }

                // Область для клика, вызывающая окно подписи
                signatureAreas.filter { it.page == pageIndex }.forEach { area ->
                    Box(
                        modifier = Modifier
                            .offset(x = area.x.dp, y = area.y.dp)
                            .size(area.width.dp, area.height.dp)
                            .background(Color.Red.copy(alpha = 0.3f))
                            .pointerInput(Unit) {
                                detectTapGestures { onSignatureAreaClick() }
                            }
                    )
                }
            }
        }
    }
}
fun renderPage(renderer: PdfRenderer, pageIndex: Int): Bitmap {
    val page = renderer.openPage(pageIndex)

    // Создаём Bitmap для рендеринга страницы
    val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Рисуем белый фон (чтобы избежать прозрачности)
    canvas.drawColor(android.graphics.Color.WHITE)

    // Рендерим страницу PDF в Bitmap
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    page.close()

    return bitmap
}

@Composable
fun SignatureDialog(onDismiss: () -> Unit, onSaveSignature: (Bitmap) -> Unit) {
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sign Document") },
        text = {
            SignatureCanvas { bitmap ->
                signatureBitmap = bitmap
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


fun createSignatureBitmap(path: List<Offset>): Bitmap {
    val bitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply { color = android.graphics.Color.BLACK; strokeWidth = 5f }
    for (i in 1 until path.size) {
        canvas.drawLine(path[i - 1].x, path[i - 1].y, path[i].x, path[i].y, paint)
    }
    return bitmap
}
