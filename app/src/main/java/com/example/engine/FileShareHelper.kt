package com.example.engine

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileShareHelper {

    fun getUriForFile(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun sharePdf(context: Context, pdfFile: File, title: String) {
        if (!pdfFile.exists()) return
        val uri = getUriForFile(context, pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share PDF Document").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareImages(context: Context, files: List<File>, mimeType: String) {
        if (files.isEmpty()) return
        val uris = ArrayList(files.filter { it.exists() }.map { getUriForFile(context, it) })
        if (uris.isEmpty()) return

        val intent = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uris.first())
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeType
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Share Exported Images").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun shareText(context: Context, text: String, title: String = "Extracted OCR Text") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Share Text").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }

    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun formatShortDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }

    fun generateDefaultDocTitle(prefix: String = "Scan"): String {
        val formatter = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US)
        return "${prefix}_${formatter.format(Date())}"
    }
}
