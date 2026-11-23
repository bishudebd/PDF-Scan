package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM scanned_documents ORDER BY updatedAt DESC")
    fun observeAllDocuments(): Flow<List<ScannedDocumentEntity>>

    @Query("SELECT * FROM scanned_documents ORDER BY updatedAt DESC LIMIT :limit")
    fun observeRecentDocuments(limit: Int = 6): Flow<List<ScannedDocumentEntity>>

    @Query(
        """
        SELECT * FROM scanned_documents
        WHERE title LIKE '%' || :query || '%' OR ocrText LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
        """
    )
    fun searchDocuments(query: String): Flow<List<ScannedDocumentEntity>>

    @Query("SELECT * FROM scanned_documents WHERE folderId = :folderId ORDER BY updatedAt DESC")
    fun observeDocumentsInFolder(folderId: Long): Flow<List<ScannedDocumentEntity>>

    @Transaction
    @Query("SELECT * FROM scanned_documents WHERE id = :documentId")
    fun observeDocumentWithPages(documentId: Long): Flow<DocumentWithPages?>

    @Transaction
    @Query("SELECT * FROM scanned_documents WHERE id = :documentId")
    suspend fun getDocumentWithPages(documentId: Long): DocumentWithPages?

    @Query("SELECT * FROM scanned_documents WHERE id = :documentId")
    suspend fun getDocumentById(documentId: Long): ScannedDocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: ScannedDocumentEntity): Long

    @Update
    suspend fun updateDocument(document: ScannedDocumentEntity)

    @Query("UPDATE scanned_documents SET title = :newTitle, updatedAt = :updatedAt WHERE id = :documentId")
    suspend fun renameDocument(documentId: Long, newTitle: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE scanned_documents SET folderId = :folderId, updatedAt = :updatedAt WHERE id = :documentId")
    suspend fun moveDocumentToFolder(documentId: Long, folderId: Long?, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE scanned_documents SET ocrText = :ocrText, updatedAt = :updatedAt WHERE id = :documentId")
    suspend fun updateOcrText(documentId: Long, ocrText: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM scanned_documents WHERE id = :documentId")
    suspend fun deleteDocumentById(documentId: Long)

    // Pages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<DocumentPageEntity>)

    @Query("DELETE FROM document_pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Long)

    @Query("DELETE FROM document_pages WHERE id = :pageId")
    suspend fun deletePageById(pageId: Long)

    // Folders
    @Query(
        """
        SELECT f.*, COUNT(d.id) AS documentCount
        FROM folders f
        LEFT JOIN scanned_documents d ON d.folderId = f.id
        GROUP BY f.id
        ORDER BY f.createdAt DESC
        """
    )
    fun observeFoldersWithCount(): Flow<List<FolderWithCount>>

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAllFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :newName, colorArgb = :colorArgb WHERE id = :folderId")
    suspend fun updateFolder(folderId: Long, newName: String, colorArgb: Long)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun deleteFolderById(folderId: Long)
}
