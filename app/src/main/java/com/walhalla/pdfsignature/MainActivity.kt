package com.walhalla.pdfsignature

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import com.walhalla.pdfsignature.ui.screen.PdfSignatureScreen1
import com.walhalla.pdfsignature.ui.theme.PDFsignatureTheme
import java.io.File


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PdfUtils.init(this)
        setContent {
            PDFsignatureTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // MainScreen()
                    PdfSignatureScreen1()
                    //PdfViewerExample()
                }
            }
        }
    }
}

fun sharePdf(context: android.content.Context, pdfFile: File) {
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))
}