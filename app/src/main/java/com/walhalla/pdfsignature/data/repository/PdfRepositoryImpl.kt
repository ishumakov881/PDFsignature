package com.walhalla.pdfsignature.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.walhalla.pdfsignature.core.repository.PdfRepository
import com.walhalla.pdfsignature.core.repository.PdfDocument
import com.walhalla.pdfsignature.core.repository.SignatureNotation
import com.walhalla.pdfsignature.data.db.dao.PdfDocumentDao
import com.walhalla.pdfsignature.data.db.dao.SignatureNotationDao
import com.walhalla.pdfsignature.data.db.entity.PdfDocumentEntity
import com.walhalla.pdfsignature.data.db.entity.SignatureNotationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PdfRepositoryImpl(
    private val context: Context,
    private val pdfDocumentDao: PdfDocumentDao,
    private val signatureNotationDao: SignatureNotationDao
) : PdfRepository {

    private var isInitialized = false
    private val signatureBitmaps = mutableMapOf<String, Bitmap>()

    init {
        PDFBoxResourceLoader.init(context)
    }

    private suspend fun initializeData() {
        if (!isInitialized) {
            val documents = pdfDocumentDao.getAllDocuments().first()
            if (documents.isEmpty()) {
                val sampleFile = getPdfFromAssets("sample.pdf")
                val document = PdfDocumentEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Пример документа",
                    path = sampleFile.path,
                    addedDate = System.currentTimeMillis()
                )
                pdfDocumentDao.insertDocument(document)
            }
            isInitialized = true
        }
    }

    override suspend fun getPdfFromAssets(fileName: String): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, fileName)
        if (!file.exists()) {
            context.assets.open(fileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }
        file
    }

    override suspend fun getPdfFromLocal(uri: String): File = withContext(Dispatchers.IO) {
        File(uri)
    }

    override suspend fun getPdfFromRemote(url: String): File {
        TODO("Not yet implemented - будет реализовано с Ktor")
    }

    override suspend fun savePdfWithSignature(
        file: File,
        signature: Bitmap,
        page: Int,
        x: Float,
        y: Float
    ): File = withContext(Dispatchers.IO) {
        println("DEBUG: PdfRepositoryImpl: Сохраняем подпись для файла ${file.name}")
        println("DEBUG: PdfRepositoryImpl: Страница $page, координаты ($x%, $y%)")
        println("DEBUG: PdfRepositoryImpl: Размер подписи ${signature.width}x${signature.height}")
        
        // Получаем текущий документ
        val document = pdfDocumentDao.getAllDocuments().first().firstOrNull()
        if (document != null) {
            println("DEBUG: PdfRepositoryImpl: Документ найден, ID: ${document.id}")
            val signatureKey = "${document.id}_${page}_${x}_${y}"
            println("DEBUG: PdfRepositoryImpl: Ключ для подписи: $signatureKey")
            signatureBitmaps[signatureKey] = signature
            
            // Сохраняем нотацию
            saveSignatureNotation(document.id, page, x, y)
            println("DEBUG: PdfRepositoryImpl: Нотация сохранена")
        } else {
            println("DEBUG: PdfRepositoryImpl: Документ не найден!")
        }
        
        file
    }

    override suspend fun getAllDocuments(): Flow<List<PdfDocument>> {
        initializeData()
        return pdfDocumentDao.getAllDocuments()
            .map { entities ->
                entities.map { it.toDomain() }
            }
    }

    override suspend fun saveSignatureNotation(documentId: String, page: Int, x: Float, y: Float) {
        println("DEBUG: PdfRepositoryImpl: Сохраняем нотацию")
        println("DEBUG: PdfRepositoryImpl: documentId=$documentId, page=$page, x=$x, y=$y")
        val notation = SignatureNotationEntity(
            documentId = documentId,
            page = page,
            xPercent = x,
            yPercent = y,
            widthPercent = 20f,
            heightPercent = 10f
        )
        println("DEBUG: PdfRepositoryImpl: Создана нотация: $notation")
        signatureNotationDao.insertNotation(notation)
        pdfDocumentDao.updateHasNotations(documentId, true)
        println("DEBUG: PdfRepositoryImpl: Нотация сохранена в базу данных")
    }

    override suspend fun getSignatureNotations(documentId: String): Flow<List<SignatureNotation>> {
        println("DEBUG: PdfRepositoryImpl: Получаем нотации для документа $documentId")
        return signatureNotationDao.getNotationsForDocument(documentId)
            .map { entities ->
                entities.map { entity ->
                    val signatureKey = "${documentId}_${entity.page}_${entity.xPercent}_${entity.yPercent}"
                    println("DEBUG: PdfRepositoryImpl: Ищем подпись по ключу: $signatureKey")
                    val bitmap = signatureBitmaps[signatureKey]
                    println("DEBUG: PdfRepositoryImpl: Подпись ${if (bitmap != null) "найдена" else "не найдена"}")
                    SignatureNotation(
                        documentId = entity.documentId,
                        page = entity.page,
                        x = entity.xPercent,
                        y = entity.yPercent,
                        signatureBitmap = bitmap
                    )
                }
            }
    }

    override suspend fun removeSignatureFromPdf(
        file: File,
        page: Int,
        x: Float,
        y: Float
    ): File = withContext(Dispatchers.IO) {
        val signatureKey = "${file.name}_${page}_${x}_${y}"
        signatureBitmaps.remove(signatureKey)
        file
    }

    override suspend fun saveDocument(document: PdfDocument) {
        val entity = PdfDocumentEntity(
            id = document.id,
            title = document.title,
            path = document.path,
            addedDate = document.addedDate,
            hasNotations = false
        )
        pdfDocumentDao.insertDocument(entity)
    }

    private fun PdfDocumentEntity.toDomain() = PdfDocument(
        id = id,
        title = title,
        addedDate = addedDate,
        path = path
    )

    private fun SignatureNotationEntity.toDomain() = SignatureNotation(
        documentId = documentId,
        page = page,
        x = xPercent,
        y = yPercent,
        signatureBitmap = signatureBitmaps["${documentId}_${page}_${xPercent}_${yPercent}"]
    )
} 