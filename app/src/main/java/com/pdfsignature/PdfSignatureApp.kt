package com.pdfsignature

import android.app.Application
import android.content.Context
import com.pdfsignature.di.appModule

import com.pdfsignature.core.repository.PdfRepository
import com.pdfsignature.di.databaseModule
import com.pdfsignature.core.viewer.PdfViewer
import com.pdfsignature.data.preferences.AppPreferences
import com.walhalla.pdfsignature.data.repository.PdfRepositoryImpl
import com.pdfsignature.ui.components.PdfViewerImpl
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

class PdfSignatureApp : Application() {
    override fun onCreate() {
        super.onCreate()


        val x = module{
            single<Context> { applicationContext }
            single { AppPreferences(get()) }
            single<PdfRepository> { PdfRepositoryImpl(get(), get(), get()) }
            single<PdfViewer> { PdfViewerImpl() }
        }

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@PdfSignatureApp)
            modules(listOf(x, appModule, databaseModule))
        }
    }
} 