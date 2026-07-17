package br.com.sos.osmobile.data.drive

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Network
import android.net.NetworkRequest
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.WorkOrderDao
import br.com.sos.osmobile.data.local.dao.WorkOrderPhotoDao
import br.com.sos.osmobile.data.local.entity.DriveSyncStatus
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.DRIVE_ROOT_URI_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.DRIVE_SYNC_ENABLED_KEY
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class DriveSyncRepository(
    private val context: Context,
    private val workOrderDao: WorkOrderDao,
    private val photoDao: WorkOrderPhotoDao,
    private val settingsRepository: SettingsRepository,
    private val auditRepository: AuditRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var networkCallbackRegistered = false

    fun startAutoSync() {
        if (networkCallbackRegistered) return
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(
            request,
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch { syncAllPending() }
                }
            },
        )
        networkCallbackRegistered = true
        scope.launch { syncAllPending() }
    }

    suspend fun syncAllPending(): DriveSyncResult {
        if (!isConfigured()) return DriveSyncResult.Skipped("Drive nao configurado.")
        if (!isOnline()) return DriveSyncResult.Skipped("Sem internet. Sync pendente.")
        var synced = 0
        workOrderDao.listPendingDriveSync().forEach { workOrder ->
            if (syncWorkOrder(workOrder.id).isSuccess) synced++
        }
        photoDao.listPendingDriveSync().forEach { photo ->
            if (syncPhoto(photo).isSuccess) synced++
        }
        return DriveSyncResult.Done(synced)
    }

    suspend fun syncWorkOrder(workOrderId: Long): Result<Unit> {
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
            val folder = ensureWorkOrderFolder(workOrder)
            markWorkOrder(workOrder, DriveSyncStatus.SYNCED, null, folder.uri.toString())
            photoDao.listPendingDriveSyncByWorkOrder(workOrder.id).forEach { syncPhoto(it) }
            auditRepository.record("Google Drive", "Pasta da OS sincronizada", "ordens_servico", workOrder.id, details = workOrder.numero)
        }.onFailure {
            markWorkOrder(workOrder, DriveSyncStatus.ERROR, it.message ?: "Falha ao sincronizar.")
        }
    }

    suspend fun syncPhoto(photoId: Long): Result<Unit> {
        val photo = photoDao.findById(photoId) ?: return Result.failure(IllegalArgumentException("Anexo nao encontrado."))
        return syncPhoto(photo)
    }

    private suspend fun syncPhoto(photo: WorkOrderPhotoEntity): Result<Unit> {
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
            val workOrderFolder = ensureWorkOrderFolder(workOrder)
            val targetFolderName = if (photo.fileName.startsWith("comprovante_")) "Comprovantes" else "Fotos"
            val targetFolder = workOrderFolder.findOrCreateFolder(targetFolderName)
            val existing = targetFolder.findFile(photo.fileName)
            val targetFile = existing ?: targetFolder.createFile(photo.mimeType, photo.fileName)
                ?: error("Nao foi possivel criar arquivo no Drive.")
            File(context.filesDir, photo.relativePath).inputStream().use { input ->
                context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { output ->
                    input.copyTo(output)
                } ?: error("Nao foi possivel escrever no Drive.")
            }
            photoDao.updateDriveSync(photo.id, targetFile.uri.toString(), DriveSyncStatus.SYNCED, null)
            auditRepository.record("Google Drive", "Anexo sincronizado", "ordens_servico", photo.workOrderId, details = photo.fileName)
        }.onFailure {
            photoDao.updateDriveSync(photo.id, photo.driveFileUri, DriveSyncStatus.ERROR, it.message ?: "Falha ao sincronizar anexo.")
        }
    }

    private suspend fun ensureWorkOrderFolder(workOrder: WorkOrderEntity): DocumentFile {
        val root = rootFolder() ?: error("Pasta do Drive nao configurada.")
        val summary = workOrderDao.findSummaryById(workOrder.id)
        val customerName = sanitizeName(summary?.customerName ?: "Cliente ${workOrder.customerId}")
        val customerFolder = root.findOrCreateFolder(customerName)
        val osFolder = customerFolder.findOrCreateFolder("OS-${sanitizeName(workOrder.numero)}")
        osFolder.findOrCreateFolder("Fotos")
        osFolder.findOrCreateFolder("Comprovantes")
        osFolder.findOrCreateFolder("Assinaturas")
        osFolder.findOrCreateFolder("Documentos")
        return osFolder
    }

    private suspend fun rootFolder(): DocumentFile? {
        val uri = settingsRepository.getString(DRIVE_ROOT_URI_KEY)?.takeIf { it.isNotBlank() } ?: return null
        return DocumentFile.fromTreeUri(context, Uri.parse(uri))?.takeIf { it.canWrite() }
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

    private fun DocumentFile.findOrCreateFolder(name: String): DocumentFile =
        findFile(name)?.takeIf { it.isDirectory } ?: createDirectory(name) ?: error("Nao foi possivel criar pasta $name.")

    private fun sanitizeName(value: String): String =
        value.replace(Regex("[\\\\/:*?\"<>|]"), "-").trim().take(80).ifBlank { "Sem nome" }
}

sealed class DriveSyncResult {
    data class Done(val syncedItems: Int) : DriveSyncResult()
    data class Skipped(val reason: String) : DriveSyncResult()
}
