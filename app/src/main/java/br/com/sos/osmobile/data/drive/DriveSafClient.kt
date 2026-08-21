package br.com.sos.osmobile.data.drive

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import br.com.sos.osmobile.data.repository.SettingsRepository

class DriveSafClient(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {
internal fun isDriveRootLike(document: DocumentFile): Boolean {
    val normalized = document.name.orEmpty().trim().lowercase()
    return normalized in setOf("meu drive", "my drive", "drive", "arquivos do drive")
}

internal suspend fun ensureNamedFolder(workOrderId: Long, workOrderFolder: Uri, folderName: String): Uri {
    val key = driveSubFolderKey(workOrderId, folderName)
    val existingFolders = findChildren(workOrderFolder, folderName, DocumentsContract.Document.MIME_TYPE_DIR)
    settingsRepository.getString(key)
        ?.takeIf { it.isNotBlank() && documentExists(it) }
        ?.takeIf { documentDisplayName(it) == folderName }
        ?.takeIf { stored -> existingFolders.any { it.uri.toString() == stored } }
        ?.let { return Uri.parse(it) }
    val folder = existingFolders.firstOrNull()?.uri
        ?: DocumentsContract.createDocument(context.contentResolver, workOrderFolder, DocumentsContract.Document.MIME_TYPE_DIR, folderName)
        ?: error("Nao foi possivel criar pasta $folderName.")
    setSettingIfChanged(key, folder.toString())
    return folder
}

internal suspend fun findExistingNamedFolder(workOrderId: Long, workOrderFolder: Uri, folderName: String): Uri? {
    val existingFolders = findChildren(workOrderFolder, folderName, DocumentsContract.Document.MIME_TYPE_DIR)
    return settingsRepository.getString(driveSubFolderKey(workOrderId, folderName))
        ?.takeIf { it.isNotBlank() && documentExists(it) }
        ?.takeIf { documentDisplayName(it) == folderName }
        ?.takeIf { stored -> existingFolders.any { it.uri.toString() == stored } }
        ?.let(Uri::parse)
        ?: existingFolders.firstOrNull()?.uri
}

internal suspend fun clearStoredSubFolders(workOrderId: Long) {
    setSettingIfChanged(driveSubFolderKey(workOrderId, "Imagens"), "")
    setSettingIfChanged(driveSubFolderKey(workOrderId, "Documentos"), "")
    setSettingIfChanged(driveSubFolderKey(workOrderId, "Assinaturas"), "")
}

internal fun driveSubFolderKey(workOrderId: Long, folderName: String): String =
    "drive_subfolder_${workOrderId}_${folderName.lowercase()}"

internal fun driveWorkOrderRootKey(workOrderId: Long): String =
    "drive_work_order_root_$workOrderId"

internal fun driveWorkOrderPathKey(workOrderId: Long): String =
    "drive_work_order_path_$workOrderId"

internal suspend fun setSettingIfChanged(key: String, value: String) {
    if (settingsRepository.getString(key) != value) {
        settingsRepository.set(key, value)
    }
}

internal fun findOrCreateDirectory(parent: Uri, name: String): Uri =
    findChildDirectory(parent, name)
        ?: DocumentsContract.createDocument(context.contentResolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
        ?: error("Nao foi possivel criar pasta $name.")

internal fun findChildDirectory(parent: Uri, name: String): Uri? =
    findChild(parent, name, DocumentsContract.Document.MIME_TYPE_DIR)

internal fun findChildFile(parent: Uri, name: String): Uri? =
    findChild(parent, name, expectedMimeType = null)

internal fun createFile(parent: Uri, mimeType: String, name: String): Uri? =
    DocumentsContract.createDocument(context.contentResolver, parent, mimeType, name)

internal fun confirmFileInFolder(parent: Uri, fileName: String, expectedMinSize: Long, label: String): Uri {
    val confirmedFile = findChildFile(parent, fileName)
        ?: error("${label.replaceFirstChar { it.uppercase() }} gravado, mas nao confirmado na pasta do Drive.")
    if (!documentExists(confirmedFile.toString())) {
        error("${label.replaceFirstChar { it.uppercase() }} nao confirmado no Drive.")
    }
    val remoteSize = documentSize(confirmedFile)
    if (remoteSize != null && expectedMinSize > 0L && remoteSize < expectedMinSize) {
        error("${label.replaceFirstChar { it.uppercase() }} criado no Drive, mas ainda sem conteudo.")
    }
    return confirmedFile
}

internal fun findChild(parent: Uri, name: String, expectedMimeType: String?): Uri? {
    return findChildren(parent, name, expectedMimeType).firstOrNull()?.uri
}

internal fun findChildren(parent: Uri, name: String? = null, expectedMimeType: String? = null): List<DriveChild> {
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
            val childName = cursor.getString(nameIndex).orEmpty()
            val mimeType = cursor.getString(mimeIndex).orEmpty()
                val nameMatches = name == null || childName == name
            val mimeMatches = expectedMimeType == null || mimeType == expectedMimeType
                if (nameMatches && mimeMatches) {
                    add(
                        DriveChild(
                            name = childName,
                            mimeType = mimeType,
                            uri = DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(idIndex)),
                            sizeBytes = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null,
                            modifiedAt = if (modifiedIndex >= 0 && !cursor.isNull(modifiedIndex)) cursor.getLong(modifiedIndex) else null,
                        ),
                    )
                }
            }
        }
    }.orEmpty()
}

