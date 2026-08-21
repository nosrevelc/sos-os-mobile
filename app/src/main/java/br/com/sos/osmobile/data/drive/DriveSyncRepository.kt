package br.com.sos.osmobile.data.drive

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.WorkOrderDao
import br.com.sos.osmobile.data.local.dao.WorkOrderPhotoDao
import br.com.sos.osmobile.data.local.dao.WorkOrderSignatureDao
import br.com.sos.osmobile.data.local.entity.DriveSyncStatus
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.DRIVE_ROOT_URI_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.DRIVE_SYNC_ENABLED_KEY
import java.io.File
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class DriveSyncRepository(
    private val context: Context,
    private val workOrderDao: WorkOrderDao,
    private val photoDao: WorkOrderPhotoDao,
    private val signatureDao: WorkOrderSignatureDao,
    private val settingsRepository: SettingsRepository,
    private val auditRepository: AuditRepository,
) {
    private val syncMutex = Mutex()
    private val saf = DriveSafClient(context, settingsRepository)

    suspend fun syncAllPending(): DriveSyncResult = syncMutex.withLock {
        if (!isConfigured()) return DriveSyncResult.Skipped("Drive nao configurado.")
        if (!isOnline()) return DriveSyncResult.Skipped("Sem internet. Sync pendente.")
        revalidateSyncedItems()
        var synced = 0
        val failures = mutableListOf<String>()
        val pendingWorkOrderIds = linkedSetOf<Long>()
        workOrderDao.listPendingDriveSync().forEach { pendingWorkOrderIds += it.id }
        photoDao.listPendingDriveSync().forEach { pendingWorkOrderIds += it.workOrderId }
        signatureDao.listPendingDriveSync().forEach { pendingWorkOrderIds += it.workOrderId }
        pendingWorkOrderIds.forEach { workOrderId ->
            syncWorkOrderLocked(workOrderId).fold(
                onSuccess = { synced++ },
                onFailure = { failures += it.message ?: "Falha ao sincronizar OS $workOrderId." },
            )
        }
        return DriveSyncResult.Done(
            syncedItems = synced,
            failedItems = failures.size,
            firstError = failures.firstOrNull(),
        )
    }

    suspend fun syncWorkOrder(workOrderId: Long): Result<Unit> = syncMutex.withLock {
        syncWorkOrderLocked(workOrderId)
    }

    suspend fun smartSyncWorkOrder(workOrderId: Long): Result<DriveSmartSyncResult> = syncMutex.withLock {
        val workOrder = workOrderDao.findById(workOrderId)
            ?: return Result.failure(IllegalArgumentException("OS nao encontrada."))
        val mustRebuild = workOrder.driveFolderUri.isNullOrBlank() || !saf.documentExists(workOrder.driveFolderUri)
        val result = if (mustRebuild) rebuildWorkOrderSyncLocked(workOrderId) else syncWorkOrderLocked(workOrderId)
        result.map {
            DriveSmartSyncResult(
                rebuilt = mustRebuild,
                warning = workOrderDriveWarningLocked(workOrderId),
            )
        }
    }

    suspend fun listDesignImportCandidates(workOrderId: Long): Result<List<DriveDesignImportCandidate>> = syncMutex.withLock {
        val workOrder = workOrderDao.findById(workOrderId)
            ?: return Result.failure(IllegalArgumentException("OS nao encontrada."))
        if (!isConfigured()) return Result.failure(IllegalStateException("Drive nao configurado."))
        if (!isOnline()) return Result.failure(IllegalStateException("Sem internet."))
        runCatching {
            val workOrderFolder = ensureWorkOrderFolders(workOrder)
            markWorkOrder(workOrder, DriveSyncStatus.SYNCED, null, workOrderFolder.toString())
            val designFolder = saf.ensureNamedFolder(workOrder.id, workOrderFolder, DRIVE_DESIGN_FOLDER)
            val existingPhotos = photoDao.listByWorkOrderAsc(workOrder.id)
            saf.findChildren(designFolder)
                .filter { it.mimeType != DocumentsContract.Document.MIME_TYPE_DIR }
                .filterNot { file -> isDesignAlreadyImported(existingPhotos, file) }
                .map { file ->
                    DriveDesignImportCandidate(
                        name = file.name,
                        uri = file.uri.toString(),
                        sizeBytes = file.sizeBytes,
                        modifiedAt = file.modifiedAt,
                    )
                }
        }
    }

    suspend fun importDesignFiles(workOrderId: Long, selectedUris: Set<String>? = null): Result<DriveImportResult> = syncMutex.withLock {
        val workOrder = workOrderDao.findById(workOrderId)
            ?: return Result.failure(IllegalArgumentException("OS nao encontrada."))
        if (!isConfigured()) return Result.failure(IllegalStateException("Drive nao configurado."))
        if (!isOnline()) return Result.failure(IllegalStateException("Sem internet."))
        runCatching {
            val workOrderFolder = ensureWorkOrderFolders(workOrder)
            markWorkOrder(workOrder, DriveSyncStatus.SYNCED, null, workOrderFolder.toString())
            val designFolder = saf.ensureNamedFolder(workOrder.id, workOrderFolder, DRIVE_DESIGN_FOLDER)
            val existingPhotos = photoDao.listByWorkOrderAsc(workOrder.id)
            val remoteFiles = saf.findChildren(designFolder)
                .filter { it.mimeType != DocumentsContract.Document.MIME_TYPE_DIR }
                .filter { selectedUris == null || it.uri.toString() in selectedUris }
            var imported = 0
            var alreadyImported = 0
            remoteFiles.forEach { file ->
                if (isDesignAlreadyImported(existingPhotos, file)) {
                    alreadyImported++
                } else {
                    importDriveFileAsDocument(workOrder.id, file)
                    imported++
                }
            }
            auditRepository.record(
                module = "Google Drive",
                action = "Arquivos de design importados",
                table = "ordens_servico",
                recordId = workOrder.id,
                details = "importados=$imported, existentes=$alreadyImported",
            )
            DriveImportResult(
                folderName = DRIVE_DESIGN_FOLDER,
                foundFiles = remoteFiles.size,
                importedFiles = imported,
                alreadyImportedFiles = alreadyImported,
            )
        }
    }

    private fun isDesignAlreadyImported(existingPhotos: List<WorkOrderPhotoEntity>, file: DriveChild): Boolean =
        existingPhotos.any {
            it.driveFileUri == file.uri.toString() ||
                saf.isDesignAttachment(it.fileName) && it.fileName.endsWith("_${saf.sanitizeFileName(file.name)}")
        }

    private suspend fun syncWorkOrderLocked(workOrderId: Long): Result<Unit> {
        val workOrder = workOrderDao.findById(workOrderId) ?: return Result.failure(IllegalArgumentException("OS nao encontrada."))
        if (!isConfigured()) {
            markWorkOrder(workOrder, DriveSyncStatus.NOT_CONFIGURED, "Configure a pasta do Drive.")
            return Result.failure(IllegalStateException("Drive nao configurado."))
        }
        if (!isOnline()) {
            markWorkOrder(workOrder, DriveSyncStatus.PENDING, "Sem internet.")
            return Result.failure(IllegalStateException("Sem internet."))
        }
        return runCatching {
            if (workOrder.driveFolderUri != null && !saf.documentExists(workOrder.driveFolderUri)) {
                workOrderDao.resetDriveSync(workOrder.id, Clock.nowMillis())
                photoDao.resetDriveSyncByWorkOrder(workOrder.id)
                signatureDao.resetDriveSyncByWorkOrder(workOrder.id)
                saf.clearStoredSubFolders(workOrder.id)
            }
            val folders = syncWorkOrderFolder(workOrder)
            val failures = photoDao.listPendingDriveSyncByWorkOrder(workOrder.id)
                .filterNot { saf.isDesignAttachment(it.fileName) }
                .mapNotNull { syncPhoto(it, folders).exceptionOrNull()?.message }
                .toMutableList()
            signatureDao.findByWorkOrder(workOrder.id)
                ?.takeUnless { it.driveSyncStatus == DriveSyncStatus.SYNCED && !it.driveFileUri.isNullOrBlank() && saf.documentExists(it.driveFileUri) }
                ?.let { signature ->
                    syncSignature(signature, folders).exceptionOrNull()?.message?.let(failures::add)
                }
            if (failures.isNotEmpty()) {
                error(failures.first())
            }
            auditRepository.record("Google Drive", "Pasta da OS sincronizada", "ordens_servico", workOrder.id, details = workOrder.numero)
        }.onFailure {
            markWorkOrder(workOrder, DriveSyncStatus.ERROR, it.message ?: "Falha ao sincronizar.")
        }
    }

    suspend fun rebuildWorkOrderSync(workOrderId: Long): Result<Unit> = syncMutex.withLock {
        rebuildWorkOrderSyncLocked(workOrderId)
    }

    private suspend fun rebuildWorkOrderSyncLocked(workOrderId: Long): Result<Unit> {
        val workOrder = workOrderDao.findById(workOrderId) ?: return Result.failure(IllegalArgumentException("OS nao encontrada."))
        workOrderDao.resetDriveSync(workOrder.id, Clock.nowMillis())
        photoDao.resetDriveSyncByWorkOrder(workOrder.id)
        signatureDao.resetDriveSyncByWorkOrder(workOrder.id)
        saf.clearStoredSubFolders(workOrder.id)
        if (!isConfigured()) {
            markWorkOrder(workOrder, DriveSyncStatus.NOT_CONFIGURED, "Configure a pasta do Drive.", null)
            return Result.failure(IllegalStateException("Drive nao configurado."))
        }
        if (!isOnline()) {
            markWorkOrder(workOrder, DriveSyncStatus.PENDING, "Sem internet.", null)
            return Result.failure(IllegalStateException("Sem internet."))
        }
        return runCatching {
            val folders = syncWorkOrderFolder(workOrder)
            val failures = photoDao.listByWorkOrderAsc(workOrder.id)
                .filterNot { saf.isDesignAttachment(it.fileName) }
                .mapNotNull { syncPhoto(it, folders).exceptionOrNull()?.message }
                .toMutableList()
            signatureDao.findByWorkOrder(workOrder.id)
                ?.let { signature ->
                    syncSignature(signature, folders).exceptionOrNull()?.message?.let(failures::add)
                }
            if (failures.isNotEmpty()) {
                error(failures.first())
            }
            auditRepository.record("Google Drive", "Sincronizacao da OS refeita", "ordens_servico", workOrder.id, details = workOrder.numero)
        }.onFailure {
            markWorkOrder(workOrder, DriveSyncStatus.ERROR, it.message ?: "Falha ao refazer sincronizacao.", null)
        }
    }

    suspend fun syncPhoto(photoId: Long): Result<Unit> = syncMutex.withLock {
        val photo = photoDao.findById(photoId) ?: return Result.failure(IllegalArgumentException("Anexo nao encontrado."))
        return syncPhoto(photo)
    }

    suspend fun syncSignature(workOrderId: Long): Result<Unit> = syncMutex.withLock {
        val signature = signatureDao.findByWorkOrder(workOrderId) ?: return Result.failure(IllegalArgumentException("Assinatura nao encontrada."))
        val workOrder = workOrderDao.findById(workOrderId) ?: return Result.failure(IllegalArgumentException("OS nao encontrada."))
        syncSignature(signature, syncWorkOrderFolder(workOrder))
    }

    private suspend fun syncPhoto(photo: WorkOrderPhotoEntity, folders: WorkOrderDriveFolders? = null): Result<Unit> {
        if (!isConfigured()) {
            photoDao.updateDriveSync(photo.id, photo.driveFileUri, DriveSyncStatus.NOT_CONFIGURED, "Configure a pasta do Drive.")
            return Result.failure(IllegalStateException("Drive nao configurado."))
        }
        if (!isOnline()) {
            photoDao.updateDriveSync(photo.id, photo.driveFileUri, DriveSyncStatus.PENDING, "Sem internet.")
            return Result.failure(IllegalStateException("Sem internet."))
        }
        return runCatching {
            if (saf.isDesignAttachment(photo.fileName)) {
                if (!photo.driveFileUri.isNullOrBlank() && saf.documentExists(photo.driveFileUri)) {
                    photoDao.updateDriveSync(photo.id, photo.driveFileUri, DriveSyncStatus.SYNCED, null)
                    return@runCatching
                }
                error("Arquivo Design importado do Drive nao deve ser reenviado para Documentos ou Imagens.")
            }
            val workOrder = workOrderDao.findById(photo.workOrderId) ?: error("OS nao encontrada.")
            val driveFolders = folders ?: syncWorkOrderFolder(workOrder)
            val localFile = File(context.filesDir, photo.relativePath)
                .takeIf { it.exists() && it.length() > 0L }
                ?: error("Arquivo local do anexo nao encontrado.")
            val targetFolder = if (saf.isDocumentAttachment(photo.fileName)) {
                driveFolders.documents ?: saf.ensureNamedFolder(photo.workOrderId, driveFolders.workOrder, "Documentos")
                    .also { driveFolders.documents = it }
            } else {
                driveFolders.images ?: saf.ensureNamedFolder(photo.workOrderId, driveFolders.workOrder, "Imagens")
                    .also { driveFolders.images = it }
            }
            val targetFile = photo.driveFileUri
                ?.takeIf { it.isNotBlank() && saf.documentExists(it) }
                ?.let(Uri::parse)
                ?: saf.findChildFile(targetFolder, photo.fileName)
                ?: saf.createFile(targetFolder, photo.mimeType, photo.fileName)
                ?: error("Nao foi possivel criar arquivo no Drive.")
            localFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(targetFile, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: error("Nao foi possivel escrever no Drive.")
            }
            val confirmedFile = saf.confirmFileInFolder(targetFolder, photo.fileName, localFile.length(), "anexo")
            photoDao.updateDriveSync(photo.id, confirmedFile.toString(), DriveSyncStatus.SYNCED, null)
            auditRepository.record("Google Drive", "Anexo sincronizado", "ordens_servico", photo.workOrderId, details = photo.fileName)
        }.onFailure {
            photoDao.updateDriveSync(photo.id, null, DriveSyncStatus.ERROR, it.message ?: "Falha ao sincronizar anexo.")
        }
    }

    private suspend fun syncSignature(signature: WorkOrderSignatureEntity, folders: WorkOrderDriveFolders): Result<Unit> {
        if (!isConfigured()) {
            signatureDao.updateDriveSync(signature.id, signature.driveFileUri, DriveSyncStatus.NOT_CONFIGURED, "Configure a pasta do Drive.")
            return Result.failure(IllegalStateException("Drive nao configurado."))
        }
        if (!isOnline()) {
            signatureDao.updateDriveSync(signature.id, signature.driveFileUri, DriveSyncStatus.PENDING, "Sem internet.")
            return Result.failure(IllegalStateException("Sem internet."))
        }
        return runCatching {
            val localFile = File(context.filesDir, signature.relativePath)
                .takeIf { it.exists() && it.length() > 0L }
                ?: error("Arquivo local da assinatura nao encontrado.")
            val targetFolder = folders.signatures ?: saf.ensureNamedFolder(signature.workOrderId, folders.workOrder, "Assinaturas")
                .also { folders.signatures = it }
            val targetFile = signature.driveFileUri
                ?.takeIf { it.isNotBlank() && saf.documentExists(it) }
                ?.let(Uri::parse)
                ?: saf.findChildFile(targetFolder, signature.fileName)
                ?: saf.createFile(targetFolder, "image/png", signature.fileName)
                ?: error("Nao foi possivel criar assinatura no Drive.")
            localFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(targetFile, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: error("Nao foi possivel escrever assinatura no Drive.")
            }
            val confirmedFile = saf.confirmFileInFolder(targetFolder, signature.fileName, localFile.length(), "assinatura")
            signatureDao.updateDriveSync(signature.id, confirmedFile.toString(), DriveSyncStatus.SYNCED, null)
            auditRepository.record("Google Drive", "Assinatura sincronizada", "ordens_servico", signature.workOrderId, details = signature.fileName)
        }.onFailure {
            signatureDao.updateDriveSync(signature.id, null, DriveSyncStatus.ERROR, it.message ?: "Falha ao sincronizar assinatura.")
        }
    }

    private suspend fun importDriveFileAsDocument(workOrderId: Long, file: DriveChild) {
        val now = Clock.nowMillis()
        val originalName = saf.sanitizeFileName(file.name)
        val mimeType = file.mimeType.ifBlank { saf.mimeTypeFromFileName(originalName) }
        val targetName = "design_${now}_$originalName"
        val relativePath = "work_order_photos/$workOrderId/$targetName"
        val targetFile = File(context.filesDir, relativePath).apply { parentFile?.mkdirs() }
        context.contentResolver.openInputStream(file.uri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Nao foi possivel abrir ${file.name} no Drive.")
        photoDao.insert(
            WorkOrderPhotoEntity(
                workOrderId = workOrderId,
                fileName = targetName,
                relativePath = relativePath,
                mimeType = mimeType,
                driveFileUri = file.uri.toString(),
                driveSyncStatus = DriveSyncStatus.SYNCED,
                driveSyncError = null,
                createdAt = now,
            ),
        )
    }

    private suspend fun revalidateSyncedItems() {
        workOrderDao.listAll()
            .filter { it.driveSyncStatus == DriveSyncStatus.SYNCED && it.driveFolderUri != null && !saf.documentExists(it.driveFolderUri) }
            .forEach {
            workOrderDao.resetDriveSync(it.id, Clock.nowMillis())
            photoDao.resetDriveSyncByWorkOrder(it.id)
            signatureDao.resetDriveSyncByWorkOrder(it.id)
        }
        photoDao.listAll()
            .filter { it.driveSyncStatus == DriveSyncStatus.SYNCED && it.driveFileUri != null && !saf.documentExists(it.driveFileUri) }
            .forEach { photoDao.updateDriveSync(it.id, null, DriveSyncStatus.PENDING, "Arquivo nao encontrado no Drive.") }
        signatureDao.listAll()
            .filter { it.driveSyncStatus == DriveSyncStatus.SYNCED && it.driveFileUri != null && !saf.documentExists(it.driveFileUri) }
            .forEach { signatureDao.updateDriveSync(it.id, null, DriveSyncStatus.PENDING, "Assinatura nao encontrada no Drive.") }
    }

    suspend fun validateWorkOrderFiles(workOrderId: Long): DriveValidationResult = syncMutex.withLock {
        if (!isConfigured() || !isOnline()) return DriveValidationResult(checked = false, missingItems = 0)
        val workOrder = workOrderDao.findById(workOrderId) ?: return DriveValidationResult(checked = false, missingItems = 0)
        var missing = 0

        val workOrderFolder = findExpectedWorkOrderFolder(workOrder)
        if (workOrderFolder == null) {
            workOrderDao.resetDriveSync(workOrder.id, Clock.nowMillis())
            photoDao.resetDriveSyncByWorkOrder(workOrder.id)
            signatureDao.resetDriveSyncByWorkOrder(workOrder.id)
            saf.clearStoredSubFolders(workOrder.id)
                val signatureMissing = if (signatureDao.findByWorkOrder(workOrder.id) != null) 1 else 0
                val uploadablePhotoMissing = photoDao.listByWorkOrderAsc(workOrder.id).count { !saf.isDesignAttachment(it.fileName) }
                return DriveValidationResult(
                    checked = true,
                    missingItems = 1 + uploadablePhotoMissing + signatureMissing,
                )
            }

        photoDao.listByWorkOrderAsc(workOrderId)
            .filter { it.driveSyncStatus == DriveSyncStatus.SYNCED }
            .forEach { photo ->
                val fileExists = drivePhotoExistsInExpectedFolder(workOrderFolder, photo)
                if (!fileExists) {
                    missing++
                    photoDao.updateDriveSync(photo.id, null, DriveSyncStatus.PENDING, "Arquivo nao encontrado no Drive.")
                }
            }

        signatureDao.findByWorkOrder(workOrderId)
            ?.takeIf { it.driveSyncStatus == DriveSyncStatus.SYNCED }
            ?.let { signature ->
                val fileExists = driveSignatureExistsInExpectedFolder(workOrderFolder, signature)
                if (!fileExists) {
                    missing++
                    signatureDao.updateDriveSync(signature.id, null, DriveSyncStatus.PENDING, "Assinatura nao encontrada no Drive.")
                }
            }

        DriveValidationResult(checked = true, missingItems = missing)
    }

    private suspend fun findExpectedWorkOrderFolder(workOrder: WorkOrderEntity): Uri? {
        val root = rootFolder() ?: return null
        val summary = workOrderDao.findSummaryById(workOrder.id)
        val customerName = saf.sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerPhone = saf.sanitizeName(summary?.customerPhone.orEmpty().filter { it.isDigit() })
        val customerFolderName = if (customerPhone.isBlank()) customerName else "${customerName}_$customerPhone"
        val osFolderName = "OS-${saf.sanitizeName(workOrder.numero)}"
        val expectedPath = "$customerFolderName/$osFolderName"
        val storedRoot = settingsRepository.getString(saf.driveWorkOrderRootKey(workOrder.id)).orEmpty()
        val storedPath = settingsRepository.getString(saf.driveWorkOrderPathKey(workOrder.id)).orEmpty()
        workOrder.driveFolderUri
            ?.takeIf { it.isNotBlank() && saf.documentExists(it) }
            ?.takeIf { storedRoot == root.treeUri.toString() }
            ?.takeIf { storedPath == expectedPath }
            ?.takeIf { saf.documentDisplayName(it) == osFolderName }
            ?.let { return Uri.parse(it) }
        val customerFolder = saf.findChildDirectory(root.folderUri, customerFolderName) ?: return null
        return saf.findChildDirectory(customerFolder, osFolderName)
    }

    private suspend fun drivePhotoExistsInExpectedFolder(workOrderFolder: Uri, photo: WorkOrderPhotoEntity): Boolean {
        val folderName = when {
            saf.isDesignAttachment(photo.fileName) -> DRIVE_DESIGN_FOLDER
            saf.isDocumentAttachment(photo.fileName) -> "Documentos"
            else -> "Imagens"
        }
        val targetFolder = saf.findExistingNamedFolder(photo.workOrderId, workOrderFolder, folderName) ?: return false
        return saf.findChildFile(targetFolder, photo.fileName) != null
            || photo.driveFileUri?.takeIf { it.isNotBlank() }?.let { saf.documentExists(it) } == true
    }

    private suspend fun driveSignatureExistsInExpectedFolder(workOrderFolder: Uri, signature: WorkOrderSignatureEntity): Boolean {
        val targetFolder = saf.findExistingNamedFolder(signature.workOrderId, workOrderFolder, "Assinaturas") ?: return false
        return saf.findChildFile(targetFolder, signature.fileName) != null
    }

    private suspend fun ensureWorkOrderFolders(workOrder: WorkOrderEntity): Uri {
        val root = rootFolder() ?: error("Selecione uma pasta dentro do Drive. A raiz Meu Drive nao deve ser usada.")
        val summary = workOrderDao.findSummaryById(workOrder.id)
        val customerName = saf.sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerPhone = saf.sanitizeName(summary?.customerPhone.orEmpty().filter { it.isDigit() })
        val customerFolderName = if (customerPhone.isBlank()) customerName else "${customerName}_$customerPhone"
        val osFolderName = "OS-${saf.sanitizeName(workOrder.numero)}"
        val expectedPath = "$customerFolderName/$osFolderName"
        val storedRoot = settingsRepository.getString(saf.driveWorkOrderRootKey(workOrder.id)).orEmpty()
        val storedPath = settingsRepository.getString(saf.driveWorkOrderPathKey(workOrder.id)).orEmpty()
        workOrder.driveFolderUri
            ?.takeIf { it.isNotBlank() && saf.documentExists(it) }
            ?.takeIf { storedRoot == root.treeUri.toString() }
            ?.takeIf { storedPath == expectedPath }
            ?.takeIf { saf.documentDisplayName(it) == osFolderName }
            ?.let { storedFolderUri ->
                saf.setSettingIfChanged(saf.driveWorkOrderRootKey(workOrder.id), root.treeUri.toString())
                saf.setSettingIfChanged(saf.driveWorkOrderPathKey(workOrder.id), expectedPath)
                return Uri.parse(storedFolderUri)
            }

        val customerFolder = saf.findOrCreateDirectory(root.folderUri, customerFolderName)
        val osFolder = saf.findOrCreateDirectory(customerFolder, osFolderName)
        if (workOrder.driveFolderUri != osFolder.toString()) {
            saf.clearStoredSubFolders(workOrder.id)
        }
        saf.setSettingIfChanged(saf.driveWorkOrderRootKey(workOrder.id), root.treeUri.toString())
        saf.setSettingIfChanged(saf.driveWorkOrderPathKey(workOrder.id), expectedPath)
        return osFolder
    }

    private suspend fun syncWorkOrderFolder(workOrder: WorkOrderEntity): WorkOrderDriveFolders {
        val workOrderFolder = ensureWorkOrderFolders(workOrder)
        val folders = WorkOrderDriveFolders(
            workOrder = workOrderFolder,
        )
        markWorkOrder(workOrder, DriveSyncStatus.SYNCED, null, folders.workOrder.toString())
        return folders
    }

    private suspend fun rootFolder(): DriveRoot? {
        val uri = settingsRepository.getString(DRIVE_ROOT_URI_KEY)?.takeIf { it.isNotBlank() } ?: return null
        val treeUri = Uri.parse(uri)
        DocumentFile.fromTreeUri(context, treeUri)
            ?.takeIf { it.canWrite() }
            ?.takeUnless { saf.isDriveRootLike(it) }
            ?: return null
        return DriveRoot(
            treeUri = treeUri,
            folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri)),
        )
    }

    suspend fun resetAllDriveSyncReferences() = syncMutex.withLock {
        workOrderDao.listAll().forEach { workOrder ->
            workOrderDao.resetDriveSync(workOrder.id, Clock.nowMillis())
            photoDao.resetDriveSyncByWorkOrder(workOrder.id)
            signatureDao.resetDriveSyncByWorkOrder(workOrder.id)
            saf.clearStoredSubFolders(workOrder.id)
            saf.setSettingIfChanged(saf.driveWorkOrderRootKey(workOrder.id), "")
            saf.setSettingIfChanged(saf.driveWorkOrderPathKey(workOrder.id), "")
        }
    }

    suspend fun buildWorkOrderDebugReport(workOrderId: Long): String = syncMutex.withLock {
        val workOrder = workOrderDao.findById(workOrderId) ?: return "Drive debug: OS $workOrderId nao encontrada."
        val summary = workOrderDao.findSummaryById(workOrder.id)
        val root = rootFolder()
        val customerName = saf.sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerPhone = saf.sanitizeName(summary?.customerPhone.orEmpty().filter { it.isDigit() })
        val customerFolderName = if (customerPhone.isBlank()) customerName else "${customerName}_$customerPhone"
        val osFolderName = "OS-${saf.sanitizeName(workOrder.numero)}"
        val expectedPath = "$customerFolderName/$osFolderName"
        val storedRoot = settingsRepository.getString(saf.driveWorkOrderRootKey(workOrder.id)).orEmpty()
        val storedPath = settingsRepository.getString(saf.driveWorkOrderPathKey(workOrder.id)).orEmpty()
        val storedImages = settingsRepository.getString(saf.driveSubFolderKey(workOrder.id, "Imagens")).orEmpty()
        val storedDocuments = settingsRepository.getString(saf.driveSubFolderKey(workOrder.id, "Documentos")).orEmpty()
        val storedSignatures = settingsRepository.getString(saf.driveSubFolderKey(workOrder.id, "Assinaturas")).orEmpty()
        val photos = photoDao.listByWorkOrderAsc(workOrder.id)
        val signature = signatureDao.findByWorkOrder(workOrder.id)

        buildString {
            appendLine("OS Mobile Drive Debug")
            appendLine("OS id: ${workOrder.id}")
            appendLine("OS numero: ${workOrder.numero}")
            appendLine("Cliente esperado: $customerFolderName")
            appendLine("Pasta OS esperada: $osFolderName")
            appendLine("Path esperado: $expectedPath")
            appendLine("Status OS: ${workOrder.driveSyncStatus}")
            appendLine("Erro OS: ${workOrder.driveSyncError.orEmpty()}")
            appendLine("URI OS salva: ${workOrder.driveFolderUri.orEmpty()}")
            appendLine("URI OS existe: ${workOrder.driveFolderUri?.let { saf.documentExists(it) } ?: false}")
            appendLine("Root configurado: ${root?.treeUri ?: "nao configurado/sem escrita"}")
            appendLine("Root salvo na OS: $storedRoot")
            appendLine("Path salvo na OS: $storedPath")
            appendLine("Subpasta Imagens salva: $storedImages | existe=${storedImages.takeIf { it.isNotBlank() }?.let { saf.documentExists(it) } ?: false}")
            appendLine("Subpasta Documentos salva: $storedDocuments | existe=${storedDocuments.takeIf { it.isNotBlank() }?.let { saf.documentExists(it) } ?: false}")
            appendLine("Subpasta Assinaturas salva: $storedSignatures | existe=${storedSignatures.takeIf { it.isNotBlank() }?.let { saf.documentExists(it) } ?: false}")
            if (root == null) {
                appendLine("Pastas no Drive: root indisponivel.")
            } else {
                val customerFolders = saf.findChildren(root.folderUri, customerFolderName, DocumentsContract.Document.MIME_TYPE_DIR)
                appendLine("Pastas cliente com mesmo nome: ${customerFolders.size}")
                customerFolders.forEachIndexed { index, customer ->
                    appendLine("Cliente[$index]: ${customer.uri}")
                    val osFolders = saf.findChildren(customer.uri, osFolderName, DocumentsContract.Document.MIME_TYPE_DIR)
                    appendLine("  Pastas OS com mesmo nome: ${osFolders.size}")
                    osFolders.forEachIndexed { osIndex, os ->
                        appendLine("  OS[$osIndex]: ${os.uri}")
                        val children = saf.findChildren(os.uri, expectedMimeType = DocumentsContract.Document.MIME_TYPE_DIR)
                        appendLine("    Subpastas: ${children.joinToString { it.name }}")
                        appendLine("    Qtd Documentos: ${children.count { it.name == "Documentos" }}")
                        appendLine("    Qtd Imagens: ${children.count { it.name == "Imagens" }}")
                        appendLine("    Qtd Assinaturas: ${children.count { it.name == "Assinaturas" }}")
                    }
                }
            }
            appendLine("Anexos locais: ${photos.size}")
            photos.forEach {
                appendLine("Anexo id=${it.id} nome=${it.fileName} status=${it.driveSyncStatus} erro=${it.driveSyncError.orEmpty()} uri=${it.driveFileUri.orEmpty()}")
            }
            appendLine("Assinatura: ${signature?.let { "id=${it.id} nome=${it.fileName} status=${it.driveSyncStatus} erro=${it.driveSyncError.orEmpty()} uri=${it.driveFileUri.orEmpty()}" } ?: "nenhuma"}")
        }
    }

    suspend fun workOrderDriveWarning(workOrderId: Long): String? = syncMutex.withLock {
        workOrderDriveWarningLocked(workOrderId)
    }

    private suspend fun workOrderDriveWarningLocked(workOrderId: Long): String? {
        val workOrder = workOrderDao.findById(workOrderId) ?: return null
        val root = rootFolder() ?: return "Drive nao configurado ou sem permissao de escrita."
        val summary = workOrderDao.findSummaryById(workOrder.id)
        val customerName = saf.sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerPhone = saf.sanitizeName(summary?.customerPhone.orEmpty().filter { it.isDigit() })
        val customerFolderName = if (customerPhone.isBlank()) customerName else "${customerName}_$customerPhone"
        val osFolderName = "OS-${saf.sanitizeName(workOrder.numero)}"
        val customerFolders = saf.findChildren(root.folderUri, customerFolderName, DocumentsContract.Document.MIME_TYPE_DIR)
        val duplicateOsCount = customerFolders.sumOf { customer ->
            saf.findChildren(customer.uri, osFolderName, DocumentsContract.Document.MIME_TYPE_DIR).size
        }
        val duplicateSubfolders = workOrder.driveFolderUri
            ?.takeIf { it.isNotBlank() && saf.documentExists(it) }
            ?.let { Uri.parse(it) }
            ?.let { osFolder ->
                listOf("Documentos", "Imagens", "Assinaturas")
                    .map { folderName -> folderName to saf.findChildren(osFolder, folderName, DocumentsContract.Document.MIME_TYPE_DIR).size }
                    .filter { (_, count) -> count > 1 }
            }
            .orEmpty()
        return when {
            customerFolders.size > 1 -> "Atencao: existem ${customerFolders.size} pastas do mesmo cliente no Drive."
            duplicateOsCount > 1 -> "Atencao: existem $duplicateOsCount pastas da OS ${workOrder.numero} no Drive. Use o debug para identificar e remova a duplicada manualmente."
            duplicateSubfolders.isNotEmpty() -> "Atencao: subpastas duplicadas no Drive: ${duplicateSubfolders.joinToString { "${it.first}=${it.second}" }}. Remova as duplicadas vazias manualmente."
            else -> null
        }
    }

    private suspend fun isConfigured(): Boolean =
        settingsRepository.getString(DRIVE_SYNC_ENABLED_KEY)?.toBooleanStrictOrNull() == true &&
            !settingsRepository.getString(DRIVE_ROOT_URI_KEY).isNullOrBlank()

    private fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun markWorkOrder(workOrder: WorkOrderEntity, status: String, error: String?, folderUri: String? = workOrder.driveFolderUri) {
        workOrderDao.updateDriveSync(workOrder.id, folderUri, status, error, Clock.nowMillis())
    }
    private companion object {
        const val DRIVE_DESIGN_FOLDER = "Design"
    }
}


internal data class WorkOrderDriveFolders(
    val workOrder: Uri,
    var images: Uri? = null,
    var documents: Uri? = null,
    var signatures: Uri? = null,
)

private data class DriveRoot(
    val treeUri: Uri,
    val folderUri: Uri,
)

internal data class DriveChild(
    val name: String,
    val mimeType: String,
    val uri: Uri,
    val sizeBytes: Long?,
    val modifiedAt: Long?,
)

sealed class DriveSyncResult {
    data class Done(
        val syncedItems: Int,
        val failedItems: Int = 0,
        val firstError: String? = null,
    ) : DriveSyncResult()
    data class Skipped(val reason: String) : DriveSyncResult()
}

data class DriveSmartSyncResult(
    val rebuilt: Boolean,
    val warning: String?,
)

data class DriveValidationResult(
    val checked: Boolean,
    val missingItems: Int,
)

data class DriveImportResult(
    val folderName: String,
    val foundFiles: Int,
    val importedFiles: Int,
    val alreadyImportedFiles: Int,
)

data class DriveDesignImportCandidate(
    val name: String,
    val uri: String,
    val sizeBytes: Long?,
    val modifiedAt: Long?,
)

