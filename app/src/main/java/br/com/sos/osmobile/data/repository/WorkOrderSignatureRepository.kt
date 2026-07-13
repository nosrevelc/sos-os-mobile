package br.com.sos.osmobile.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.WorkOrderSignatureDao
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity
import java.io.File

class WorkOrderSignatureRepository(
    private val context: Context,
    private val signatureDao: WorkOrderSignatureDao,
    private val auditRepository: AuditRepository,
) {
    suspend fun findByWorkOrder(workOrderId: Long): WorkOrderSignatureEntity? =
        signatureDao.findByWorkOrder(workOrderId)

    suspend fun saveSignature(workOrderId: Long, signerName: String, bitmap: Bitmap): Long {
        val now = Clock.nowMillis()
        val dir = File(context.filesDir, "work_order_signatures/$workOrderId").apply { mkdirs() }
        val fileName = "assinatura_$now.png"
        val target = File(dir, fileName)
        target.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        signatureDao.deleteByWorkOrder(workOrderId)
        val id = signatureDao.upsert(
            WorkOrderSignatureEntity(
                workOrderId = workOrderId,
                fileName = fileName,
                relativePath = "work_order_signatures/$workOrderId/$fileName",
                signerName = signerName.trim().ifBlank { "Cliente" },
                createdAt = now,
            ),
        )
        auditRepository.record("Assinaturas", "Assinatura salva na OS", "ordens_servico", workOrderId, details = signerName)
        return id
    }

    suspend fun deleteSignature(workOrderId: Long) {
        val signature = signatureDao.findByWorkOrder(workOrderId) ?: return
        File(context.filesDir, signature.relativePath).delete()
        signatureDao.deleteByWorkOrder(workOrderId)
        auditRepository.record("Assinaturas", "Assinatura removida da OS", "ordens_servico", workOrderId)
    }

    fun uriFor(signature: WorkOrderSignatureEntity): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(context.filesDir, signature.relativePath),
        )
}
