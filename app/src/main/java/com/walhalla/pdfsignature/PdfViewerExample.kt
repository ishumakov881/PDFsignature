//package com.walhalla.pdfsignature
//
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.Button
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//
//@Composable
//fun PdfViewerExample() {
//    val context = LocalContext.current
//
//    Button(onClick = { /* Your action here */ }) {
//        Text(text = "Load PDF")
//    }
//
//    // Отображение PDF через AndroidView
//    androidx.compose.ui.viewinterop.AndroidView(
//        factory = { context ->
//            PDFView(context, null).apply {
//                fromAsset("sample.pdf")
//                    .enableSwipe(true)
//                    .swipeHorizontal(false)
//                    .enableDoubletap(true)
//                    .onPageChange { page, _ ->
//                        // Обработка изменения страницы
//                    }
//                    .load()
//            }
//        },
//        modifier = Modifier.fillMaxSize()
//    )
//}