internal fun documentExists(uri: String): Boolean {
    val parsed = Uri.parse(uri)
    val existsByQuery = runCatching {
        context.contentResolver.query(
            parsed,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null,
        )?.use { it.moveToFirst() } == true
    }.getOrDefault(false)
    return existsByQuery ||
        DocumentFile.fromSingleUri(context, parsed)?.exists() == true ||
        DocumentFile.fromTreeUri(context, parsed)?.exists() == true
}

internal fun documentDisplayName(uri: String): String? {
    val parsed = Uri.parse(uri)
    val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
    return runCatching {
        context.contentResolver.query(parsed, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            if (nameIndex < 0 || cursor.isNull(nameIndex)) null else cursor.getString(nameIndex)
        }
    }.getOrNull()
        ?: DocumentFile.fromSingleUri(context, parsed)?.name
        ?: DocumentFile.fromTreeUri(context, parsed)?.name
}

internal fun documentSize(uri: Uri): Long? {
    val projection = arrayOf(DocumentsContract.Document.COLUMN_SIZE)
    return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
        if (sizeIndex < 0 || cursor.isNull(sizeIndex)) null else cursor.getLong(sizeIndex)
    }
}

internal fun sanitizeName(value: String): String =
    value.replace(Regex("[\\\\/:*?\"<>|]"), "-").trim().take(80).ifBlank { "Sem nome" }

internal fun sanitizeFileName(value: String): String =
    value
        .replace(Regex("[\\\\/:*?\"<>|]"), "-")
        .replace(Regex("\\s+"), "_")
        .trim('.', '_', ' ')
        .take(90)
        .ifBlank { "arquivo" }

internal fun mimeTypeFromFileName(fileName: String): String =
    when (fileName.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        "svg" -> "image/svg+xml"
        "ai" -> "application/postscript"
        "cdr" -> "application/octet-stream"
        else -> "application/octet-stream"
    }

internal fun isDocumentAttachment(fileName: String): Boolean =
    fileName.startsWith("documento_") || fileName.startsWith("comprovante_")

internal fun isDesignAttachment(fileName: String): Boolean =
    fileName.startsWith("design_") ||
        (fileName.startsWith("documento_") && fileName.split("_", limit = 4).getOrNull(2) == DRIVE_DESIGN_FOLDER)


    private companion object {
        const val DRIVE_DESIGN_FOLDER = "Design"
    }
}
