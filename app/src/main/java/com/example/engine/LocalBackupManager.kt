package com.example.engine

import android.content.Context
import android.net.Uri
import com.example.data.repository.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupExportResult(
    val exportedDocuments: Int,
    val totalBytes: Long
)

data class BackupRestoreResult(
    val restoredDocuments: Int
)

/**
 * Local ZIP-based backup & restore engine for PDF Scan documents.
 * Uses Android Storage Access Framework URIs so users can save backups to Downloads,
 * SD card, or their preferred cloud storage provider without extra permissions.
 */
class LocalBackupManager(
    private val context: Context,
    private val repository: DocumentRepository
) {
    suspend fun exportBackupToUri(destinationUri: Uri): Result<BackupExportResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val documents = repository.observeAllDocuments().first()
                val manifestArray = JSONArray()
                var exportedCount = 0
                var totalBytes = 0L

                val outStream = context.contentResolver.openOutputStream(destinationUri)
                    ?: error("Unable to open backup destination")

                outStream.use { rawOut ->
                    ZipOutputStream(rawOut).use { zip ->
                        documents.forEachIndexed { index, doc ->
                            val pdfFile = File(doc.pdfPath)
                            if (pdfFile.exists() && pdfFile.length() > 0L) {
                                val safeName = doc.title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                                val entryName = "pdfs/${index + 1}_${safeName}.pdf"
                                zip.putNextEntry(ZipEntry(entryName))
                                pdfFile.inputStream().use { input ->
                                    totalBytes += input.copyTo(zip)
                                }
                                zip.closeEntry()

                                manifestArray.put(
                                    JSONObject().apply {
                                        put("title", doc.title)
                                        put("entry", entryName)
                                        put("pageCount", doc.pageCount)
                                        put("createdAt", doc.createdAt)
                                        put("isSigned", doc.isSigned)
                                    }
                                )
                                exportedCount++
                            }
                        }

                        val manifestRoot = JSONObject().apply {
                            put("app", "PDF Scan")
                            put("formatVersion", 1)
                            put("exportedAt", System.currentTimeMillis())
                            put("documentCount", exportedCount)
                            put("documents", manifestArray)
                        }
                        val manifestBytes = manifestRoot.toString(2).toByteArray(Charsets.UTF_8)
                        zip.putNextEntry(ZipEntry("manifest.json"))
                        zip.write(manifestBytes)
                        zip.closeEntry()
                        totalBytes += manifestBytes.size
                    }
                }
                BackupExportResult(exportedDocuments = exportedCount, totalBytes = totalBytes)
            }
        }

    suspend fun restoreBackupFromUri(sourceUri: Uri): Result<BackupRestoreResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tempDir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply {
                    mkdirs()
                }
                try {
                    val extractedEntries = mutableMapOf<String, File>()
                    var manifestJson: String? = null

                    val inStream = context.contentResolver.openInputStream(sourceUri)
                        ?: error("Unable to read selected backup file")

                    inStream.use { rawIn ->
                        ZipInputStream(rawIn).use { zip ->
                            var entry = zip.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory) {
                                    val normalizedName = entry.name.replace("\\", "/")
                                    if (normalizedName == "manifest.json") {
                                        manifestJson = zip.readBytes().toString(Charsets.UTF_8)
                                    } else if (normalizedName.endsWith(".pdf", ignoreCase = true) && !normalizedName.contains("..")) {
                                        val outFile = File(tempDir, File(normalizedName).name)
                                        FileOutputStream(outFile).use { out ->
                                            zip.copyTo(out)
                                        }
                                        extractedEntries[normalizedName] = outFile
                                    }
                                }
                                zip.closeEntry()
                                entry = zip.nextEntry
                            }
                        }
                    }

                    var restoredCount = 0
                    val handledFiles = mutableSetOf<File>()

                    if (manifestJson != null) {
                        val root = JSONObject(manifestJson!!)
                        val docs = root.optJSONArray("documents")
                        if (docs != null) {
                            for (i in 0 until docs.length()) {
                                val item = docs.optJSONObject(i) ?: continue
                                val entryName = item.optString("entry")
                                val title = item.optString("title").ifBlank { "Restored Document ${i + 1}" }
                                val isSigned = item.optBoolean("isSigned", false)
                                val file = extractedEntries[entryName]
                                if (file != null && file.exists()) {
                                    handledFiles.add(file)
                                    val id = repository.importPdfFileAsDocument(
                                        sourceFile = file,
                                        title = title,
                                        isSigned = isSigned
                                    )
                                    if (id != null) restoredCount++
                                }
                            }
                        }
                    }

                    // Fallback for any PDFs inside the ZIP not listed in manifest.json
                    extractedEntries.values.forEach { file ->
                        if (file !in handledFiles && file.exists()) {
                            val id = repository.importPdfFileAsDocument(
                                sourceFile = file,
                                title = file.nameWithoutExtension
                            )
                            if (id != null) restoredCount++
                        }
                    }

                    BackupRestoreResult(restoredDocuments = restoredCount)
                } finally {
                    tempDir.deleteRecursively()
                }
            }
        }
}
