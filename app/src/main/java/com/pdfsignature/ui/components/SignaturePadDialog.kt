package com.pdfsignature.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.github.gcacace.signaturepad.views.SignaturePad
import com.pdfsignature.data.preferences.AppPreferences
import org.koin.compose.koinInject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Composable
fun SignaturePadDialog(
    existingSignature: Bitmap? = null,
    onSignatureComplete: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    preferences: AppPreferences = koinInject()
) {
    var signaturePad by remember { mutableStateOf<SignaturePad?>(null) }
    var hasSignature by remember { mutableStateOf(existingSignature != null) }
    val isAdvancedSignature by preferences.isAdvancedSignature.collectAsState(initial = false)
    val currentColor by preferences.signatureColor.collectAsState(initial = "BLACK")
    val currentStrokeWidth by preferences.signatureStrokeWidth.collectAsState(initial = "MEDIUM")
    
    // Конвертируем настройки в реальные значения
    val penColor = when (currentColor) {
        "BLACK" -> Color.BLACK
        "BLUE" -> Color.BLUE
        "RED" -> Color.RED
        "GREEN" -> Color.GREEN
        else -> Color.BLACK
    }
    
    val strokeWidth = when (currentStrokeWidth) {
        "THIN" -> 2f
        "MEDIUM" -> 5f
        "THICK" -> 8f
        else -> 5f
    }
    
    // Обновляем настройки подписи при их изменении
    LaunchedEffect(currentColor, currentStrokeWidth, signaturePad) {
        signaturePad?.let { pad ->
            pad.setPenColor(penColor)
            pad.setMinWidth(strokeWidth)
            pad.setMaxWidth(strokeWidth * 2)
        }
    }
    
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
                
                if (isAdvancedSignature) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ColorCircle("BLACK", androidx.compose.ui.graphics.Color.Black, currentColor) { 
                            preferences.setSignatureColor(it)
                        }
                        ColorCircle("BLUE", androidx.compose.ui.graphics.Color.Blue, currentColor) { 
                            preferences.setSignatureColor(it)
                        }
                        ColorCircle("RED", androidx.compose.ui.graphics.Color.Red, currentColor) { 
                            preferences.setSignatureColor(it)
                        }
                        ColorCircle("GREEN", androidx.compose.ui.graphics.Color.Green, currentColor) { 
                            preferences.setSignatureColor(it)
                        }
                    }
                }
                
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
                                setPenColor(penColor)
                                setMinWidth(strokeWidth)
                                setMaxWidth(strokeWidth * 2)
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

@Composable
private fun ColorCircle(
    colorName: String,
    color: androidx.compose.ui.graphics.Color,
    selectedColor: String,
    onColorSelected: suspend (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 2.dp,
                color = if (selectedColor == colorName) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .clickable { 
                // Запускаем корутину для вызова suspend функции
                MainScope().launch {
                    onColorSelected(colorName)
                }
            }
    )
} 