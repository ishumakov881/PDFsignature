package com.walhalla.pdfsignature.ui.screen

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.walhalla.pdfsignature.PdfUtils
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var signedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var signatureBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPage by remember { mutableStateOf(0) }
    var totalPages by remember { mutableStateOf(1) }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val storagePermission = rememberPermissionState(
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    fun loadPdfPage(pageIndex: Int) {
        selectedPdfUri?.let { uri ->
            pdfBitmap = PdfUtils.renderPdfPage(context, uri, pageIndex)
            currentPage = pageIndex
            signatureBitmap = null
            scope.launch {
                scrollState.scrollTo(0)
            }
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedPdfUri = it
            signedPdfUri = null
            signatureBitmap = null

            PdfUtils.getPdfInfo(context, it)?.let { info ->
                totalPages = info.pageCount
                currentPage = 0
                pdfBitmap = info.bitmap
                val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "неизвестный файл"
                statusMessage = "Выбран файл: $fileName (${info.pageCount} стр.)"
            }

            scope.launch {
                scrollState.scrollTo(0)
            }
        }
    }

    statusMessage?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            statusMessage = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    if (storagePermission.status.isGranted) {
                        pdfLauncher.launch("application/pdf")
                    } else {
                        storagePermission.launchPermissionRequest()
                        statusMessage = "Необходимо разрешение для доступа к файлам"
                    }
                }
            ) {
                Text(if (selectedPdfUri == null) "Выбрать PDF" else "Сменить PDF")
            }

            if (selectedPdfUri != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (currentPage > 0) loadPdfPage(currentPage - 1) },
                        enabled = currentPage > 0
                    ) {
                        Icon(Icons.Default.ArrowDropDown, "Предыдущая страница")
                    }

                    Text("${currentPage + 1} / $totalPages")

                    IconButton(
                        onClick = { if (currentPage < totalPages - 1) loadPdfPage(currentPage + 1) },
                        enabled = currentPage < totalPages - 1
                    ) {
                        Icon(Icons.Default.ArrowDropDown, "Следующая страница")
                    }
                }
            }

            signedPdfUri?.let { uri ->
                IconButton(
                    onClick = {
                        try {
                            val file = File(uri.path!!)
                            val contentUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(
                                    intent,
                                    "Поделиться подписанным PDF"
                                )
                            )
                            statusMessage = "Открываю меню для отправки файла"
                        } catch (e: Exception) {
                            statusMessage = "Ошибка при отправке файла: ${e.localizedMessage}"
                        }
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Поделиться")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedPdfUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                pdfBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF страница ${currentPage + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(1f),
                        contentScale = ContentScale.FillWidth
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .zIndex(2f)
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
//                        SignaturePadView(
//                            modifier = Modifier.matchParentSize(),
//                            onSignatureDrawn = { bitmap ->
//                                signatureBitmap = bitmap
//                            },
//                            onSignatureCleared = {
//                                signatureBitmap = null
//                                signedPdfUri = null
//                                statusMessage = "Подпись очищена"
//                            }
//                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        signatureBitmap = null
                        signedPdfUri = null
                        statusMessage = "Подпись очищена"
                    }
                ) {
                    Text("Очистить")
                }
                Button(
                    onClick = {
                        selectedPdfUri?.let { uri ->
                            signatureBitmap?.let { bitmap ->
                                isProcessing = true
                                statusMessage = "Подписываю PDF..."
                                PdfUtils.signPdf(
                                    context = context,
                                    sourceUri = uri,
                                    signatureBitmap = bitmap,
                                    x = 0f,
                                    y = 0f,
                                    pageIndex = currentPage
                                ) { result ->
                                    isProcessing = false
                                    statusMessage = result.message
                                    signedPdfUri = result.uri
                                }
                            } ?: run {
                                statusMessage = "Сначала нарисуйте подпись"
                            }
                        }
                    },
                    enabled = selectedPdfUri != null && !isProcessing && signatureBitmap != null
                ) {
                    Text(if (isProcessing) "Подписываю..." else "Подписать PDF")
                }
            }
        } else {
            Text(
                "Выберите PDF документ для подписи",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}