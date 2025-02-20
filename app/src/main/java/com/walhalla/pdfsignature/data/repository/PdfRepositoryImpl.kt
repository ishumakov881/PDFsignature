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
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import android.provider.MediaStore
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.net.Uri
import android.content.ContentValues
import android.net.Uri as AndroidUri
import android.content.ContentUris
import android.os.Build
import android.os.Environment
import android.content.Intent

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
//            if (documents.isEmpty()) {
//                val sampleFile = getPdfFromAssets("sample.pdf")
//                val document = PdfDocumentEntity(
//                    id = UUID.randomUUID().toString(),
//                    title = "Пример документа",
//                    path = sampleFile.path,
//                    addedDate = System.currentTimeMillis()
//                )
//                pdfDocumentDao.insertDocument(document)
//            }
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
        try {
            val docsDir = File(context.filesDir, "documents").apply {
                if (!exists()) mkdirs()
            }

            // Генерируем уникальное имя файла
            val fileName = "doc_${System.currentTimeMillis()}.pdf"
            val destinationFile = File(docsDir, fileName)

            // Копируем содержимое из URI в файл
            context.contentResolver.openInputStream(AndroidUri.parse(uri))?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Не удалось открыть файл")

            destinationFile
        } catch (e: Exception) {
            throw Exception("Ошибка при сохранении файла: ${e.message}")
        }
    }

    override suspend fun getPdfFromRemote(url: String): File {
        TODO("Not yet implemented - будет реализовано с Ktor")
    }


    suspend fun savePdfWithSignature1(
        file: File, signature: Bitmap, notation: SignatureNotation
    ): File = withContext(Dispatchers.IO) {
        println("DEBUG: PdfRepositoryImpl: Сохраняем подпись для файла ${file.name}")
        println("DEBUG: PdfRepositoryImpl: Размер подписи ${signature.width}x${signature.height}")

        val signatureKey = "${notation.documentId}_${notation.page}_${notation.x}_${notation.y}"
        println("DEBUG: PdfRepositoryImpl: Ключ для подписи: $signatureKey")
        signatureBitmaps[signatureKey] = signature

        //@@@@@@@@@@@@@@
        //@@@@@@@@@@@@@@
        // Сохраняем нотацию
//        val notation = SignatureNotationEntity(
//            documentId = notation.documentId, page = notation.page,
//            xPercent = notation.x,
//            yPercent = notation.y,
//            widthPercent = 20f, heightPercent = 10f
//        )
//        val inserted = signatureNotationDao.insertNotation(notation)
//        notation.id = inserted
//        println("xxx DEBUG: PdfRepositoryImpl: Создана нотация: ${notation}")
        pdfDocumentDao.updateHasNotations(notation.documentId, true)/*UI Trigger*/
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

    override suspend fun removeSignatureFromPdf(file: File, page: Int, x: Float, y: Float): File =
        withContext(Dispatchers.IO) {
            val signatureKey = "${file.name}_${page}_${x}_${y}"
            signatureBitmaps.remove(signatureKey)
            file
        }

    override suspend fun saveDocument(document: PdfDocument) {
        try {
            // Создаем директорию если её нет
            val docsDir = File(context.filesDir, "documents").apply {
                if (!exists()) mkdirs()
            }

            // Генерируем уникальное имя файла
            val fileName = "doc_${System.currentTimeMillis()}.pdf"
            val destinationFile = File(docsDir, fileName)

            // Копируем содержимое из URI в файл
            context.contentResolver.openInputStream(AndroidUri.parse(document.path))?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Не удалось открыть файл")

            // Сохраняем документ с путем к локальной копии
            val entity = PdfDocumentEntity(
                id = document.id,
                title = document.title,
                path = getRelativePath(destinationFile),
                addedDate = document.addedDate,
                hasNotations = false
            )
            pdfDocumentDao.insertDocument(entity)
        } catch (e: Exception) {
            throw Exception("Ошибка при сохранении документа: ${e.message}")
        }
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
            it.page == page && abs(it.xPercent - oldX) < 0.1f && abs(it.yPercent - oldY) < 0.1f
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

    override suspend fun createSignedPdfCopy(file: File, notations: List<SignatureNotation>): File = withContext(Dispatchers.IO) {

        // Создаем временный файл для подписанного PDF
        val timestamp = System.currentTimeMillis()
        val fileName = "signed_${timestamp}_${file.name}"
        val tempSignedFile = File(context.cacheDir, fileName)


        println("DEBUG: PdfRepositoryImpl: Создаем копию PDF с подписями ${file.absolutePath}")
        println("DEBUG: PdfRepositoryImpl: Создаем копию PDF с подписями ${tempSignedFile.absolutePath}")


        PDDocument.load(file).use { document ->
            // Добавляем подписи на страницы
            notations.forEach { notation ->
                notation.signatureBitmap?.let { bitmap ->
                    val page = document.getPage(notation.page)
                    val pageWidth = page.mediaBox.width
                    val pageHeight = page.mediaBox.height

                    // Конвертируем проценты в координаты PDF
                    val x = (notation.x / 100f) * pageWidth
                    val y = pageHeight - ((notation.y / 100f) * pageHeight) // Инвертируем Y

                    PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND,
                        true,
                        true
                    ).use { contentStream ->
                        val image = LosslessFactory.createFromImage(document, bitmap)

                        // Рисуем подпись
                        contentStream.drawImage(image, x - (image.width / 2f), y - (image.height / 2f))
                    }
                }
            }

            // Сохраняем временный файл
            document.save(tempSignedFile)
        }



        try {
            // После сохранения во временный файл
            savePdfToDownloads(context, tempSignedFile, fileName)
        } catch (e: Exception) {
            throw Exception("Ошибка при сохранении файла: ${e.message}")
        }

        tempSignedFile
    }

    private fun savePdfToDownloads(context: Context, tempSignedFile: File, fileName: String) {
        // Сохраняем файл через MediaStore
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                put(MediaStore.MediaColumns.DATA, file.absolutePath) // Для API 28 и ниже
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external") // Для Android ниже API 29
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(collection, contentValues) ?: throw Exception("Не удалось создать файл")


        resolver.openOutputStream(uri)?.use { outputStream ->
            tempSignedFile.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: throw Exception("Не удалось открыть поток для записи")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

//        // Показываем уведомление о сохранении
//        Handler(Looper.getMainLooper()).post {
//            Toast.makeText(context, "PDF сохранен в Downloads: $fileName", Toast.LENGTH_LONG).show()
//        }
    }

    override suspend fun getSignedDocuments(): Flow<List<PdfDocument>> = flow {
        val documents = mutableListOf<PdfDocument>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10 и выше используем MediaStore
            val projection = arrayOf(

                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.DATE_ADDED,
                MediaStore.Downloads._ID //aka MediaStore.MediaColumns._ID
            )

            val selection =
                "${MediaStore.Downloads.DISPLAY_NAME} LIKE ? AND ${MediaStore.Downloads.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("signed_%", "application/pdf")
            val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"


            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                MediaStore.Files.getContentUri("external") // Для Android ниже API 29
            }

            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameColumn)
                    val dateAdded = cursor.getLong(dateColumn) * 1000
                    val id = cursor.getLong(idColumn)

                    val contentUri = ContentUris.withAppendedId(collection, id)

                    documents.add(
                        PdfDocument(
                            id = id.toString(),
                            title = name,
                            path = contentUri.toString(),
                            addedDate = dateAdded
                        )
                    )
                }
            }
        } else {
            // Для более старых версий ищем файлы в Downloads напрямую
            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val files = downloadsDir.listFiles { file ->
                file.name.startsWith("signed_") && file.name.endsWith(".pdf")
            }

            files?.forEach { file ->
                documents.add(
                    PdfDocument(
                        id = file.name /*file.absolutePath*/,
                        title = file.name,
                        path = file.absolutePath,
                        addedDate = file.lastModified()
                    )
                )
            }
        }

        emit(documents.sortedByDescending { it.addedDate })
    }.flowOn(Dispatchers.IO)

    override suspend fun getSignedDocumentFile(fileName: String): File =
        withContext(Dispatchers.IO) {
            val tempFile = File(context.cacheDir, "temp_view_$fileName")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Для Android 10 и выше используем MediaStore
                    val projection = arrayOf(MediaStore.Downloads._ID)
                    //val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
                    val selection = "${MediaStore.Downloads._ID} = ?"

                    val selectionArgs = arrayOf(fileName)

                    context.contentResolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        projection, selection, selectionArgs, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id =
                                cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID))
                            val contentUri = ContentUris.withAppendedId(
                                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                                id
                            )
                            println("[URI] $contentUri")
                            context.contentResolver.openInputStream(contentUri)?.use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                } else {
                    // Для более старых версий копируем файл из Downloads напрямую
                    val downloadsDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val sourceFile = File(downloadsDir, fileName)

                    if (sourceFile.exists()) {
                        sourceFile.inputStream().use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }

                if (!tempFile.exists()) {
                    throw Exception("Файл не найден $fileName")
                }

                tempFile
            } catch (e: Exception) {
                throw Exception("Ошибка при получении файла: ${e.message}")
            }
        }

    private fun PdfDocumentEntity.toDomain() = PdfDocument(
        id = id,
        title = title,
        addedDate = addedDate,
        path = File(context.filesDir, path).absolutePath
    )

    private fun SignatureNotationEntity.toDomain() = SignatureNotation(
        documentId = documentId,
        page = page,
        x = xPercent,
        y = yPercent,
        signatureBitmap = signatureBitmaps["${documentId}_${page}_${xPercent}_${yPercent}"]
    )

    override suspend fun resetSignatures(documentId: String) {
        // Очищаем все битмапы для данного документа из кэша
        signatureBitmaps.entries.removeIf { (key, _) ->
            key.startsWith("${documentId}_")
        }
    }

    override suspend fun deleteDocument0(document: PdfDocument): Unit = withContext(Dispatchers.IO) {
        withContext(Dispatchers.IO) {
            val entity = PdfDocumentEntity(
                id = document.id,
                title = document.title,
                path = "",
                addedDate = document.addedDate,
                hasNotations = false
            )
            pdfDocumentDao.deleteDocument(entity)
        }
    }

    override suspend fun deleteSignedDocument(documentId: String): Unit =
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Для Android 10 и выше используем MediaStore
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        documentId.toLong()
                    )
                    val rowsDeleted = context.contentResolver.delete(uri, null, null)
                    if (rowsDeleted <= 0) {
                        throw Exception("Не удалось удалить файл через MediaStore")
                    }
                } else {
                    // Для более старых версий удаляем файл напрямую
                    val file = File(documentId) // В старых версиях ID это путь к файлу
                    if (!file.exists() || !file.delete()) {
                        throw Exception("Не удалось удалить файл")
                    }
                }

                // Показываем уведомление об успешном удалении
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Документ удален", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                throw Exception("Ошибка при удалении документа: ${e.message}")
            }
        }

    // Новый метод для получения относительного пути
    private fun getRelativePath(file: File): String {
        return file.absolutePath.substringAfter(context.filesDir.absolutePath + "/")
    }

    private fun getFileProviderUri(file: File): android.net.Uri {
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }
} 