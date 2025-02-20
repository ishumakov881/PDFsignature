package com.pdfsignature.ui.screens

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pdfsignature.ui.viewmodels.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val isDarkTheme by viewModel.isDarkTheme.collectAsState(initial = false)
    val isAdvancedSignature by viewModel.isAdvancedSignature.collectAsState(initial = false)
    val currentColor by viewModel.signatureColor.collectAsState(initial = "BLACK")
    val currentStrokeWidth by viewModel.signatureStrokeWidth.collectAsState(initial = "MEDIUM")
    val currentMarkerType by viewModel.markerType.collectAsState(initial = "RECTANGLE")
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Настройки") }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Темная тема
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Темная тема",
                    style = MaterialTheme.typography.bodyLarge
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.setDarkTheme(it) }
                )
            }

            HorizontalDivider()

            // Расширенный режим подписи
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Расширенный режим подписи",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = isAdvancedSignature,
                        onCheckedChange = { viewModel.setAdvancedSignature(it) }
                    )
                }

                if (isAdvancedSignature) {
                    // Выбор цвета
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Цвет подписи",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorButton(
                                "BLACK",
                                Color.Black,
                                currentColor
                            ) { viewModel.setSignatureColor(it) }
                            ColorButton("BLUE", Color.Blue, currentColor) {
                                viewModel.setSignatureColor(
                                    it
                                )
                            }
                            ColorButton(
                                "RED",
                                Color.Red,
                                currentColor
                            ) { viewModel.setSignatureColor(it) }
                            ColorButton(
                                "GREEN",
                                Color.Green,
                                currentColor
                            ) { viewModel.setSignatureColor(it) }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Выбор толщины
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Толщина линии",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StrokeButton(
                                "THIN",
                                "Тонкая",
                                currentStrokeWidth
                            ) { viewModel.setSignatureStrokeWidth(it) }
                            StrokeButton(
                                "MEDIUM",
                                "Средняя",
                                currentStrokeWidth
                            ) { viewModel.setSignatureStrokeWidth(it) }
                            StrokeButton(
                                "THICK",
                                "Толстая",
                                currentStrokeWidth
                            ) { viewModel.setSignatureStrokeWidth(it) }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Выбор типа маркера
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Тип маркера",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MarkerButton(
                                "RECTANGLE",
                                "Прямоугольник",
                                currentMarkerType
                            ) { viewModel.setMarkerType(it) }
                            MarkerButton(
                                "DOT",
                                "Точка",
                                currentMarkerType
                            ) { viewModel.setMarkerType(it) }
                            MarkerButton(
                                "CROSS",
                                "Крестик",
                                currentMarkerType
                            ) { viewModel.setMarkerType(it) }
                        }
                    }
                }
            }

            Divider()

            // Информация о версиях
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "О приложении",
                    style = MaterialTheme.typography.titleMedium
                )
                
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                Text(
                    text = "Версия приложения: ${packageInfo.versionName} (${packageInfo.versionCode})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Устройство: ${Build.MANUFACTURER} ${Build.MODEL}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorButton(
    colorName: String,
    color: Color,
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 2.dp,
                color = if (selectedColor == colorName) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onColorSelected(colorName) }
    )
}

@Composable
private fun StrokeButton(
    width: String,
    label: String,
    selectedWidth: String,
    onWidthSelected: (String) -> Unit
) {
    Button(
        onClick = { onWidthSelected(width) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedWidth == width)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = label,
            color = if (selectedWidth == width)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MarkerButton(
    type: String,
    label: String,
    selectedType: String,
    onTypeSelected: (String) -> Unit
) {
    Button(
        onClick = { onTypeSelected(type) },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selectedType == type)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = label,
            color = if (selectedType == type)
                MaterialTheme.colorScheme.onPrimary
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
} 