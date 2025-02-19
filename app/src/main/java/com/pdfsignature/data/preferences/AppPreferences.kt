package com.pdfsignature.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferences(private val context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        val IS_ADVANCED_SIGNATURE = booleanPreferencesKey("is_advanced_signature")
        val SIGNATURE_COLOR = stringPreferencesKey("signature_color")
        val SIGNATURE_STROKE_WIDTH = stringPreferencesKey("signature_stroke_width")
        val MARKER_TYPE = stringPreferencesKey("marker_type")
    }

    val isDarkTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_DARK_THEME] ?: false
    }

    val isAdvancedSignature: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_ADVANCED_SIGNATURE] ?: false
    }

    val signatureColor: Flow<String> = dataStore.data.map { preferences ->
        preferences[SIGNATURE_COLOR] ?: "BLACK"
    }

    val signatureStrokeWidth: Flow<String> = dataStore.data.map { preferences ->
        preferences[SIGNATURE_STROKE_WIDTH] ?: "MEDIUM"
    }

    val markerType: Flow<String> = dataStore.data.map { preferences ->
        preferences[MARKER_TYPE] ?: "RECTANGLE"
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DARK_THEME] = isDark
        }
    }

    suspend fun setAdvancedSignature(isAdvanced: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_ADVANCED_SIGNATURE] = isAdvanced
        }
    }

    suspend fun setSignatureColor(color: String) {
        dataStore.edit { preferences ->
            preferences[SIGNATURE_COLOR] = color
        }
    }

    suspend fun setSignatureStrokeWidth(width: String) {
        dataStore.edit { preferences ->
            preferences[SIGNATURE_STROKE_WIDTH] = width
        }
    }

    suspend fun setMarkerType(type: String) {
        dataStore.edit { preferences ->
            preferences[MARKER_TYPE] = type
        }
    }
} 