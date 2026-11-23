package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

data class OcrResult(
    val fullText: String,
    val blockCount: Int,
    val wordCount: Int
)

/**
 * On-device Google ML Kit Text Recognition wrapper.
 * Works completely locally without network calls or registration.
 */
class OcrEngine(private val context: Context) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractTextFromBitmap(bitmap: Bitmap): Result<OcrResult> = withContext(Dispatchers.Default) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        runRecognition(inputImage)
    }

    suspend fun extractTextFromUri(uri: Uri): Result<OcrResult> = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            runRecognition(inputImage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun runRecognition(inputImage: InputImage): Result<OcrResult> =
        suspendCancellableCoroutine { cont ->
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val cleaned = visionText.text.trim()
                    val words = if (cleaned.isEmpty()) 0 else cleaned.split(Regex("\\s+")).size
                    if (cont.isActive) {
                        cont.resume(
                            Result.success(
                                OcrResult(
                                    fullText = cleaned,
                                    blockCount = visionText.textBlocks.size,
                                    wordCount = words
                                )
                            )
                        )
                    }
                }
                .addOnFailureListener { ex ->
                    if (cont.isActive) {
                        cont.resume(Result.failure(ex))
                    }
                }
        }
}
