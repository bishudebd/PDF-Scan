package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.data.local.DocumentDao
import com.example.data.local.DocumentPageEntity
import com.example.data.local.DocumentWithPages
import com.example.data.local.FolderEntity
import com.example.data.local.FolderWithCount
import com.example.data.local.ScannedDocumentEntity
import com.example.data.model.DraftScanPage
import com.example.data.model.ExportImageFormat
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfQuality
import com.example.data.model.ScanFilter
import com.example.engine.CompressResult
import com.example.engine.FileShareHelper
import com.example.engine.ImageProcessor
import com.example.engine.OcrEngine
import com.example.engine.PdfEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class StorageStats(
    val documentCount: Int,
    val totalPdfBytes: Long,
    val totalCacheBytes: Long
)

class DocumentRepository(
    private val context: Context,
    private val dao: DocumentDao,
    val imageProcessor: ImageProcessor,
    val pdfEngine: PdfEngine,
    val ocrEngine: OcrEngine
) {
    val documentsRootDir: File = File(context.filesDir, "documents").apply { mkdirs() }
    val exportsRootDir: File = File(context.filesDir, "exports").apply { mkdirs() }
    val scanCacheDir: File = File(context.cacheDir, "scans").apply { mkdirs() }

    fun observeAllDocuments(): Flow<List<ScannedDocumentEntity>> = dao.observeAllDocuments()

    fun observeRecentDocuments(limit: Int = 6): Flow<List<ScannedDocumentEntity>> =
        dao.observeRecentDocuments(limit)

    fun searchDocuments(query: String): Flow<List<ScannedDocumentEntity>> =
        if (query.isBlank()) dao.observeAllDocuments() else dao.searchDocuments(query.trim())

    fun observeDocumentsInFolder(folderId: Long): Flow<List<ScannedDocumentEntity>> =
        dao.observeDocumentsInFolder(folderId)

    fun observeDocumentWithPages(documentId: Long): Flow<DocumentWithPages?> =
        dao.observeDocumentWithPages(documentId)

    fun observeFoldersWithCount(): Flow<List<FolderWithCount>> = dao.observeFoldersWithCount()

    fun observeAllFolders(): Flow<List<FolderEntity>> = dao.observeAllFolders()

    suspend fun getDocumentWithPages(documentId: Long): DocumentWithPages? =
        dao.getDocumentWithPages(documentId)

    /**
     * Saves a finalized multi-page scanning session into internal persistent storage,
     * generates the PDF via Android PdfDocument, and inserts document + pages into Room.
     */
    suspend fun saveDraftSessionAsDocument(
        title: String,
        pages: List<DraftScanPage>,
        pageSize: PageSizeOption,
        quality: PdfQuality,
        folderId: Long? = null,
        existingDocumentId: Long? = null
    ): Long? = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) return@withContext null

        val sessionFolder = File(documentsRootDir, "doc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}")
        sessionFolder.mkdirs()

        val savedPageEntities = mutableListOf<DocumentPageEntity>()
        val finalImagePaths = mutableListOf<String>()

        pages.forEachIndexed { idx, draft ->
            val persistedOriginal = File(sessionFolder, "page_${idx + 1}_orig.jpg")
            val persistedEnhanced = File(sessionFolder, "page_${idx + 1}_final.jpg")

            val origFile = File(draft.rawImagePath)
            if (origFile.exists()) {
                origFile.copyTo(persistedOriginal, overwrite = true)
            }

            val finalSource = File(draft.displayPath)
            if (finalSource.exists()) {
                finalSource.copyTo(persistedEnhanced, overwrite = true)
            } else if (persistedOriginal.exists()) {
                persistedOriginal.copyTo(persistedEnhanced, overwrite = true)
            }

            if (persistedEnhanced.exists()) {
                finalImagePaths.add(persistedEnhanced.absolutePath)
                savedPageEntities.add(
                    DocumentPageEntity(
                        documentId = 0L,
                        pageIndex = idx,
                        imagePath = persistedEnhanced.absolutePath,
                        originalImagePath = if (persistedOriginal.exists()) persistedOriginal.absolutePath else persistedEnhanced.absolutePath,
                        filterName = draft.filter.name,
                        rotationDegrees = draft.rotationDegrees
                    )
                )
            }
        }

        if (finalImagePaths.isEmpty()) return@withContext null

        val safeTitle = title.trim().ifEmpty { FileShareHelper.generateDefaultDocTitle() }
        val fileSafeName = safeTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val pdfFile = File(sessionFolder, "${fileSafeName}.pdf")
        val pdfOk = pdfEngine.generatePdfFromImages(finalImagePaths, pdfFile, pageSize, quality)
        if (!pdfOk) return@withContext null

        val thumbFile = File(sessionFolder, "thumbnail.jpg")
        imageProcessor.createThumbnail(finalImagePaths.first(), thumbFile.absolutePath)

        val now = System.currentTimeMillis()
        if (existingDocumentId != null) {
            val existing = dao.getDocumentById(existingDocumentId)
            if (existing != null) {
                val updated = existing.copy(
                    title = safeTitle,
                    pdfPath = pdfFile.absolutePath,
                    thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else finalImagePaths.first(),
                    pageCount = finalImagePaths.size,
                    fileSizeBytes = pdfFile.length(),
                    updatedAt = now
                )
                dao.updateDocument(updated)
                dao.deletePagesForDocument(existingDocumentId)
                dao.insertPages(savedPageEntities.map { it.copy(documentId = existingDocumentId) })
                return@withContext existingDocumentId
            }
        }

        val entity = ScannedDocumentEntity(
            title = safeTitle,
            pdfPath = pdfFile.absolutePath,
            thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else finalImagePaths.first(),
            pageCount = finalImagePaths.size,
            fileSizeBytes = pdfFile.length(),
            folderId = folderId,
            createdAt = now,
            updatedAt = now
        )
        val docId = dao.insertDocument(entity)
        dao.insertPages(savedPageEntities.map { it.copy(documentId = docId) })
        docId
    }

    /**
     * Exports all pages in an active scan session as high-resolution JPG files into [exportsRootDir].
     */
    suspend fun exportDraftSessionAsJpgs(
        title: String,
        pages: List<DraftScanPage>,
        quality: PdfQuality = PdfQuality.HIGH
    ): List<File> = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) return@withContext emptyList()
        val safeBase = title.trim()
            .ifEmpty { FileShareHelper.generateDefaultDocTitle("Scan") }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val outDir = File(exportsRootDir, "jpg_${safeBase}_${System.currentTimeMillis()}").apply { mkdirs() }
        val exported = mutableListOf<File>()
        pages.forEachIndexed { idx, draft ->
            val bmp = imageProcessor.decodeSampledBitmapFromFile(draft.displayPath, quality.maxDimensionPx)
                ?: return@forEachIndexed
            val outFile = File(outDir, "${safeBase}_page_${idx + 1}.jpg")
            imageProcessor.saveBitmap(bmp, outFile, ExportImageFormat.JPG, quality.jpegQuality)
            bmp.recycle()
            if (outFile.exists() && outFile.length() > 0L) {
                exported.add(outFile)
            }
        }
        exported
    }

    /**
     * Imports an external PDF URI (from SAF or ML Kit Document Scanner result) into Room & local storage.
     */
    suspend fun importPdfFileAsDocument(
        sourceFile: File,
        title: String,
        folderId: Long? = null,
        isSigned: Boolean = false
    ): Long? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists()) return@withContext null
        val sessionFolder = File(documentsRootDir, "doc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}")
        sessionFolder.mkdirs()

        val safeTitle = title.trim().ifEmpty { FileShareHelper.generateDefaultDocTitle("PDF") }
        val fileSafeName = safeTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val destPdf = File(sessionFolder, "$fileSafeName.pdf")
        sourceFile.copyTo(destPdf, overwrite = true)

        val pageImages = pdfEngine.exportPdfToImages(
            pdfFile = destPdf,
            outputDir = sessionFolder,
            baseName = "page",
            format = ExportImageFormat.JPG,
            renderWidth = 1400
        )
        if (pageImages.isEmpty()) return@withContext null

        val thumbFile = File(sessionFolder, "thumbnail.jpg")
        imageProcessor.createThumbnail(pageImages.first().absolutePath, thumbFile.absolutePath)

        val now = System.currentTimeMillis()
        val docId = dao.insertDocument(
            ScannedDocumentEntity(
                title = safeTitle,
                pdfPath = destPdf.absolutePath,
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else pageImages.first().absolutePath,
                pageCount = pageImages.size,
                fileSizeBytes = destPdf.length(),
                folderId = folderId,
                isSigned = isSigned,
                createdAt = now,
                updatedAt = now
            )
        )
        dao.insertPages(
            pageImages.mapIndexed { idx, img ->
                DocumentPageEntity(
                    documentId = docId,
                    pageIndex = idx,
                    imagePath = img.absolutePath,
                    originalImagePath = img.absolutePath,
                    filterName = ScanFilter.ORIGINAL.name
                )
            }
        )
        docId
    }

    suspend fun importExternalPdfUri(uri: Uri, displayName: String? = null): Long? = withContext(Dispatchers.IO) {
        val tempPdf = File(scanCacheDir, "import_${System.currentTimeMillis()}.pdf")
        val copied = pdfEngine.copyPdfUriToFile(uri, tempPdf)
        if (!copied) return@withContext null
        val cleanTitle = displayName?.removeSuffix(".pdf")?.trim()
            ?.ifEmpty { FileShareHelper.generateDefaultDocTitle("Imported") }
            ?: FileShareHelper.generateDefaultDocTitle("Imported")
        val id = importPdfFileAsDocument(tempPdf, cleanTitle)
        tempPdf.delete()
        id
    }

    /**
     * Merges multiple existing saved documents into one new document in Room.
     */
    suspend fun mergeSavedDocuments(documentIds: List<Long>, outputTitle: String): Long? =
        withContext(Dispatchers.IO) {
            val files = documentIds.mapNotNull { id ->
                dao.getDocumentById(id)?.pdfPath?.let { File(it) }?.takeIf { it.exists() }
            }
            if (files.size < 2) return@withContext null

            val sessionFolder = File(documentsRootDir, "merged_${System.currentTimeMillis()}")
            sessionFolder.mkdirs()
            val safeTitle = outputTitle.trim().ifEmpty { FileShareHelper.generateDefaultDocTitle("Merged") }
            val pdfFile = File(sessionFolder, "${safeTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.pdf")

            val pageImages = pdfEngine.mergePdfs(files, pdfFile, sessionFolder)
            if (pageImages.isEmpty()) return@withContext null

            val thumbFile = File(sessionFolder, "thumbnail.jpg")
            imageProcessor.createThumbnail(pageImages.first().absolutePath, thumbFile.absolutePath)

            val now = System.currentTimeMillis()
            val docId = dao.insertDocument(
                ScannedDocumentEntity(
                    title = safeTitle,
                    pdfPath = pdfFile.absolutePath,
                    thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else pageImages.first().absolutePath,
                    pageCount = pageImages.size,
                    fileSizeBytes = pdfFile.length(),
                    createdAt = now,
                    updatedAt = now
                )
            )
            dao.insertPages(
                pageImages.mapIndexed { idx, img ->
                    DocumentPageEntity(
                        documentId = docId,
                        pageIndex = idx,
                        imagePath = img.absolutePath,
                        originalImagePath = img.absolutePath,
                        filterName = ScanFilter.ORIGINAL.name
                    )
                }
            )
            docId
        }

    /**
     * Splits a saved document by extracting chosen 0-based page indices into a new PDF document.
     */
    suspend fun splitSavedDocument(
        documentId: Long,
        selectedPageIndices: List<Int>,
        outputTitle: String
    ): Long? = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(documentId) ?: return@withContext null
        val srcPdf = File(doc.pdfPath)
        if (!srcPdf.exists()) return@withContext null

        val sessionFolder = File(documentsRootDir, "split_${System.currentTimeMillis()}")
        sessionFolder.mkdirs()
        val safeTitle = outputTitle.trim().ifEmpty { "${doc.title}_Split" }
        val pdfFile = File(sessionFolder, "${safeTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.pdf")

        val pageImages = pdfEngine.splitPdf(srcPdf, selectedPageIndices, pdfFile, sessionFolder)
        if (pageImages.isEmpty()) return@withContext null

        val thumbFile = File(sessionFolder, "thumbnail.jpg")
        imageProcessor.createThumbnail(pageImages.first().absolutePath, thumbFile.absolutePath)

        val now = System.currentTimeMillis()
        val newId = dao.insertDocument(
            ScannedDocumentEntity(
                title = safeTitle,
                pdfPath = pdfFile.absolutePath,
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else pageImages.first().absolutePath,
                pageCount = pageImages.size,
                fileSizeBytes = pdfFile.length(),
                folderId = doc.folderId,
                createdAt = now,
                updatedAt = now
            )
        )
        dao.insertPages(
            pageImages.mapIndexed { idx, img ->
                DocumentPageEntity(
                    documentId = newId,
                    pageIndex = idx,
                    imagePath = img.absolutePath,
                    originalImagePath = img.absolutePath,
                    filterName = ScanFilter.ORIGINAL.name
                )
            }
        )
        newId
    }

    /**
     * Compresses a saved document and saves the compressed version as a new Room document.
     */
    suspend fun compressSavedDocument(
        documentId: Long,
        quality: PdfQuality
    ): Pair<Long, CompressResult>? = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(documentId) ?: return@withContext null
        val srcPdf = File(doc.pdfPath)
        if (!srcPdf.exists()) return@withContext null

        val sessionFolder = File(documentsRootDir, "compressed_${System.currentTimeMillis()}")
        sessionFolder.mkdirs()
        val safeTitle = "${doc.title} (Compressed)"
        val outPdf = File(sessionFolder, "${safeTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.pdf")

        val (result, pageImages) = pdfEngine.compressPdf(srcPdf, outPdf, sessionFolder, quality)
            ?: return@withContext null

        val thumbFile = File(sessionFolder, "thumbnail.jpg")
        imageProcessor.createThumbnail(pageImages.first().absolutePath, thumbFile.absolutePath)

        val now = System.currentTimeMillis()
        val newId = dao.insertDocument(
            ScannedDocumentEntity(
                title = safeTitle,
                pdfPath = outPdf.absolutePath,
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else pageImages.first().absolutePath,
                pageCount = pageImages.size,
                fileSizeBytes = outPdf.length(),
                folderId = doc.folderId,
                createdAt = now,
                updatedAt = now
            )
        )
        dao.insertPages(
            pageImages.mapIndexed { idx, img ->
                DocumentPageEntity(
                    documentId = newId,
                    pageIndex = idx,
                    imagePath = img.absolutePath,
                    originalImagePath = img.absolutePath,
                    filterName = ScanFilter.ORIGINAL.name
                )
            }
        )
        newId to result
    }

    /**
     * Signs a specific page of a saved PDF document by compositing the signature bitmap
     * and regenerating the full PDF as a new signed document.
     */
    suspend fun signSavedDocument(
        documentId: Long,
        pageIndex: Int,
        signatureBitmap: Bitmap,
        centerXNorm: Float,
        centerYNorm: Float,
        widthNorm: Float
    ): Long? = withContext(Dispatchers.IO) {
        val docWithPages = dao.getDocumentWithPages(documentId) ?: return@withContext null
        val doc = docWithPages.document
        val srcPdf = File(doc.pdfPath)
        if (!srcPdf.exists()) return@withContext null

        val sessionFolder = File(documentsRootDir, "signed_${System.currentTimeMillis()}")
        sessionFolder.mkdirs()

        val renderedPages = pdfEngine.exportPdfToImages(
            pdfFile = srcPdf,
            outputDir = sessionFolder,
            baseName = "signed_page",
            format = ExportImageFormat.JPG,
            renderWidth = 1650
        ).toMutableList()
        if (renderedPages.isEmpty()) return@withContext null

        val safeIndex = pageIndex.coerceIn(0, renderedPages.lastIndex)
        val targetFile = renderedPages[safeIndex]
        val pageBmp = imageProcessor.decodeSampledBitmapFromFile(targetFile.absolutePath, 2000)
            ?: return@withContext null
        val signedBmp = imageProcessor.overlaySignatureOnBitmap(
            pageBitmap = pageBmp,
            signatureBitmap = signatureBitmap,
            centerXNorm = centerXNorm,
            centerYNorm = centerYNorm,
            widthNorm = widthNorm
        )
        pageBmp.recycle()
        imageProcessor.saveBitmap(signedBmp, targetFile, ExportImageFormat.JPG, 92)
        signedBmp.recycle()

        val safeTitle = "${doc.title} (Signed)"
        val outPdf = File(sessionFolder, "${safeTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.pdf")
        val ok = pdfEngine.generatePdfFromImages(
            imagePaths = renderedPages.map { it.absolutePath },
            outputPdfFile = outPdf,
            pageSize = PageSizeOption.A4,
            quality = PdfQuality.HIGH
        )
        if (!ok) return@withContext null

        val thumbFile = File(sessionFolder, "thumbnail.jpg")
        imageProcessor.createThumbnail(renderedPages.first().absolutePath, thumbFile.absolutePath)

        val now = System.currentTimeMillis()
        val newId = dao.insertDocument(
            ScannedDocumentEntity(
                title = safeTitle,
                pdfPath = outPdf.absolutePath,
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else renderedPages.first().absolutePath,
                pageCount = renderedPages.size,
                fileSizeBytes = outPdf.length(),
                folderId = doc.folderId,
                isSigned = true,
                createdAt = now,
                updatedAt = now
            )
        )
        dao.insertPages(
            renderedPages.mapIndexed { idx, img ->
                DocumentPageEntity(
                    documentId = newId,
                    pageIndex = idx,
                    imagePath = img.absolutePath,
                    originalImagePath = img.absolutePath,
                    filterName = ScanFilter.ORIGINAL.name
                )
            }
        )
        newId
    }

    suspend fun exportDocumentAsImages(
        documentId: Long,
        format: ExportImageFormat
    ): List<File> = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(documentId) ?: return@withContext emptyList()
        val pdfFile = File(doc.pdfPath)
        if (!pdfFile.exists()) return@withContext emptyList()
        val exportFolder = File(exportsRootDir, "export_${System.currentTimeMillis()}")
        pdfEngine.exportPdfToImages(
            pdfFile = pdfFile,
            outputDir = exportFolder,
            baseName = doc.title,
            format = format,
            renderWidth = 1800
        )
    }

    suspend fun deletePageFromDocument(documentId: Long, pageId: Long): Boolean = withContext(Dispatchers.IO) {
        val docWithPages = dao.getDocumentWithPages(documentId) ?: return@withContext false
        val remaining = docWithPages.orderedPages.filter { it.id != pageId }
        if (remaining.isEmpty()) {
            deleteDocument(documentId)
            return@withContext true
        }
        val doc = docWithPages.document
        val pdfFile = File(doc.pdfPath)
        val ok = pdfEngine.generatePdfFromImages(
            imagePaths = remaining.map { it.imagePath },
            outputPdfFile = pdfFile,
            pageSize = PageSizeOption.A4,
            quality = PdfQuality.HIGH
        )
        if (!ok) return@withContext false

        dao.deletePagesForDocument(documentId)
        dao.insertPages(remaining.mapIndexed { idx, p -> p.copy(id = 0L, pageIndex = idx) })
        val thumbFile = File(pdfFile.parentFile, "thumbnail.jpg")
        imageProcessor.createThumbnail(remaining.first().imagePath, thumbFile.absolutePath)
        dao.updateDocument(
            doc.copy(
                pageCount = remaining.size,
                fileSizeBytes = pdfFile.length(),
                thumbnailPath = if (thumbFile.exists()) thumbFile.absolutePath else remaining.first().imagePath,
                updatedAt = System.currentTimeMillis()
            )
        )
        true
    }

    suspend fun renameDocument(documentId: Long, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isNotEmpty()) {
            dao.renameDocument(documentId, trimmed)
        }
    }

    suspend fun moveDocumentToFolder(documentId: Long, folderId: Long?) {
        dao.moveDocumentToFolder(documentId, folderId)
    }

    suspend fun updateDocumentOcrText(documentId: Long, ocrText: String) {
        dao.updateOcrText(documentId, ocrText)
    }

    suspend fun deleteDocument(documentId: Long) = withContext(Dispatchers.IO) {
        val doc = dao.getDocumentById(documentId)
        dao.deleteDocumentById(documentId)
        if (doc != null) {
            val pdfFile = File(doc.pdfPath)
            val parent = pdfFile.parentFile
            if (parent != null && parent.parentFile?.absolutePath == documentsRootDir.absolutePath) {
                parent.deleteRecursively()
            } else {
                pdfFile.delete()
            }
        }
    }

    // Folders
    suspend fun createFolder(name: String, colorArgb: Long): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        return dao.insertFolder(FolderEntity(name = trimmed, colorArgb = colorArgb))
    }

    suspend fun updateFolder(folderId: Long, name: String, colorArgb: Long) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            dao.updateFolder(folderId, trimmed, colorArgb)
        }
    }

    suspend fun deleteFolder(folderId: Long) {
        dao.deleteFolderById(folderId)
    }

    // Storage diagnostics & cleanup
    suspend fun computeStorageStats(): StorageStats = withContext(Dispatchers.IO) {
        val docBytes = folderSize(documentsRootDir) + folderSize(exportsRootDir)
        val cacheBytes = folderSize(scanCacheDir)
        val docFilesCount = documentsRootDir.listFiles()?.size ?: 0
        StorageStats(
            documentCount = docFilesCount,
            totalPdfBytes = docBytes,
            totalCacheBytes = cacheBytes
        )
    }

    suspend fun clearScanCache(): Long = withContext(Dispatchers.IO) {
        val freed = folderSize(scanCacheDir)
        scanCacheDir.listFiles()?.forEach { it.deleteRecursively() }
        freed
    }

    private fun folderSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
