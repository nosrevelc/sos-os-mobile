package br.com.sos.osmobile.feature.workorders

import android.graphics.Bitmap
import android.net.Uri
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity
import br.com.sos.osmobile.data.repository.WorkOrderChecklistRepository
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderPhotoRepository
import br.com.sos.osmobile.data.repository.WorkOrderSignatureRepository
import br.com.sos.osmobile.data.repository.WorkOrderWarrantyRepository
import kotlinx.coroutines.launch

class WorkOrderAttachmentController(
    private val session: WorkOrderSessionState,
    private val photoRepository: WorkOrderPhotoRepository,
    private val signatureRepository: WorkOrderSignatureRepository,
    private val checklistRepository: WorkOrderChecklistRepository,
    private val warrantyRepository: WorkOrderWarrantyRepository,
    private val paymentRepository: WorkOrderPaymentRepository,
    private val driveSyncRepository: DriveSyncRepository,
    private val workOrderRepository: br.com.sos.osmobile.data.repository.WorkOrderRepository,
) {

    fun addPhoto(uri: Uri, isDocument: Boolean = false, documentDescription: String = "") {
        val workOrderId = session.formState.editingId ?: run {
            session.formState = session.formState.copy(message = "Salve a OS antes de adicionar anexos.")
            return
        }
        session.scope?.launch {
            var syncError: String? = null
            var driveWarning: String? = null
            runCatching {
                photoRepository.addPhoto(workOrderId, uri, isDocument, documentDescription)
                syncError = driveSyncRepository.syncWorkOrder(workOrderId).exceptionOrNull()?.message
                loadPhotos(workOrderId)
                refreshDriveStatus(workOrderId)
                driveWarning = driveSyncRepository.workOrderDriveWarning(workOrderId)
            }.onSuccess {
                val savedMessage = if (isDocument) "Documento anexado." else "Imagem adicionada."
                session.formState = session.formState.copy(
                    message = listOfNotNull(syncError?.let { "$savedMessage Sync pendente: $it" } ?: savedMessage, driveWarning).joinToString(" "),
                )
            }.onFailure {
                session.formState = session.formState.copy(message = "Nao foi possivel adicionar o anexo: ${it.message.orEmpty()}")
            }
        }
    }

    fun deletePhoto(photoId: Long) {
        val workOrderId = session.formState.editingId ?: return
        session.scope?.launch {
            photoRepository.deletePhoto(photoId)
            loadPhotos(workOrderId)
            session.formState = session.formState.copy(message = "Foto removida.")
        }
    }

    fun photoUri(photo: WorkOrderPhotoEntity): Uri = photoRepository.uriFor(photo)

    fun saveSignature(signerName: String, bitmap: Bitmap) {
        val workOrderId = session.formState.editingId ?: run {
            session.formState = session.formState.copy(message = "Salve a OS antes de assinar.")
            return
        }
        session.scope?.launch {
            var syncError: String? = null
            signatureRepository.saveSignature(workOrderId, signerName, bitmap)
            syncError = driveSyncRepository.syncSignature(workOrderId).exceptionOrNull()?.message
            loadSignature(workOrderId)
            refreshDriveStatus(workOrderId)
            session.formState = session.formState.copy(
                message = syncError?.let { "Assinatura salva. Sync pendente: $it" } ?: "Assinatura salva e sincronizada.",
            )
        }
    }

    fun deleteSignature() {
        val workOrderId = session.formState.editingId ?: return
        session.scope?.launch {
            signatureRepository.deleteSignature(workOrderId)
            loadSignature(workOrderId)
            session.formState = session.formState.copy(message = "Assinatura removida.")
        }
    }

    fun signatureUri(signature: WorkOrderSignatureEntity): Uri = signatureRepository.uriFor(signature)

    fun addChecklistItem(description: String) {
        val workOrderId = session.formState.editingId ?: run {
            session.formState = session.formState.copy(message = "Salve a OS antes de adicionar checklist.")
            return
        }
        if (description.isBlank()) {
            session.formState = session.formState.copy(message = "Informe o item do checklist.")
            return
        }
        session.scope?.launch {
            checklistRepository.addItem(workOrderId, description)
            loadChecklist(workOrderId)
            session.formState = session.formState.copy(message = "Item adicionado ao checklist.")
        }
    }

    fun setChecklistChecked(itemId: Long, checked: Boolean) {
        val workOrderId = session.formState.editingId ?: return
        session.scope?.launch {
            checklistRepository.setChecked(itemId, checked)
            loadChecklist(workOrderId)
        }
    }

    fun deleteChecklistItem(itemId: Long) {
        val workOrderId = session.formState.editingId ?: return
        session.scope?.launch {
            checklistRepository.deleteItem(itemId)
            loadChecklist(workOrderId)
        }
    }

    fun saveWarranty(days: String, terms: String) {
        val workOrderId = session.formState.editingId ?: run {
            session.formState = session.formState.copy(message = "Salve a OS antes de criar garantia.")
            return
        }
        session.scope?.launch {
            warrantyRepository.save(workOrderId, days.toIntOrNull() ?: 0, terms)
            loadWarranty(workOrderId)
            session.formState = session.formState.copy(message = "Garantia salva.")
        }
    }

    fun deleteWarranty() {
        val workOrderId = session.formState.editingId ?: return
        session.scope?.launch {
            warrantyRepository.delete(workOrderId)
            loadWarranty(workOrderId)
            session.formState = session.formState.copy(message = "Garantia removida.")
        }
    }

    fun addPayment(value: String, method: String, note: String) {
        val workOrderId = session.formState.editingId ?: run {
            session.formState = session.formState.copy(message = "Salve a OS antes de registrar pagamento.")
            return
        }
        val parsedValue = WorkOrderFormValidator.parseDecimal(value)
        if (parsedValue == null || parsedValue <= 0.0) {
            session.formState = session.formState.copy(message = "Informe um valor de pagamento valido.")
            return
        }
        session.scope?.launch {
            paymentRepository.addPayment(workOrderId, parsedValue, method, note)
            loadPayments(workOrderId)
            session.formState = session.formState.copy(message = "Pagamento registrado.")
        }
    }

    fun deletePayment(paymentId: Long) {
        val workOrderId = session.formState.editingId ?: return
        session.scope?.launch {
            paymentRepository.deletePayment(paymentId)
            loadPayments(workOrderId)
        }
    }

    suspend fun loadPhotos(workOrderId: Long) {
        session.photos = photoRepository.listByWorkOrder(workOrderId)
    }

    suspend fun loadSignature(workOrderId: Long) {
        session.signature = signatureRepository.findByWorkOrder(workOrderId)
    }

    suspend fun loadChecklist(workOrderId: Long) {
        session.checklist = checklistRepository.listByWorkOrder(workOrderId)
    }

    suspend fun loadWarranty(workOrderId: Long) {
        session.warranty = warrantyRepository.findByWorkOrder(workOrderId)
    }

    suspend fun loadPayments(workOrderId: Long) {
        session.payments = paymentRepository.listByWorkOrder(workOrderId)
    }

    suspend fun refreshDriveStatus(workOrderId: Long) {
        val workOrder = workOrderRepository.findById(workOrderId) ?: return
        session.formState = session.formState.copy(
            driveSyncStatus = workOrder.driveSyncStatus,
            driveSyncError = workOrder.driveSyncError.orEmpty(),
        )
    }
}
