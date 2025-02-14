package com.walhalla.pdfsignature

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.gcacace.signaturepad.views.SignaturePad

//@Composable
//fun SignatureCanvas(onSave: (Bitmap) -> Unit) {
//    var path by remember { mutableStateOf(listOf<Offset>()) }
//    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
//        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
//            detectDragGestures(onDrag = { change, _ ->
//                path = path + change.position
//            })
//        }) {
//            for (i in 1 until path.size) {
//                drawLine(
//                    color = androidx.compose.ui.graphics.Color.Black,
//                    start = path[i - 1],
//                    end = path[i],
//                    strokeWidth = 5f
//                )
//            }
//        }
//        Button(onClick = { saveSignature(path, onSave) }, modifier = Modifier.align(Alignment.BottomCenter)) {
//            Text("Save Signature")
//        }
//    }
//}
@Composable
fun SignatureCanvas(onSaveBitmap: (Bitmap) -> Unit) {
    val context = LocalContext.current
    var signaturePad: SignaturePad? by remember { mutableStateOf(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {

        // Embed SignaturePad in Composable using AndroidView
        AndroidView(
            factory = {
                SignaturePad(context, null).apply {
                    signaturePad = this
                    setOnSignedListener(object : SignaturePad.OnSignedListener {
                        override fun onStartSigning() {}
                        override fun onSigned() {}
                        override fun onClear() {}
                    })
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )

        Button(
            onClick = {
                signaturePad?.getSignatureBitmap()?.let {
                    Toast.makeText(context, "@@@@", Toast.LENGTH_SHORT).show()
                    onSaveBitmap(it)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Text("Save Signature...")
        }
    }
}

//@Preview
//@Composable
//fun SignatureCanvasPreview() {
//    SignatureCanvas(onSave = { bitmap -> /* Handle saved bitmap */ })
//}