package com.pdfsignature.di

import com.pdfsignature.data.preferences.AppPreferences
import com.pdfsignature.ui.viewmodels.*
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { AppPreferences(androidContext()) }

    // ViewModels
    factory { CurrentDocumentViewModel(get()) }
    factory { SignedDocumentsViewModel(get()) }
    factory { DocumentListViewModel(get()) }
    factory { NotationEditorViewModel(get()) }
    factory { SettingsViewModel(get()) }
    factory { HistoryPdfViewerViewModel(get()) }
} 