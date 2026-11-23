package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.DocumentRepository
import com.example.engine.DocumentScannerEngine
import com.example.engine.ImageProcessor
import com.example.engine.LocalBackupManager
import com.example.engine.OcrEngine
import com.example.engine.PdfEngine

class AppContainer(application: Application) {
    val database: AppDatabase = AppDatabase.getInstance(application)
    val preferencesManager: PreferencesManager = PreferencesManager(application)
    val imageProcessor: ImageProcessor = ImageProcessor(application)
    val pdfEngine: PdfEngine = PdfEngine(application, imageProcessor)
    val ocrEngine: OcrEngine = OcrEngine(application)
    val scannerEngine: DocumentScannerEngine = DocumentScannerEngine(application)
    val documentRepository: DocumentRepository = DocumentRepository(
        context = application,
        dao = database.documentDao(),
        imageProcessor = imageProcessor,
        pdfEngine = pdfEngine,
        ocrEngine = ocrEngine
    )
    val backupManager: LocalBackupManager = LocalBackupManager(application, documentRepository)
}

class PdfScanApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
