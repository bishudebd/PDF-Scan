package com.example

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.model.CropQuad
import com.example.data.model.DraftScanPage
import com.example.data.model.ExportImageFormat
import com.example.data.model.PageSizeOption
import com.example.data.model.PdfQuality
import com.example.data.model.ScanFilter
import com.example.data.repository.DocumentRepository
import com.example.engine.DocumentScannerEngine
import com.example.engine.DocumentStabilityAnalyzer
import com.example.engine.ImageProcessor
import com.example.engine.MlKitScanOutcome
import com.example.engine.OcrEngine
import com.example.engine.PdfEngine
import com.example.engine.ScannerPreflightStatus
import com.example.ui.viewmodel.ScannerEditorViewModel
import com.google.android.gms.common.ConnectionResult
import android.graphics.pdf.PdfDocument
import android.os.Looper
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

/**
 * Test-only Robolectric shadow for [PdfDocument] under [GraphicsMode.Mode.NATIVE].
 * Android's native PdfDocument JNI (`nativeCreateDocument`) is unimplemented on the host JVM;
 * this shadow backs each page with a real native Skia [Bitmap]+[Canvas] and writes a valid
 * PDF 1.4 binary stream containing the rendered pages so [PdfEngine] is exercised end-to-end.
 */
@Implements(PdfDocument::class)
class TestPdfDocumentShadow {
    private val finishedPages = mutableListOf<Pair<PdfDocument.PageInfo, Bitmap>>()
    private var currentPage: PdfDocument.Page? = null
    private var currentBitmap: Bitmap? = null
    private var closed = false

