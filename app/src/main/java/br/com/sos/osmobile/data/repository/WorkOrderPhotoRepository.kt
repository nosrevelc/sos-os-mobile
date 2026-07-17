package br.com.sos.osmobile.data.repository

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.WorkOrderPhotoDao
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import java.io.File

class WorkOrderPhotoRepository(
    private val context: Context,
    private val photoDao: WorkOrderPhotoDao,
    private val auditRepository: AuditRepository,
) {
    suspend fun listByWorkOrder(workOrderId: Long): List<WorkOrderPhotoEntity> =
        photoDao.listByWorkOrder(workOrderId)

    suspend fun addPhoto(workOrderId: Long, source: Uri, isPaymentProof: Boolean = false): Long {
        val now = Clock.nowMillis()
        val mimeType = context.contentResolver.getType(source) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "application/pdf" -> "pdf"
            else -> "jpg"
        }
        val dir = File(context.filesDir, "work_order_photos/$workOrderId").apply { mkdirs() }
        val fileName = if (isPaymentProof) "comprovante_${now}.$extension" else "foto_${now}.$extension"
        val target = File(dir, fileName)
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Nao foi possivel abrir a foto.")
        val id = photoDao.insert(
            WorkOrderPhotoEntity(
                workOrderId = workOrderId,
                fileName = fileName,
                relativePath = "work_order_photos/$workOrderId/$fileName",
                mimeType = mimeType,
                createdAt = now,
            ),
        )
        auditRepository.record("Fotos", "Foto adicionada na OS", "ordens_servico", workOrderId, details = fileName)
        return id
    }

    suspend fun deletePhoto(id: Long) {
        val photo = photoDao.findById(id) ?: return
        File(context.filesDir, photo.relativePath).delete()
        photoDao.deleteById(id)
        auditRepository.record("Fotos", "Foto removida da OS", "ordens_servico", photo.workOrderId, details = photo.fileName)
    }

    fun uriFor(photo: WorkOrderPhotoEntity): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(context.filesDir, photo.relativePath),
        )
}
