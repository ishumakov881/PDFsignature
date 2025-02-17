package com.walhalla.pdfsignature.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.github.gcacace.signaturepad.views.SignaturePad

@Composable
fun SignaturePadDialog(
    existingSignature: Bitmap? = null,
    onSignatureComplete: (Bitmap) -> Unit,
    onDismiss: () -> Unit
) {
    var signaturePad by remember { mutableStateOf<SignaturePad?>(null) }
    var hasSignature by remember { mutableStateOf(existingSignature != null) }
    
    // Загружаем существующую подпись
    LaunchedEffect(signaturePad, existingSignature) {
        if (signaturePad != null && existingSignature != null) {
            signaturePad?.signatureBitmap = existingSignature
            hasSignature = true
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (existingSignature != null) "Редактировать подпись" else "Поставьте подпись",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier
                        .width(268.dp)
                        .height(120.dp),
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    AndroidView(
                        factory = { context ->
                            SignaturePad(context, null).apply {
                                signaturePad = this
                                setOnSignedListener(object : SignaturePad.OnSignedListener {
                                    override fun onStartSigning() {
                                        hasSignature = true
                                    }

                                    override fun onSigned() {
                                        hasSignature = true
                                    }

                                    override fun onClear() {
                                        hasSignature = false
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            signaturePad?.clear()
                            hasSignature = false
                        }
                    ) {
                        Text("Очистить")
                    }
                    
                    Row {
                        TextButton(onClick = onDismiss) {
                            Text("Отмена")
                        }
                        
                        Button(
                            onClick = {
                                signaturePad?.transparentSignatureBitmap?.let { bitmap ->
                                    onSignatureComplete(bitmap)
                                }
                            },
                            enabled = hasSignature
                        ) {
                            Text("Готово")
                        }
                    }
                }
            }
        }
    }
} 