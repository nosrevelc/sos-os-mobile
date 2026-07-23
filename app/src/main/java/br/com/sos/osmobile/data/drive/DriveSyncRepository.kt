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
        val mustRebuild = workOrder.driveFolderUri.isNullOrBlank() || !documentExists(workOrder.driveFolderUri)
        val result = if (mustRebuild) rebuildWorkOrderSyncLocked(workOrderId) else syncWorkOrderLocked(workOrderId)
        result.map {
            DriveSmartSyncResult(
                rebuilt = mustRebuild,
                warning = workOrderDriveWarningLocked(workOrderId),
            )
        }
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
            if (workOrder.driveFolderUri != null && !documentExists(workOrder.driveFolderUri)) {
                workOrderDao.resetDriveSync(workOrder.id, Clock.nowMillis())
                photoDao.resetDriveSyncByWorkOrder(workOrder.id)
                signatureDao.resetDriveSyncByWorkOrder(workOrder.id)
                clearStoredSubFolders(workOrder.id)
            }
            val folders = syncWorkOrderFolder(workOrder)
            val failures = photoDao.listPendingDriveSyncByWorkOrder(workOrder.id)
                .mapNotNull { syncPhoto(it, folders).exceptionOrNull()?.message }
                .toMutableList()
            signatureDao.findByWorkOrder(workOrder.id)
                ?.takeUnless { it.driveSyncStatus == DriveSyncStatus.SYNCED && !it.driveFileUri.isNullOrBlank() && documentExists(it.driveFileUri) }
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
        clearStoredSubFolders(workOrder.id)
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
            val workOrder = workOrderDao.findById(photo.workOrderId) ?: error("OS nao encontrada.")
            val driveFolders = folders ?: syncWorkOrderFolder(workOrder)
            val localFile = File(context.filesDir, photo.relativePath)
                .takeIf { it.exists() && it.length() > 0L }
                ?: error("Arquivo local do anexo nao encontrado.")
            val targetFolder = if (isDocumentAttachment(photo.fileName)) {
                driveFolders.documents ?: ensureNamedFolder(photo.workOrderId, driveFolders.workOrder, "Documentos")
                    .also { driveFolders.documents = it }
            } else {
                driveFolders.images ?: ensureNamedFolder(photo.workOrderId, driveFolders.workOrder, "Imagens")
                    .also { driveFolders.images = it }
            }
            val targetFile = photo.driveFileUri
                ?.takeIf { it.isNotBlank() && documentExists(it) }
                ?.let(Uri::parse)
                ?: findChildFile(targetFolder, photo.fileName)
                ?: createFile(targetFolder, photo.mimeType, photo.fileName)
                ?: error("Nao foi possivel criar arquivo no Drive.")
            localFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(targetFile, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: error("Nao foi possivel escrever no Drive.")
            }
            val confirmedFile = confirmFileInFolder(targetFolder, photo.fileName, localFile.length(), "anexo")
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
            val targetFolder = folders.signatures ?: ensureNamedFolder(signature.workOrderId, folders.workOrder, "Assinaturas")
                .also { folders.signatures = it }
            val targetFile = signature.driveFileUri
                ?.takeIf { it.isNotBlank() && documentExists(it) }
                ?.let(Uri::parse)
                ?: findChildFile(targetFolder, signature.fileName)
                ?: createFile(targetFolder, "image/png", signature.fileName)
                ?: error("Nao foi possivel criar assinatura no Drive.")
            localFile.inputStream().use { input ->
                context.contentResolver.openOutputStream(targetFile, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: error("Nao foi possivel escrever assinatura no Drive.")
            }
            val confirmedFile = confirmFileInFolder(targetFolder, signature.fileName, localFile.length(), "assinatura")
            signatureDao.updateDriveSync(signature.id, confirmedFile.toString(), DriveSyncStatus.SYNCED, null)
            auditRepository.record("Google Drive", "Assinatura sincronizada", "ordens_servico", signature.workOrderId, details = signature.fileName)
        }.onFailure {
            signatureDao.updateDriveSync(signature.id, null, DriveSyncStatus.ERROR, it.message ?: "Falha ao sincronizar assinatura.")
        }
    }

    private suspend fun revalidateSyncedItems() {
        workOrderDao.listAll()
            .filter { it.driveSyncStatus == DriveSyncStatus.SYNCED && it.driveFolderUri != null && !documentExists(it.driveFolderUri) }
            .forEach {
            workOrderDao.resetDriveSync(it.id, Clock.nowMillis())
            photoDao.resetDriveSyncByWorkOrder(it.id)
            signatureDao.resetDriveSyncByWorkOrder(it.id)
        }
        photoDao.listAll()
            .filter { it.driveSyncStatus == DriveSyncStatus.SYNCED && it.driveFileUri != null && !documentExists(it.driveFileUri) }
            .forEach { photoDao.updateDriveSync(it.id, null, DriveSyncStatus.PENDING, "Arquivo nao encontrado no Drive.") }
        signatureDao.listAll()
            .filter { it.driveSyncStatus == DriveSyncStatus.SYNCED && it.driveFileUri != null && !documentExists(it.driveFileUri) }
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
            clearStoredSubFolders(workOrder.id)
            val signatureMissing = if (signatureDao.findByWorkOrder(workOrder.id) != null) 1 else 0
            return DriveValidationResult(
                checked = true,
                missingItems = 1 + photoDao.listByWorkOrderAsc(workOrder.id).size + signatureMissing,
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
        val customerName = sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerPhone = sanitizeName(summary?.customerPhone.orEmpty().filter { it.isDigit() })
        val customerFolderName = if (customerPhone.isBlank()) customerName else "${customerName}_$customerPhone"
        val osFolderName = "OS-${sanitizeName(workOrder.numero)}"
        val expectedPath = "$customerFolderName/$osFolderName"
        val storedRoot = settingsRepository.getString(driveWorkOrderRootKey(workOrder.id)).orEmpty()
        val storedPath = settingsRepository.getString(driveWorkOrderPathKey(workOrder.id)).orEmpty()
        workOrder.driveFolderUri
            ?.takeIf { it.isNotBlank() && documentExists(it) }
            ?.takeIf { storedRoot == root.treeUri.toString() }
            ?.takeIf { storedPath == expectedPath }
            ?.takeIf { documentDisplayName(it) == osFolderName }
            ?.let { return Uri.parse(it) }
        val customerFolder = findChildDirectory(root.folderUri, customerFolderName) ?: return null
        return findChildDirectory(customerFolder, osFolderName)
    }

    private suspend fun drivePhotoExistsInExpectedFolder(workOrderFolder: Uri, photo: WorkOrderPhotoEntity): Boolean {
        val folderName = if (isDocumentAttachment(photo.fileName)) "Documentos" else "Imagens"
        val targetFolder = findExistingNamedFolder(photo.workOrderId, workOrderFolder, folderName) ?: return false
        return findChildFile(targetFolder, photo.fileName) != null
    }

    private suspend fun driveSignatureExistsInExpectedFolder(workOrderFolder: Uri, signature: WorkOrderSignatureEntity): Boolean {
        val targetFolder = findExistingNamedFolder(signature.workOrderId, workOrderFolder, "Assinaturas") ?: return false
        return findChildFile(targetFolder, signature.fileName) != null
    }

    private suspend fun ensureWorkOrderFolders(workOrder: WorkOrderEntity): Uri {
        val root = rootFolder() ?: error("Selecione uma pasta dentro do Drive. A raiz Meu Drive nao deve ser usada.")
        val summary = workOrderDao.findSummaryById(workOrder.id)
        val customerName = sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerPhone = sanitizeName(summary?.customerPhone.orEmpty().filter { it.isDigit() })
        val customerFolderName = if (customerPhone.isBlank()) customerName else "${customerName}_$customerPhone"
        val osFolderName = "OS-${sanitizeName(workOrder.numero)}"
        val expectedPath = "$customerFolderName/$osFolderName"
        val storedRoot = settingsRepository.getString(driveWorkOrderRootKey(workOrder.id)).orEmpty()
        val storedPath = settingsRepository.getString(driveWorkOrderPathKey(workOrder.id)).orEmpty()
        workOrder.driveFolderUri
            ?.takeIf { it.isNotBlank() && documentExists(it) }
            ?.takeIf { storedRoot == root.treeUri.toString() }
            ?.takeIf { storedPath == expectedPath }
            ?.takeIf { documentDisplayName(it) == osFolderName }
            ?.let { storedFolderUri ->
                setSettingIfChanged(driveWorkOrderRootKey(workOrder.id), root.treeUri.toString())
                setSettingIfChanged(driveWorkOrderPathKey(workOrder.id), expectedPath)
                return Uri.parse(storedFolderUri)
            }

        val customerFolder = findOrCreateDirectory(root.folderUri, customerFolderName)
        val osFolder = findOrCreateDirectory(customerFolder, osFolderName)
        if (workOrder.driveFolderUri != osFolder.toString()) {
            clearStoredSubFolders(workOrder.id)
        }
        setSettingIfChanged(driveWorkOrderRootKey(workOrder.id), root.treeUri.toString())
        setSettingIfChanged(driveWorkOrderPathKey(workOrder.id), expectedPath)
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
            ?.takeUnless { it.isDriveRootLike() }
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
            clearStoredSubFolders(workOrder.id)
            setSettingIfChanged(driveWorkOrderRootKey(workOrder.id), "")
            setSettingIfChanged(driveWorkOrderPathKey(workOrder.id), "")
        }
    }

    suspend fun buildWorkOrderDebugReport(workOrderId: Long): String = syncMutex.withLock {
        val workOrder = workOrderDao.findById(workOrderId) ?: return "Drive debug: OS $workOrderId nao encontrada."
        val summary = workOrderDao.findSummaryById(workOrder.id)
        val root = rootFolder()
        val customerName = sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerPhone = sanitizeName(summary?.customerPhone.orEmpty().filter { it.isDigit() })
        val customerFolderName = if (customerPhone.isBlank()) customerName else "${customerName}_$customerPhone"
        val osFolderName = "OS-${sanitizeName(workOrder.numero)}"
        val expectedPath = "$customerFolderName/$osFolderName"
        val storedRoot = settingsRepository.getString(driveWorkOrderRootKey(workOrder.id)).orEmpty()
        val storedPath = settingsRepository.getString(driveWorkOrderPathKey(workOrder.id)).orEmpty()
        val storedImages = settingsRepository.getString(driveSubFolderKey(workOrder.id, "Imagens")).orEmpty()
        val storedDocuments = settingsRepository.getString(driveSubFolderKey(workOrder.id, "Documentos")).orEmpty()
        val storedSignatures = settingsRepository.getString(driveSubFolderKey(workOrder.id, "Assinaturas")).orEmpty()
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
            appendLine("URI OS existe: ${workOrder.driveFolderUri?.let(::documentExists) ?: false}")
            appendLine("Root configurado: ${root?.treeUri ?: "nao configurado/sem escrita"}")
            appendLine("Root salvo na OS: $storedRoot")
            appendLine("Path salvo na OS: $storedPath")
            appendLine("Subpasta Imagens salva: $storedImages | existe=${storedImages.takeIf { it.isNotBlank() }?.let(::documentExists) ?: false}")
            appendLine("Subpasta Documentos salva: $storedDocuments | existe=${storedDocuments.takeIf { it.isNotBlank() }?.let(::documentExists) ?: false}")
            appendLine("Subpasta Assinaturas salva: $storedSignatures | existe=${storedSignatures.takeIf { it.isNotBlank() }?.let(::documentExists) ?: false}")
            if (root == null) {
                appendLine("Pastas no Drive: root indisponivel.")
            } else {
                val customerFolders = findChildren(root.folderUri, customerFolderName, DocumentsContract.Document.MIME_TYPE_DIR)
                appendLine("Pastas cliente com mesmo nome: ${customerFolders.size}")
                customerFolders.forEachIndexed { index, customer ->
                    appendLine("Cliente[$index]: ${customer.uri}")
                    val osFolders = findChildren(customer.uri, osFolderName, DocumentsContract.Document.MIME_TYPE_DIR)
                    appendLine("  Pastas OS com mesmo nome: ${osFolders.size}")
                    osFolders.forEachIndexed { osIndex, os ->
                        appendLine("  OS[$osIndex]: ${os.uri}")
                        val children = findChildren(os.uri, expectedMimeType = DocumentsContract.Document.MIME_TYPE_DIR)
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
        val customerName = sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerPhone = sanitizeName(summary?.customerPhone.orEmpty().filter { it.isDigit() })
        val customerFolderName = if (customerPhone.isBlank()) customerName else "${customerName}_$customerPhone"
        val osFolderName = "OS-${sanitizeName(workOrder.numero)}"
        val customerFolders = findChildren(root.folderUri, customerFolderName, DocumentsContract.Document.MIME_TYPE_DIR)
        val duplicateOsCount = customerFolders.sumOf { customer ->
            findChildren(customer.uri, osFolderName, DocumentsContract.Document.MIME_TYPE_DIR).size
        }
        val duplicateSubfolders = workOrder.driveFolderUri
            ?.takeIf { it.isNotBlank() && documentExists(it) }
            ?.let { Uri.parse(it) }
            ?.let { osFolder ->
                listOf("Documentos", "Imagens", "Assinaturas")
                    .map { folderName -> folderName to findChildren(osFolder, folderName, DocumentsContract.Document.MIME_TYPE_DIR).size }
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

    private fun DocumentFile.isDriveRootLike(): Boolean {
        val normalized = name.orEmpty().trim().lowercase()
        return normalized in setOf("meu drive", "my drive", "drive", "arquivos do drive")
    }

    private suspend fun ensureNamedFolder(workOrderId: Long, workOrderFolder: Uri, folderName: String): Uri {
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

    private suspend fun findExistingNamedFolder(workOrderId: Long, workOrderFolder: Uri, folderName: String): Uri? {
        val existingFolders = findChildren(workOrderFolder, folderName, DocumentsContract.Document.MIME_TYPE_DIR)
        return settingsRepository.getString(driveSubFolderKey(workOrderId, folderName))
            ?.takeIf { it.isNotBlank() && documentExists(it) }
            ?.takeIf { documentDisplayName(it) == folderName }
            ?.takeIf { stored -> existingFolders.any { it.uri.toString() == stored } }
            ?.let(Uri::parse)
            ?: existingFolders.firstOrNull()?.uri
    }

    private suspend fun clearStoredSubFolders(workOrderId: Long) {
        setSettingIfChanged(driveSubFolderKey(workOrderId, "Imagens"), "")
        setSettingIfChanged(driveSubFolderKey(workOrderId, "Documentos"), "")
        setSettingIfChanged(driveSubFolderKey(workOrderId, "Assinaturas"), "")
    }

    private fun driveSubFolderKey(workOrderId: Long, folderName: String): String =
        "drive_subfolder_${workOrderId}_${folderName.lowercase()}"

    private fun driveWorkOrderRootKey(workOrderId: Long): String =
        "drive_work_order_root_$workOrderId"

    private fun driveWorkOrderPathKey(workOrderId: Long): String =
        "drive_work_order_path_$workOrderId"

    private suspend fun setSettingIfChanged(key: String, value: String) {
        if (settingsRepository.getString(key) != value) {
            settingsRepository.set(key, value)
        }
    }

    private fun findOrCreateDirectory(parent: Uri, name: String): Uri =
        findChildDirectory(parent, name)
            ?: DocumentsContract.createDocument(context.contentResolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: error("Nao foi possivel criar pasta $name.")

    private fun findChildDirectory(parent: Uri, name: String): Uri? =
        findChild(parent, name, DocumentsContract.Document.MIME_TYPE_DIR)

    private fun findChildFile(parent: Uri, name: String): Uri? =
        findChild(parent, name, expectedMimeType = null)

    private fun createFile(parent: Uri, mimeType: String, name: String): Uri? =
        DocumentsContract.createDocument(context.contentResolver, parent, mimeType, name)

    private fun confirmFileInFolder(parent: Uri, fileName: String, expectedMinSize: Long, label: String): Uri {
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

    private fun findChild(parent: Uri, name: String, expectedMimeType: String?): Uri? {
        return findChildren(parent, name, expectedMimeType).firstOrNull()?.uri
    }

    private fun findChildren(parent: Uri, name: String? = null, expectedMimeType: String? = null): List<DriveChild> {
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

    private fun documentExists(uri: String): Boolean {
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

    private fun documentDisplayName(uri: String): String? {
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

    private fun documentSize(uri: Uri): Long? {
        val projection = arrayOf(DocumentsContract.Document.COLUMN_SIZE)
        return context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
            if (sizeIndex < 0 || cursor.isNull(sizeIndex)) null else cursor.getLong(sizeIndex)
        }
    }

    private fun sanitizeName(value: String): String =
        value.replace(Regex("[\\\\/:*?\"<>|]"), "-").trim().take(80).ifBlank { "Sem nome" }

    private fun isDocumentAttachment(fileName: String): Boolean =
        fileName.startsWith("documento_") || fileName.startsWith("comprovante_")
}

private data class WorkOrderDriveFolders(
    val workOrder: Uri,
    var images: Uri? = null,
    var documents: Uri? = null,
    var signatures: Uri? = null,
)

private data class DriveRoot(
    val treeUri: Uri,
    val folderUri: Uri,
)

private data class DriveChild(
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
