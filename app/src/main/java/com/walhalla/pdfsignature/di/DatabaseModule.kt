package com.walhalla.pdfsignature.di

import androidx.room.Room
import com.walhalla.pdfsignature.data.db.PdfDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            PdfDatabase::class.java,
            "pdf_signature.db"
        ).build()
    }
    
    single { get<PdfDatabase>().pdfDocumentDao() }
    single { get<PdfDatabase>().signatureNotationDao() }
} 