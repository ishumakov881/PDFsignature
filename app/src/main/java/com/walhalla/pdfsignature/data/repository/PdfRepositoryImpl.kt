package com.walhalla.pdfsignature.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory
import com.pdfsignature.core.repository.PdfRepository
import com.pdfsignature.core.repository.PdfDocument
import com.pdfsignature.core.repository.SignatureNotation
import com.pdfsignature.data.db.dao.PdfDocumentDao
import com.pdfsignature.data.db.dao.SignatureNotationDao
import com.pdfsignature.data.db.entity.PdfDocumentEntity
import com.pdfsignature.data.db.entity.SignatureNotationEntity


import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.math.abs

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


    suspend fun savePdfWithSignature1(
        file: File,
        signature: Bitmap,
        notation: SignatureNotation
    ): File = withContext(Dispatchers.IO) {
        println("DEBUG: PdfRepositoryImpl: Сохраняем подпись для файла ${file.name}")
        println("DEBUG: PdfRepositoryImpl: Размер подписи ${signature.width}x${signature.height}")

        val signatureKey = "${notation.documentId}_${notation.page}_${notation.x}_${notation.y}"
        println("DEBUG: PdfRepositoryImpl: Ключ для подписи: $signatureKey")
        signatureBitmaps[signatureKey] = signature

        //@@@@@@@@@@@@@@
        //@@@@@@@@@@@@@@
        // Сохраняем нотацию
        val notation = SignatureNotationEntity(
            documentId = notation.documentId, page = notation.page,
            xPercent = notation.x,
            yPercent = notation.y,
            widthPercent = 20f, heightPercent = 10f
        )
        val inserted = signatureNotationDao.insertNotation(notation)
        notation.id = inserted
        println("DEBUG: PdfRepositoryImpl: Создана нотация: ${notation}")
        //pdfDocumentDao.updateHasNotations(documentId, true)

        println("DEBUG: PdfRepositoryImpl: Нотация сохранена в базу данных")
        println("DEBUG: PdfRepositoryImpl: Нотация сохранена")
        file
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


            //@@@@@@@@@@@@@@
            //@@@@@@@@@@@@@@
            //@@@@@@@@@@@@@@
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
            documentId = documentId, page = page,
            xPercent = x,
            yPercent = y,
            widthPercent = 20f, heightPercent = 10f
        )

        val inserted = signatureNotationDao.insertNotation(notation)
        notation.id = inserted
        println("DEBUG: PdfRepositoryImpl: Создана нотация: ${notation.id}")

        pdfDocumentDao.updateHasNotations(documentId, true)
        println("DEBUG: PdfRepositoryImpl: Нотация сохранена в базу данных")
    }

    override suspend fun getSignatureNotations(documentId: String): Flow<List<SignatureNotation>> {
        println("DEBUG: PdfRepositoryImpl: Получаем нотации для документа $documentId")
        return signatureNotationDao.getNotationsForDocument(documentId)
            .map { entities ->
                entities.map { entity ->
                    val signatureKey =
                        "${documentId}_${entity.page}_${entity.xPercent}_${entity.yPercent}"
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

    override suspend fun updateNotationPosition(
        documentId: String,
        page: Int,
        oldX: Float,
        oldY: Float,
        newX: Float,
        newY: Float
    ) {
        println("DEBUG: PdfRepositoryImpl: Обновляем позицию нотации")
        println("DEBUG: PdfRepositoryImpl: documentId=$documentId, page=$page")
        println("DEBUG: PdfRepositoryImpl: Старые координаты: ($oldX, $oldY)")
        println("DEBUG: PdfRepositoryImpl: Новые координаты: ($newX, $newY)")

        // Получаем все нотации для документа
        val notations = signatureNotationDao.getNotationsForDocument(documentId).first()

        // Находим нужную нотацию по старым координатам
        val notation = notations.find {
            it.page == page &&
                    abs(it.xPercent - oldX) < 0.1f &&
                    abs(it.yPercent - oldY) < 0.1f
        }

        if (notation != null) {
            println("DEBUG: PdfRepositoryImpl: Нотация найдена, id=${notation.id}")
            // Обновляем координаты
            val updatedNotation = notation.copy(
                xPercent = newX,
                yPercent = newY
            )
            signatureNotationDao.updateNotation(updatedNotation)
            println("DEBUG: PdfRepositoryImpl: Нотация обновлена")

            // Если есть подпись, обновляем её ключ в кэше
            val oldKey = "${documentId}_${page}_${oldX}_${oldY}"
            val newKey = "${documentId}_${page}_${newX}_${newY}"
            signatureBitmaps[oldKey]?.let { bitmap ->
                signatureBitmaps[newKey] = bitmap
                signatureBitmaps.remove(oldKey)
                println("DEBUG: PdfRepositoryImpl: Ключ подписи обновлен")
            }
        } else {
            println("DEBUG: PdfRepositoryImpl: Нотация не найдена!")
        }
    }

    override suspend fun deleteNotation(
        documentId: String,
        page: Int,
        x: Float,
        y: Float
    ) {
        println("DEBUG: PdfRepositoryImpl: Удаляем нотацию")
        println("DEBUG: PdfRepositoryImpl: documentId=$documentId, page=$page")
        println("DEBUG: PdfRepositoryImpl: Координаты ($x, $y)")

        // Получаем все нотации для документа
        val notations = signatureNotationDao.getNotationsForDocument(documentId).first()

        // Находим нужную нотацию по координатам
        val notation = notations.find {
            it.page == page &&
                    abs(it.xPercent - x) < 0.1f &&
                    abs(it.yPercent - y) < 0.1f
        }

        if (notation != null) {
            println("DEBUG: PdfRepositoryImpl: Нотация найдена, id=${notation.id}")
            // Удаляем нотацию
            signatureNotationDao.deleteNotation(notation)
            println("DEBUG: PdfRepositoryImpl: Нотация удалена")

            // Удаляем подпись из кэша если она есть
            val key = "${documentId}_${page}_${x}_${y}"
            signatureBitmaps.remove(key)
            println("DEBUG: PdfRepositoryImpl: Подпись удалена из кэша")

            // Проверяем, остались ли еще нотации у документа
            val remainingNotations =
                signatureNotationDao.getNotationsForDocument(documentId).first()
            if (remainingNotations.isEmpty()) {
                pdfDocumentDao.updateHasNotations(documentId, false)
                println("DEBUG: PdfRepositoryImpl: Обновлен статус документа (нет нотаций)")
            }
        } else {
            println("DEBUG: PdfRepositoryImpl: Нотация не найдена!")
        }
    }

    override suspend fun createSignedPdfCopy(
        file: File,
        notations: List<SignatureNotation>
    ): File = withContext(Dispatchers.IO) {
        println("DEBUG: PdfRepositoryImpl: Создаем копию PDF с подписями")

        // Создаем новый файл для подписанного PDF с таймстампом
        val timestamp = System.currentTimeMillis()
        val signedFile = File(context.cacheDir, "signed_${timestamp}_${file.name}")

        // Загружаем исходный PDF
        PDDocument.load(file).use { document ->
            println("DEBUG: PdfRepositoryImpl: Добавляем ${notations.size} подписей")

            // Для каждой нотации с подписью
            notations.filter { it.signatureBitmap != null }.forEach { notation ->
                println("DEBUG: PdfRepositoryImpl: Добавляем подпись на страницу ${notation.page}")

                // Получаем страницу
                val page = document.getPage(notation.page)

                // Создаем поток для рисования
                PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true
                ).use { contentStream ->
                    // Конвертируем подпись в формат PDImageXObject
                    val signatureImage = notation.signatureBitmap?.let { bitmap ->
                        LosslessFactory.createFromImage(document, bitmap)
                    }

                    // Вычисляем позицию подписи
                    signatureImage?.let { image ->
                        val pageWidth = page.mediaBox.width
                        val pageHeight = page.mediaBox.height

                        // Конвертируем проценты в координаты PDF
                        val x = (notation.x / 100f) * pageWidth
                        val y = pageHeight - ((notation.y / 100f) * pageHeight) // Инвертируем Y

                        // Рисуем подпись
                        contentStream.drawImage(
                            image,
                            x - (image.width / 2f),
                            y - (image.height / 2f)
                        )
                        println("DEBUG: PdfRepositoryImpl: Подпись добавлена в позицию ($x, $y)")
                    }
                }
            }

            // Сохраняем PDF с подписями
            document.save(signedFile)
            println("DEBUG: PdfRepositoryImpl: PDF сохранен: ${signedFile.path}")
        }

        signedFile
    }

    override suspend fun getSignedDocuments(): Flow<List<PdfDocument>> = flow {
        val signedFiles = context.cacheDir.listFiles { file ->
            file.name.startsWith("signed_") && file.name.endsWith(".pdf")
        }?.toList() ?: emptyList()

        val documents = signedFiles.map { file ->
            val nameParts = file.name.split("_", limit = 3)
            val timestamp = nameParts[1].toLongOrNull() ?: file.lastModified()
            val originalName = nameParts.getOrNull(2) ?: file.name

            PdfDocument(
                id = file.name,
                title = file.name,/*originalName*/
                path = file.absolutePath,
                addedDate = timestamp
            )
        }.sortedByDescending { it.addedDate } // Сортируем по времени создания, новые сверху

        emit(documents)
    }.flowOn(Dispatchers.IO)

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