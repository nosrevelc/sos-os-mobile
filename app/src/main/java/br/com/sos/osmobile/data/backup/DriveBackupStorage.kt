package br.com.sos.osmobile.data.backup

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.DRIVE_ROOT_URI_KEY

class DriveBackupStorage(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
internal suspend fun driveRootFolderUri(): Uri {
    val uri = settingsRepository.getString(DRIVE_ROOT_URI_KEY)?.takeIf { it.isNotBlank() }
        ?: error("Configure primeiro a pasta do Google Drive em Configuracoes.")
    val treeUri = Uri.parse(uri)
    return DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
}

internal fun findOrCreateDirectory(parent: Uri, name: String): Uri =
    findChild(parent, name, DocumentsContract.Document.MIME_TYPE_DIR)
        ?: DocumentsContract.createDocument(context.contentResolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
        ?: error("Nao foi possivel criar pasta $name no Drive.")

internal fun findChildFile(parent: Uri, name: String): Uri? =
    findChild(parent, name, expectedMimeType = null)

internal fun findChild(parent: Uri, name: String, expectedMimeType: String?): Uri? {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )
    return context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
        var found: Uri? = null
        while (found == null && cursor.moveToNext()) {
            val childName = cursor.getString(nameIndex).orEmpty()
            val mimeType = cursor.getString(mimeIndex).orEmpty()
            val mimeMatches = expectedMimeType == null || mimeType == expectedMimeType
            if (childName == name && mimeMatches) {
                found = DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(idIndex))
            }
        }
        found
    }
}

internal fun listChildFiles(parent: Uri): List<DriveBackupFile> {
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )
    return context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
        val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
        val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
        val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        buildList {
            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeIndex).orEmpty()
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) continue
                add(
                    DriveBackupFile(
                        name = cursor.getString(nameIndex).orEmpty(),
                        uri = DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(idIndex)).toString(),
                        sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null,
                        modifiedAt = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else null,
                    ),
                )
            }
        }
    }.orEmpty()
}

internal fun documentSize(uri: Uri): Long? {
    val projection = arrayOf(DocumentsContract.Document.COLUMN_SIZE)
    return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
        if (sizeIndex < 0 || cursor.isNull(sizeIndex)) null else cursor.getLong(sizeIndex)
    }
}
}
