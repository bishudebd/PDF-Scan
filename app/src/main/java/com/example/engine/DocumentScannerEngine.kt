package com.example.engine

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.StatFs
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Result of checking storage space and Google Play Services before starting a scan or export.
 */
sealed class ScannerPreflightStatus {
    data object Ready : ScannerPreflightStatus()
    data class LowStorage(
        val availableBytes: Long,
        val requiredBytes: Long
    ) : ScannerPreflightStatus() {
        val availableMb: Long get() = availableBytes / (1024L * 1024L)
        val requiredMb: Long get() = requiredBytes / (1024L * 1024L)
    }
    data class PlayServicesUnavailable(
        val statusCode: Int,
        val errorMessage: String
    ) : ScannerPreflightStatus()
}

/**
 * Structured outcome of a Google ML Kit Document Scanner session.
 */
sealed class MlKitScanOutcome {
    data class Success(
        val pageUris: List<Uri>,
        val pdfUri: Uri?,
        val pageCount: Int
    ) : MlKitScanOutcome()

    data object Cancelled : MlKitScanOutcome()

    data class Error(val message: String) : MlKitScanOutcome()
}

/**
 * Injectable disk space provider so storage guards are 100% deterministic and unit-testable.
 */
fun interface StorageSpaceProvider {
    fun availableBytes(path: String): Long
}

class StatFsStorageSpaceProvider : StorageSpaceProvider {
    override fun availableBytes(path: String): Long {
        return try {
            val stat = StatFs(path)
            stat.availableBytes
        } catch (_: Exception) {
            0L
        }
    }
}

/**
 * Dedicated Document Scanning Engine encapsulating:
 * - Google ML Kit Document Scanner client & options configuration (multi-page or single-page rescan)
 * - Google Play Services availability diagnostics
 * - Low-storage pre-flight protection for scanning, JPG export, and PDF generation
 * - Parsing ML Kit Intent results (Success, Cancelled, Error)
 */
