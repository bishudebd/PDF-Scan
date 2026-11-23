package com.example.data.model

import android.graphics.PointF

enum class ScanFilter(val label: String, val shortLabel: String) {
    ORIGINAL("Original", "Original"),
    ENHANCED("Magic Colour", "Magic Colour"),
    GRAYSCALE("Grayscale", "Grayscale"),
    BLACK_AND_WHITE("Black & White", "B&W"),
    CRISP_DOC("Crisp Document", "Crisp");

    companion object {
        fun fromName(name: String): ScanFilter =
            entries.firstOrNull { it.name == name } ?: ENHANCED
    }
}

enum class PdfQuality(val label: String, val jpegQuality: Int, val maxDimensionPx: Int, val subtitle: String) {
    HIGH("High-Res (300 DPI)", 95, 2800, "Maximum sharpness for print & archival"),
    MEDIUM("Balanced", 80, 1800, "Great clarity with compact file size"),
    LOW("Compact", 56, 1200, "Smallest file size for quick sharing");

    companion object {
        fun fromName(name: String): PdfQuality =
            entries.firstOrNull { it.name == name } ?: HIGH
    }
}

enum class PageSizeOption(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4 (210 × 297 mm)", 595, 842),
    LETTER("US Letter (8.5 × 11 in)", 612, 792),
    AUTO("Fit to Image", 0, 0);

    companion object {
        fun fromName(name: String): PageSizeOption =
            entries.firstOrNull { it.name == name } ?: A4
    }
}

enum class ScanMode(val label: String) {
    DOCUMENT("Document"),
    ID_CARD("ID Card"),
    OCR("OCR Text")
}

/**
 * Normalized 4-corner quadrilateral [0f..1f] in image coordinates:
 * topLeft, topRight, bottomRight, bottomLeft.
 */
data class CropQuad(
    val topLeft: PointF = PointF(0.06f, 0.06f),
    val topRight: PointF = PointF(0.94f, 0.06f),
    val bottomRight: PointF = PointF(0.94f, 0.94f),
    val bottomLeft: PointF = PointF(0.06f, 0.94f)
) {
    companion object {
        val FULL = CropQuad(
            topLeft = PointF(0f, 0f),
            topRight = PointF(1f, 0f),
            bottomRight = PointF(1f, 1f),
            bottomLeft = PointF(0f, 1f)
        )
    }

    fun isFullFrame(): Boolean {
        val eps = 0.005f
        return topLeft.x <= eps && topLeft.y <= eps &&
            topRight.x >= 1f - eps && topRight.y <= eps &&
            bottomRight.x >= 1f - eps && bottomRight.y >= 1f - eps &&
            bottomLeft.x <= eps && bottomLeft.y >= 1f - eps
    }
}

/**
 * Editable page in an active scanning/import session before saving into Room & PDF.
 */
data class DraftScanPage(
    val id: String,
    val rawImagePath: String,
    val croppedImagePath: String? = null,
    val enhancedImagePath: String? = null,
    val cropQuad: CropQuad = CropQuad.FULL,
    val rotationDegrees: Int = 0,
    val filter: ScanFilter = ScanFilter.ENHANCED,
    val brightness: Float = 0f,     // -60f..60f
    val contrast: Float = 1f        // 0.6f..1.8f
) {
    val activeSourcePath: String
        get() = croppedImagePath ?: rawImagePath

    val displayPath: String
        get() = enhancedImagePath ?: croppedImagePath ?: rawImagePath
}

enum class DocumentSortOrder(val label: String) {
    NEWEST("Newest First"),
    OLDEST("Oldest First"),
    NAME_ASC("Name (A–Z)"),
    SIZE_DESC("Largest Size")
}

enum class ExportImageFormat(val label: String, val extension: String, val mimeType: String) {
    JPG("JPEG (.jpg)", "jpg", "image/jpeg"),
    PNG("PNG (.png)", "png", "image/png")
}
