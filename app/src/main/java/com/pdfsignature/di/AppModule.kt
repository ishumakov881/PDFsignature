package com.pdfsignature.di

import com.walhalla.pdfsignature.ui.viewmodels.CurrentDocumentViewModel
import com.walhalla.pdfsignature.ui.viewmodels.DocumentListViewModel
import com.walhalla.pdfsignature.ui.viewmodels.NotationEditorViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {


    // ViewModels
    factory { CurrentDocumentViewModel(get()) }
    factory { DocumentListViewModel(get()) }
    factory { NotationEditorViewModel(get()) }
} 