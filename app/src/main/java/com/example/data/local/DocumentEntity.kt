package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val colorArgb: Long = 0xFF2563EB,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scanned_documents",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId"), Index("updatedAt")]
)
data class ScannedDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val pdfPath: String,
    val thumbnailPath: String,
    val pageCount: Int,
    val fileSizeBytes: Long,
    val folderId: Long? = null,
    val ocrText: String = "",
    val isSigned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "document_pages",
    foreignKeys = [
        ForeignKey(
            entity = ScannedDocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class DocumentPageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val documentId: Long,
    val pageIndex: Int,
    val imagePath: String,
    val originalImagePath: String,
    val filterName: String,
    val rotationDegrees: Int = 0
)

data class DocumentWithPages(
    @Embedded val document: ScannedDocumentEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "documentId"
    )
    val pages: List<DocumentPageEntity>
) {
    val orderedPages: List<DocumentPageEntity>
        get() = pages.sortedBy { it.pageIndex }
}

data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    val documentCount: Int
)
