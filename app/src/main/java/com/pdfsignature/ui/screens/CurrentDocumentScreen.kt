package com.pdfsignature.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.pdfsignature.core.repository.SignatureNotation
import com.pdfsignature.core.viewer.PdfNotation

import com.pdfsignature.core.viewer.PdfViewer
import com.pdfsignature.ui.components.SignaturePadDialog
import com.pdfsignature.ui.viewmodels.CurrentDocumentViewModel
import com.pdfsignature.ui.viewmodels.DocumentListViewModel

import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CurrentDocumentScreen(
    viewModel: CurrentDocumentViewModel = koinViewModel(),
    documentListViewModel: DocumentListViewModel = koinViewModel(),
    pdfViewer: PdfViewer = koinInject(),
    onSettingsClick: () -> Unit
) {


//    var perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
////            android.Manifest.permission.MANAGE_EXTERNAL_STORAGE
////        } else {
////            TODO("VERSION.SDK_INT < R")
////        }
//    } else {
//        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
//    }


    val permissionState =
        rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
    //ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {

    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val uiState by viewModel.uiState.collectAsState()
    var showSignaturePad by remember { mutableStateOf(false) }
    var selectedNotationTriple by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteCoordinates by remember { mutableStateOf<Triple<Int, Float, Float>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var currentNotation: SignatureNotation? by remember { mutableStateOf(null) }

    // Следим за выбранным документом
    LaunchedEffect(Unit) {
        documentListViewModel.selectedDocument.collect { document ->
            document?.let {
                viewModel.loadDocument(it)
            }
        }
    }

    // Обработка события шаринга
    LaunchedEffect(Unit) {
        viewModel.shareEvent.collect { file ->
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Поделиться PDF"))
        }
    }

    // Показываем Snackbar при появлении ошибки
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Документ") },/*Текущий документ*/
                actions = {
                    // Кнопка Reset
                    IconButton(
                        onClick = { viewModel.resetSignatures() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Сбросить подписи",
                            //tint = MaterialTheme.colorScheme.error
                        )
                    }
                    // Кнопка настроек
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }

                    // Кнопка сохранения PDF с подписями
                    IconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                viewModel.savePdfWithSignatures()
                            } else {
                                when {
                                    permissionState.status.isGranted -> {
                                        viewModel.savePdfWithSignatures()
                                    }

                                    permissionState.status.shouldShowRationale -> {
                                        showSettingsDialog = true
                                    }

                                    else -> {
                                        permissionState.launchPermissionRequest()
                                        println("Разрешение не предоставлено.")
                                    }
                                }
                                //Toast.makeText(context, "@@@@ ${permissionState.status.isGranted}", Toast.LENGTH_SHORT).show()

                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Сохранить PDF с подписями"
                        )
                    }
                    // Кнопка шаринга
                    IconButton(
                        onClick = {

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                viewModel.sharePdfWithSignatures()
                            } else {
                                when {
                                    permissionState.status.isGranted -> {
                                        viewModel.sharePdfWithSignatures()
                                    }

                                    permissionState.status.shouldShowRationale -> {
                                        showSettingsDialog = true
                                    }

                                    else -> {
                                        permissionState.launchPermissionRequest()
                                        println("Разрешение не предоставлено... ${permissionState.status.isGranted}")
                                    }
                                }
                            }


                            //Toast.makeText(context, "@@@@ ${permissionState.status.isGranted}", Toast.LENGTH_SHORT).show()

                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться PDF"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.currentDocument == null -> {
                    Text(
                        text = "Нет выбранного документа",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    uiState.pdfFile?.let { file ->
                        pdfViewer.DisplayPdf(
                            file = file,
                            onPageClick = { page, x, y, bitmap ->
                                println("DEBUG: Клик по странице $page в точке ($x, $y)")
                                println("DEBUG: Существующая подпись: ${bitmap != null}")

                                // Проверяем, есть ли нотация в этой точке
                                val notation = uiState.notations.find { notation ->
                                    notation.page == page &&
                                            kotlin.math.abs(notation.x - x) < 5 && // Допуск в 5%
                                            kotlin.math.abs(notation.y - y) < 5     // Допуск в 5%
                                }

                                if (notation != null) {
                                    // Если есть нотация и подпись - показываем диалог удаления
                                    if (bitmap != null) {
                                        deleteCoordinates = Triple(page, x, y)
                                        showDeleteDialog = true
                                    }
                                    // Если есть нотация, но нет подписи - показываем диалог подписи
                                    else {

                                        println("Selected Notation: ${notation}")
                                        currentNotation = notation


                                        selectedNotationTriple = Triple(page, x, y)
                                        showSignaturePad = true
                                    }
                                }
                                // Если нет нотации - ничего не делаем
                            },
                            notations = uiState.notations.map { notation ->
                                println("DEBUG: Нотация на странице ${notation.page} в точке (${notation.x}, ${notation.y})")
                                PdfNotation(
                                    page = notation.page,
                                    x = notation.x,
                                    y = notation.y,
                                    signatureBitmap = notation.signatureBitmap
                                )
                            },
                            currentPage = currentPage,
                            onPageChanged = { page -> currentPage = page }
                        )
                    }
                }
            }
        }
    }

    if (showSignaturePad) {
        SignaturePadDialog(
            onSignatureComplete = { bitmap ->
                selectedNotationTriple?.let { (page, x, y) ->
                    println("DEBUG: Получена подпись размером: ${bitmap.width}x${bitmap.height}, notation: ${currentNotation}")
                    viewModel.addSignature0(bitmap, page, x, y)
                }
                showSignaturePad = false
                selectedNotationTriple = null
            },
            onDismiss = {
                showSignaturePad = false
                selectedNotationTriple = null
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                deleteCoordinates = null
            },
            title = { Text("Удалить подпись?") },
            text = { Text("Вы действительно хотите удалить подпись?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCoordinates?.let { (page, x, y) ->
                            viewModel.removeSignature(page, x, y)
                        }
                        showDeleteDialog = false
                        deleteCoordinates = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        deleteCoordinates = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Требуется разрешение") },
            text = { Text("Чтобы сохранить PDF, предоставьте доступ к хранилищу в настройках.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    openAppSettings(context)
                }) {
                    Text("Открыть настройки")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

@OptIn(ExperimentalPermissionsApi::class)
fun PermissionStatus.isPermanentlyDenied(): Boolean =
    this is PermissionStatus.Denied && !this.shouldShowRationale