    @Implementation
    fun __constructor__() {
        // Skip native JNI document creation on JVM host
    }

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        check(!closed) { "document is closed!" }
        check(currentPage == null) { "current page not finished" }
        val bmp = Bitmap.createBitmap(
            pageInfo.pageWidth.coerceAtLeast(1),
            pageInfo.pageHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bmp)
        val page = ReflectionHelpers.callConstructor(
            PdfDocument.Page::class.java,
            ReflectionHelpers.ClassParameter.from(Canvas::class.java, canvas),
            ReflectionHelpers.ClassParameter.from(PdfDocument.PageInfo::class.java, pageInfo)
        )
        currentBitmap = bmp
        currentPage = page
        return page
    }

    @Implementation
    fun finishPage(page: PdfDocument.Page) {
        check(!closed) { "document is closed!" }
        check(currentPage === page) { "invalid page" }
        val bmp = checkNotNull(currentBitmap)
        finishedPages.add(page.info to bmp)
        currentPage = null
        currentBitmap = null
    }

    @Implementation
    fun getPages(): List<PdfDocument.PageInfo> = finishedPages.map { it.first }

    @Implementation
    fun writeTo(out: OutputStream) {
        check(!closed) { "document is closed!" }
        check(currentPage == null) { "current page not finished" }
        val header = "%PDF-1.4\n"
        out.write(header.toByteArray(Charsets.US_ASCII))
        val pageCount = finishedPages.size
        val catalog = "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n"
        val kids = (0 until pageCount).joinToString(" ") { "${3 + it * 2} 0 R" }
        val pagesObj = "2 0 obj << /Type /Pages /Count $pageCount /Kids [ $kids ] >> endobj\n"
        out.write(catalog.toByteArray(Charsets.US_ASCII))
        out.write(pagesObj.toByteArray(Charsets.US_ASCII))
        finishedPages.forEachIndexed { idx, (info, bmp) ->
            val pageObjNum = 3 + idx * 2
            val imgObjNum = pageObjNum + 1
            val jpegBaos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, jpegBaos)
            val jpegBytes = jpegBaos.toByteArray()
            val pageDict = "$pageObjNum 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 ${info.pageWidth} ${info.pageHeight}] /Resources << /XObject << /Im0 $imgObjNum 0 R >> >> >> endobj\n"
            out.write(pageDict.toByteArray(Charsets.US_ASCII))
            val imgHeader = "$imgObjNum 0 obj << /Type /XObject /Subtype /Image /Width ${bmp.width} /Height ${bmp.height} /Filter /DCTDecode /Length ${jpegBytes.size} >>\nstream\n"
            out.write(imgHeader.toByteArray(Charsets.US_ASCII))
            out.write(jpegBytes)
            out.write("\nendstream\nendobj\n".toByteArray(Charsets.US_ASCII))
        }
        out.write("trailer << /Root 1 0 R >>\n%%EOF\n".toByteArray(Charsets.US_ASCII))
        out.flush()
    }

    @Implementation
    fun close() {
        if (closed) return
        closed = true
        currentBitmap?.recycle()
        currentBitmap = null
        currentPage = null
        finishedPages.forEach { it.second.recycle() }
        finishedPages.clear()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], shadows = [TestPdfDocumentShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DocumentScannerEngineTest {

    private lateinit var context: Context
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var pdfEngine: PdfEngine
    private lateinit var database: AppDatabase
    private lateinit var repository: DocumentRepository
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        imageProcessor = ImageProcessor(context)
        pdfEngine = PdfEngine(context, imageProcessor)
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DocumentRepository(
            context = context,
            dao = database.documentDao(),
            imageProcessor = imageProcessor,
            pdfEngine = pdfEngine,
            ocrEngine = OcrEngine(context)
        )
        tempDir = File(context.cacheDir, "scanner_engine_tests_${System.currentTimeMillis()}").apply {
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        database.close()
        tempDir.deleteRecursively()
    }

    private fun createSyntheticDocumentImage(
        fileName: String,
        width: Int = 600,
        height: Int = 800,
        accentColor: Int = Color.rgb(30, 80, 210)
    ): File {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.rgb(30, 34, 42))
        val paperPaint = Paint().apply { color = Color.rgb(240, 240, 236) }
        canvas.drawRect(width * 0.14f, height * 0.12f, width * 0.86f, height * 0.88f, paperPaint)
        val textPaint = Paint().apply { color = Color.rgb(22, 26, 36) }
        for (i in 0..5) {
            val y = height * (0.24f + i * 0.08f)
            canvas.drawRect(width * 0.22f, y, width * 0.78f, y + 14f, textPaint)
        }
        val stampPaint = Paint().apply { color = accentColor }
        canvas.drawCircle(width * 0.70f, height * 0.76f, 38f, stampPaint)

        val file = File(tempDir, fileName)
        imageProcessor.saveBitmap(bmp, file, ExportImageFormat.JPG, 95)
        bmp.recycle()
        return file
    }

    private suspend fun waitUntil(timeoutMs: Long = 5000L, predicate: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            if (predicate()) return
            delay(25L)
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `low storage and play services preflight checks return accurate typed states`() {
        val lowStorageEngine = DocumentScannerEngine(
            context = context,
            storageSpaceProvider = { 12L * 1024L * 1024L },
            playServicesAvailabilityChecker = { ConnectionResult.SUCCESS }
        )
        val storageStatus = lowStorageEngine.checkStorage()
        assertTrue(storageStatus is ScannerPreflightStatus.LowStorage)
        assertEquals(12L, (storageStatus as ScannerPreflightStatus.LowStorage).availableMb)

        val missingGmsEngine = DocumentScannerEngine(
            context = context,
            storageSpaceProvider = { 500L * 1024L * 1024L },
            playServicesAvailabilityChecker = { ConnectionResult.SERVICE_MISSING }
        )
        val gmsStatus = missingGmsEngine.checkMlKitPreflight()
        assertTrue(gmsStatus is ScannerPreflightStatus.PlayServicesUnavailable)
        assertEquals(
            ConnectionResult.SERVICE_MISSING,
            (gmsStatus as ScannerPreflightStatus.PlayServicesUnavailable).statusCode
        )

        val readyEngine = DocumentScannerEngine(
            context = context,
            storageSpaceProvider = { 500L * 1024L * 1024L },
            playServicesAvailabilityChecker = { ConnectionResult.SUCCESS }
        )
        assertEquals(ScannerPreflightStatus.Ready, readyEngine.checkMlKitPreflight())
        assertNotNull(readyEngine.buildMlKitOptions(pageLimit = 50))
        assertNotNull(readyEngine.buildMlKitOptions(pageLimit = 1))
    }

    @Test
    fun `ml kit result parser handles cancellation and invalid data safely`() {
        val engine = DocumentScannerEngine(context)

        val cancelled = engine.parseMlKitActivityResult(Activity.RESULT_CANCELED, null)
        assertEquals(MlKitScanOutcome.Cancelled, cancelled)

        val missingIntent = engine.parseMlKitActivityResult(Activity.RESULT_OK, null)
        assertTrue(missingIntent is MlKitScanOutcome.Error)
    }

    @Test
    fun `auto capture luminance analyzer distinguishes steady document from moving or blank frames`() {
        val gridW = 16
        val gridH = 16
        val docFrame = IntArray(gridW * gridH) { idx ->
            val x = idx % gridW
            val y = idx / gridW
            val isBorder = x <= 1 || x >= gridW - 2 || y <= 1 || y >= gridH - 2
            if (isBorder) 35 else 225
        }
        val movedFrame = IntArray(gridW * gridH) { idx ->
            (docFrame[idx] + 45).coerceAtMost(255)
        }
        val blankFrame = IntArray(gridW * gridH) { 120 }

        val steadyEval = DocumentStabilityAnalyzer.evaluateLuminanceGrid(docFrame, gridW, gridH, docFrame)
        assertTrue("Document contrast should be detected", steadyEval.hasDocumentContrast)
        assertTrue("Identical frames must be steady", steadyEval.isSteady)

        val movingEval = DocumentStabilityAnalyzer.evaluateLuminanceGrid(docFrame, gridW, gridH, movedFrame)
        assertTrue(movingEval.hasDocumentContrast)
        assertFalse("Large motion delta must not be steady", movingEval.isSteady)

        val blankEval = DocumentStabilityAnalyzer.evaluateLuminanceGrid(blankFrame, gridW, gridH, blankFrame)
        assertFalse("Uniform blank frame must not trigger document detection", blankEval.hasDocumentContrast)
    }

    @Test
    fun `edge detection perspective crop rotation and all four document filters produce valid output`() = runBlocking {
        val srcFile = createSyntheticDocumentImage("sample_doc.jpg", width = 600, height = 800)

        // 1. Auto document edge detection finds paper sheet inside dark desk margin
        val quad = imageProcessor.detectDocumentQuad(srcFile.absolutePath)
        assertFalse("Detected quad should inset from full frame: $quad", quad.isFullFrame())
        assertTrue("Expected topLeft.x near 0.14 but got ${quad.topLeft.x} (quad=$quad)", quad.topLeft.x in 0.05f..0.28f)
        assertTrue("Expected bottomRight.x near 0.86 but got ${quad.bottomRight.x} (quad=$quad)", quad.bottomRight.x in 0.72f..0.95f)

        // 2. Manual 4-point perspective crop + 90-degree rotation swaps portrait to landscape
        val manualQuad = CropQuad(
            topLeft = PointF(0.15f, 0.12f),
            topRight = PointF(0.85f, 0.14f),
            bottomRight = PointF(0.86f, 0.88f),
            bottomLeft = PointF(0.14f, 0.86f)
        )
        val warpedFile = File(tempDir, "warped_rotated.jpg")
        val warpOk = imageProcessor.applyPerspectiveCropAndRotate(
            sourcePath = srcFile.absolutePath,
            outputPath = warpedFile.absolutePath,
            quad = manualQuad,
            rotationDegrees = 90
        )
        assertTrue("Perspective warp & rotate should succeed", warpOk)
        assertTrue(warpedFile.exists() && warpedFile.length() > 0L)

        val warpedBmp = BitmapFactory.decodeFile(warpedFile.absolutePath)
        assertNotNull(warpedBmp)
        assertTrue(
            "90-degree rotated portrait page must be wider than tall (${warpedBmp.width}x${warpedBmp.height})",
            warpedBmp.width > warpedBmp.height
        )

        // 3. Test Original, Magic Colour, Grayscale, and Black & White filters
        val originalBmp = imageProcessor.applyFilterToBitmap(warpedBmp, ScanFilter.ORIGINAL)
        val magicBmp = imageProcessor.applyFilterToBitmap(warpedBmp, ScanFilter.ENHANCED)
        val grayBmp = imageProcessor.applyFilterToBitmap(warpedBmp, ScanFilter.GRAYSCALE)
        val bwBmp = imageProcessor.applyFilterToBitmap(warpedBmp, ScanFilter.BLACK_AND_WHITE)

        assertEquals(warpedBmp.width, originalBmp.width)
        assertEquals(warpedBmp.width, magicBmp.width)

        val sampleGray = grayBmp.getPixel(grayBmp.width / 2, grayBmp.height / 2)
        assertEquals(Color.red(sampleGray), Color.green(sampleGray))
        assertEquals(Color.green(sampleGray), Color.blue(sampleGray))

        for (x in 0 until bwBmp.width step 25) {
            for (y in 0 until bwBmp.height step 25) {
                val p = bwBmp.getPixel(x, y)
                assertTrue("B&W pixel must be pure black or pure white", p == Color.BLACK || p == Color.WHITE)
            }
        }

        warpedBmp.recycle()
        originalBmp.recycle()
        magicBmp.recycle()
        grayBmp.recycle()
        bwBmp.recycle()
    }

    @Test
    fun `multi page scanning engine generates valid high resolution PDF and JPG outputs`() = runBlocking {
        val page1File = createSyntheticDocumentImage("page_1.jpg", width = 800, height = 1100)
        val page2File = createSyntheticDocumentImage("page_2.jpg", width = 800, height = 1100, accentColor = Color.rgb(200, 40, 60))

        val drafts = listOf(
            DraftScanPage(id = "p1", rawImagePath = page1File.absolutePath, enhancedImagePath = page1File.absolutePath, filter = ScanFilter.ENHANCED),
            DraftScanPage(id = "p2", rawImagePath = page2File.absolutePath, enhancedImagePath = page2File.absolutePath, filter = ScanFilter.BLACK_AND_WHITE)
        )

        // 1. Direct PDF generation check
        val directPdf = File(tempDir, "direct_test.pdf")
        val generated = pdfEngine.generatePdfFromImages(
            imagePaths = drafts.map { it.displayPath },
            outputPdfFile = directPdf,
            pageSize = PageSizeOption.A4,
            quality = PdfQuality.HIGH
        )
        val pdfLogs = org.robolectric.shadows.ShadowLog.getLogsForTag("PdfEngine")
            .joinToString("; ") { "${it.msg} | ${it.throwable}" }
        assertTrue(
            "Direct PdfEngine generation failed (exists=${directPdf.exists()}, len=${directPdf.length()}, logs=$pdfLogs)",
            generated
        )

        // 2. Full Repository High-Res PDF + Room persistence check
        val docId = repository.saveDraftSessionAsDocument(
            title = "Engine_Output_Test",
            pages = drafts,
            pageSize = PageSizeOption.A4,
            quality = PdfQuality.HIGH
        )
        assertNotNull("Saved document ID must not be null", docId)
        val persisted = repository.getDocumentWithPages(docId!!)
        assertNotNull(persisted)
        assertEquals(2, persisted!!.document.pageCount)
        assertEquals(2, persisted.orderedPages.size)

        val pdfFile = File(persisted.document.pdfPath)
        assertTrue("Generated PDF file must exist", pdfFile.exists())
        assertTrue("Generated PDF file must be non-empty", pdfFile.length() > 100L)
        val pdfBytes = pdfFile.readBytes()
        val pdfText = String(pdfBytes, Charsets.ISO_8859_1)
        assertTrue("Generated PDF must start with %PDF header", pdfText.startsWith("%PDF"))
        assertTrue("Generated PDF must encode 2 pages", pdfText.contains("/Count 2"))
        assertTrue("Generated PDF must embed JPEG page streams", pdfText.contains("/DCTDecode"))

        // 3. Verify High-Res JPG export for every page
        val jpgFiles = repository.exportDraftSessionAsJpgs(
            title = "Engine_Output_Test",
            pages = drafts,
            quality = PdfQuality.HIGH
        )
        assertEquals(2, jpgFiles.size)
        jpgFiles.forEach { file ->
            assertTrue(file.exists() && file.length() > 500L)
            assertTrue(file.name.endsWith(".jpg"))
            val decoded = BitmapFactory.decodeFile(file.absolutePath)
            assertNotNull("Exported JPG must decode cleanly", decoded)
            assertEquals(800, decoded.width)
            assertEquals(1100, decoded.height)
            decoded.recycle()
        }
    }

    @Test
    fun `scanner viewModel supports add rearrange rescan delete cancellation and low storage`() = runBlocking {
        var fakeFreeBytes = 500L * 1024L * 1024L
        val engine = DocumentScannerEngine(
            context = context,
            storageSpaceProvider = { fakeFreeBytes },
            playServicesAvailabilityChecker = { ConnectionResult.SUCCESS }
        )
        val viewModel = ScannerEditorViewModel(
            repository = repository,
            preferencesManager = PreferencesManager(context),
            scannerEngine = engine
        )

        val p1 = createSyntheticDocumentImage("vm_p1.jpg")
        val p2 = createSyntheticDocumentImage("vm_p2.jpg")
        val p3 = createSyntheticDocumentImage("vm_p3.jpg")
        val p2Rescan = createSyntheticDocumentImage("vm_p2_rescan.jpg")

        viewModel.addCapturedFile(p1)
        waitUntil { viewModel.uiState.value.pages.size == 1 }
        viewModel.addCapturedFile(p2)
        waitUntil { viewModel.uiState.value.pages.size == 2 }
        viewModel.addCapturedFile(p3)
        waitUntil { viewModel.uiState.value.pages.size == 3 }
        assertEquals(3, viewModel.uiState.value.pages.size)

        // Rearrange: move page index 2 -> index 0
        val thirdId = viewModel.uiState.value.pages[2].id
        viewModel.movePage(2, 0)
        assertEquals(thirdId, viewModel.uiState.value.pages[0].id)

        // Rescan individual page at index 1
        viewModel.startRescanForPage(1)
        assertTrue(viewModel.uiState.value.isRescanningPage)
        viewModel.addCapturedFile(p2Rescan)
        waitUntil { !viewModel.uiState.value.isRescanningPage }
        assertEquals(3, viewModel.uiState.value.pages.size)
        assertEquals(p2Rescan.absolutePath, viewModel.uiState.value.pages[1].rawImagePath)

        // Cancellation preserves existing pages
        viewModel.handleMlKitOutcome(MlKitScanOutcome.Cancelled) {}
        assertEquals(3, viewModel.uiState.value.pages.size)
        assertNotNull(viewModel.uiState.value.infoMessage)

        // Delete page
        viewModel.removePage(0)
        assertEquals(2, viewModel.uiState.value.pages.size)

        // Low-storage guard blocks capture & surfaces lowStorageStatus
        fakeFreeBytes = 5L * 1024L * 1024L
        val allowed = viewModel.ensureStorageAvailable()
        assertFalse(allowed)
        assertNotNull(viewModel.uiState.value.lowStorageStatus)
    }
}