class DocumentScannerEngine(
    private val context: Context,
    private val storageSpaceProvider: StorageSpaceProvider = StatFsStorageSpaceProvider(),
    private val playServicesAvailabilityChecker: (Context) -> Int = { ctx ->
        GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(ctx)
    }
) {
    companion object {
        const val MIN_SCAN_STORAGE_BYTES: Long = 50L * 1024L * 1024L // 50 MB minimum for high-res multi-page scans
        const val MIN_EXPORT_STORAGE_BYTES: Long = 20L * 1024L * 1024L // 20 MB minimum for PDF/JPG output
        const val DEFAULT_MAX_PAGES: Int = 50
    }

    /**
     * Checks device free storage before capture or file generation.
     */
    fun checkStorage(requiredBytes: Long = MIN_SCAN_STORAGE_BYTES): ScannerPreflightStatus {
        val freeBytes = storageSpaceProvider.availableBytes(context.filesDir.absolutePath)
        return if (freeBytes < requiredBytes) {
            ScannerPreflightStatus.LowStorage(
                availableBytes = freeBytes,
                requiredBytes = requiredBytes
            )
        } else {
            ScannerPreflightStatus.Ready
        }
    }

    /**
     * Checks both storage and Google Play Services availability prior to launching ML Kit Document Scanner.
     */
    fun checkMlKitPreflight(requiredBytes: Long = MIN_SCAN_STORAGE_BYTES): ScannerPreflightStatus {
        val storageStatus = checkStorage(requiredBytes)
        if (storageStatus !is ScannerPreflightStatus.Ready) return storageStatus

        val gpsStatus = try {
            playServicesAvailabilityChecker(context)
        } catch (_: Exception) {
            ConnectionResult.SERVICE_MISSING
        }
        return if (gpsStatus == ConnectionResult.SUCCESS) {
            ScannerPreflightStatus.Ready
        } else {
            val reason = when (gpsStatus) {
                ConnectionResult.SERVICE_MISSING -> "Google Play Services is not installed on this device."
                ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED -> "Google Play Services needs an update for Smart Auto-Scan."
                ConnectionResult.SERVICE_DISABLED -> "Google Play Services is currently disabled."
                ConnectionResult.SERVICE_INVALID -> "Google Play Services installation is invalid."
                else -> "Google Play Services is unavailable (code $gpsStatus)."
            }
            ScannerPreflightStatus.PlayServicesUnavailable(
                statusCode = gpsStatus,
                errorMessage = reason
            )
        }
    }

    /**
     * Builds configured Google ML Kit GmsDocumentScannerOptions requesting both high-res JPEG pages
     * and PDF output with full editing, manual cropping, rotation, and automatic capture support.
     */
    fun buildMlKitOptions(
        pageLimit: Int = DEFAULT_MAX_PAGES,
        allowGalleryImport: Boolean = true
    ): GmsDocumentScannerOptions {
        return GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(allowGalleryImport)
            .setPageLimit(pageLimit.coerceIn(1, 100))
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
            )
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    /**
     * Requests the IntentSender from Google ML Kit Document Scanner asynchronously,
     * pre-validating storage and Google Play Services availability.
     */
    fun prepareMlKitScanIntent(
        activity: Activity,
        pageLimit: Int = DEFAULT_MAX_PAGES,
        allowGalleryImport: Boolean = true,
        onIntentReady: (IntentSender) -> Unit,
        onPreflightFailed: (ScannerPreflightStatus) -> Unit,
        onError: (String) -> Unit
    ) {
        val preflight = checkMlKitPreflight()
        if (preflight !is ScannerPreflightStatus.Ready) {
            onPreflightFailed(preflight)
            return
        }
        try {
            val options = buildMlKitOptions(pageLimit, allowGalleryImport)
            val client = GmsDocumentScanning.getClient(options)
            client.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    onIntentReady(intentSender)
                }
                .addOnFailureListener { ex ->
                    val detail = ex.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "ML Kit Document Scanner could not initialize on this device."
                    onError(detail)
                }
        } catch (ex: Exception) {
            onError(ex.localizedMessage ?: "Failed to launch ML Kit Document Scanner.")
        }
    }

    /**
     * Parses an ActivityResult from the Google ML Kit Document Scanner into a typed [MlKitScanOutcome].
     */
    fun parseMlKitActivityResult(resultCode: Int, data: Intent?): MlKitScanOutcome {
        if (resultCode == Activity.RESULT_CANCELED) {
            return MlKitScanOutcome.Cancelled
        }
        if (resultCode != Activity.RESULT_OK || data == null) {
            return MlKitScanOutcome.Error("Document scan did not return valid output.")
        }
        val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
            ?: return MlKitScanOutcome.Error("Unable to parse ML Kit Document Scanner output.")

        val pageUris = scanResult.pages?.map { it.imageUri }.orEmpty()
        val pdfUri = scanResult.pdf?.uri
        if (pageUris.isEmpty() && pdfUri == null) {
            return MlKitScanOutcome.Error("No scanned pages were produced.")
        }
        return MlKitScanOutcome.Success(
            pageUris = pageUris,
            pdfUri = pdfUri,
            pageCount = max(pageUris.size, scanResult.pdf?.pageCount ?: 0)
        )
    }
}

/**
 * Live state emitted by [DocumentStabilityAnalyzer] during CameraX preview for real automatic capture.
 */
data class AutoCaptureDetectionState(
    val documentDetected: Boolean = false,
    val isSteady: Boolean = false,
    val holdProgress: Float = 0f, // 0f..1f toward automatic shutter trigger
    val guidanceText: String = "Position document inside frame"
)

/**
 * Real CameraX ImageAnalysis analyzer operating on the Y luminance plane to detect:
 * 1. Clear document/background contrast inside the scan frame
 * 2. Frame-to-frame camera stability (low motion SAD)
 * When both hold steady for [requiredSteadyMs], invokes [onAutoTriggerCapture].
 */
