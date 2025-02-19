package com.pdfsignature.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdfsignature.data.preferences.AppPreferences
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferences: AppPreferences
) : ViewModel() {
    
    val isDarkTheme = preferences.isDarkTheme
    val isAdvancedSignature = preferences.isAdvancedSignature
    val signatureColor = preferences.signatureColor
    val signatureStrokeWidth = preferences.signatureStrokeWidth
    val markerType = preferences.markerType

    fun setDarkTheme(isDark: Boolean) {
        viewModelScope.launch {
            preferences.setDarkTheme(isDark)
        }
    }

    fun setAdvancedSignature(isAdvanced: Boolean) {
        viewModelScope.launch {
            preferences.setAdvancedSignature(isAdvanced)
        }
    }

    fun setSignatureColor(color: String) {
        viewModelScope.launch {
            preferences.setSignatureColor(color)
        }
    }

    fun setSignatureStrokeWidth(width: String) {
        viewModelScope.launch {
            preferences.setSignatureStrokeWidth(width)
        }
    }

    fun setMarkerType(type: String) {
        viewModelScope.launch {
            preferences.setMarkerType(type)
        }
    }
} 