class DocumentStabilityAnalyzer(
    private val isAutoCaptureEnabled: () -> Boolean,
    private val isCaptureBusy: () -> Boolean,
    private val onStateChanged: (AutoCaptureDetectionState) -> Unit,
    private val onAutoTriggerCapture: () -> Unit,
    private val requiredSteadyMs: Long = 1100L,
    private val cooldownMs: Long = 2400L
) : ImageAnalysis.Analyzer {

    private var previousSample: IntArray? = null
    private var steadyStartTimeMs: Long = 0L
    private var lastTriggerTimeMs: Long = 0L
    private var lastAnalyzeTimeMs: Long = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val now = System.currentTimeMillis()
            if (now - lastAnalyzeTimeMs < 140L) return
            lastAnalyzeTimeMs = now

            if (!isAutoCaptureEnabled() || isCaptureBusy()) {
                resetSteady()
                return
            }

            if (now - lastTriggerTimeMs < cooldownMs) {
                onStateChanged(
                    AutoCaptureDetectionState(
                        documentDetected = true,
                        isSteady = false,
                        holdProgress = 0f,
                        guidanceText = "Captured! Present next page…"
                    )
                )
                return
            }

            val yPlane = image.planes.firstOrNull() ?: return
            val buffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val pixelStride = yPlane.pixelStride
            val width = image.width
            val height = image.height
            if (width < 32 || height < 32) return

            val gridW = 16
            val gridH = 16
            val sample = IntArray(gridW * gridH)
            for (gy in 0 until gridH) {
                val py = (gy * height) / gridH
                val rowOffset = py * rowStride
                for (gx in 0 until gridW) {
                    val px = (gx * width) / gridW
                    val idx = rowOffset + px * pixelStride
                    if (idx in 0 until buffer.limit()) {
                        sample[gy * gridW + gx] = buffer.get(idx).toInt() and 0xFF
                    }
                }
            }

            val eval = evaluateLuminanceGrid(sample, gridW, gridH, previousSample)
            previousSample = sample

            if (!eval.hasDocumentContrast) {
                resetSteady()
                onStateChanged(
                    AutoCaptureDetectionState(
                        documentDetected = false,
                        isSteady = false,
                        holdProgress = 0f,
                        guidanceText = "Align document cleanly inside frame"
                    )
                )
                return
            }

            if (!eval.isSteady) {
                steadyStartTimeMs = 0L
                onStateChanged(
                    AutoCaptureDetectionState(
                        documentDetected = true,
                        isSteady = false,
                        holdProgress = 0.15f,
                        guidanceText = "Document detected · Hold still…"
                    )
                )
                return
            }

            if (steadyStartTimeMs == 0L) {
                steadyStartTimeMs = now
            }
            val elapsed = now - steadyStartTimeMs
            val progress = (elapsed.toFloat() / requiredSteadyMs).coerceIn(0f, 1f)
            onStateChanged(
                AutoCaptureDetectionState(
                    documentDetected = true,
                    isSteady = true,
                    holdProgress = progress,
                    guidanceText = if (progress >= 0.95f) "Capturing…" else "Hold steady for auto-capture…"
                )
            )
            if (elapsed >= requiredSteadyMs) {
                lastTriggerTimeMs = now
                steadyStartTimeMs = 0L
                onAutoTriggerCapture()
            }
        } finally {
            image.close()
        }
    }

    private fun resetSteady() {
        steadyStartTimeMs = 0L
    }

    companion object {
        data class GridEvaluation(
            val hasDocumentContrast: Boolean,
            val isSteady: Boolean,
            val contrastDelta: Float,
            val motionDelta: Float
        )

        /**
         * Pure deterministic luminance-grid evaluation (unit-tested directly in JVM tests).
         * Compares inner page zone against outer border ring for document contrast,
         * and computes mean absolute difference vs previous frame for camera stillness.
         */
        fun evaluateLuminanceGrid(
            current: IntArray,
            gridW: Int,
            gridH: Int,
            previous: IntArray?
        ): GridEvaluation {
            var borderSum = 0L
            var borderCount = 0
            var centerSum = 0L
            var centerCount = 0

            for (y in 0 until gridH) {
                for (x in 0 until gridW) {
                    val v = current[y * gridW + x]
                    val isBorder = x <= 1 || x >= gridW - 2 || y <= 1 || y >= gridH - 2
                    val isCenter = x in (gridW / 4) until (gridW * 3 / 4) &&
                        y in (gridH / 4) until (gridH * 3 / 4)
                    if (isBorder) {
                        borderSum += v
                        borderCount++
                    } else if (isCenter) {
                        centerSum += v
                        centerCount++
                    }
                }
            }

            val borderAvg = if (borderCount > 0) borderSum.toFloat() / borderCount else 0f
            val centerAvg = if (centerCount > 0) centerSum.toFloat() / centerCount else 0f
            val contrastDelta = abs(centerAvg - borderAvg)
            val hasDocument = contrastDelta >= 22f && centerAvg >= 75f

            val motionDelta = if (previous != null && previous.size == current.size) {
                var diffSum = 0L
                for (i in current.indices) {
                    diffSum += abs(current[i] - previous[i])
                }
                diffSum.toFloat() / current.size
            } else {
                999f
            }

            return GridEvaluation(
                hasDocumentContrast = hasDocument,
                isSteady = motionDelta <= 8.5f,
                contrastDelta = contrastDelta,
                motionDelta = motionDelta
            )
        }
    }
}
