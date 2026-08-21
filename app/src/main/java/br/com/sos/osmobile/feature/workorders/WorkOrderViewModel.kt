package br.com.sos.osmobile.feature.workorders

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.drive.DriveDesignImportCandidate
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.local.entity.WorkOrderChecklistItemEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderWarrantyEntity
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalPrintStyle
import br.com.sos.osmobile.data.print.ThermalTextBlock
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.COMPANY_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PIX_KEY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PIX_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_BLUETOOTH_ADDRESS_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_AUTO_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_COPIES_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_FONT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_FOOTER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_ALIGN_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_BOLD_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_TEXT_SIZE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.QUOTE_MIN_ACCEPTANCE_VALUE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_DELIVERED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_NOT_DELIVERED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_ORDER_SENT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_OUT_FOR_DELIVERY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_CONFIRMED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_PENDING_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_PROOF_REQUEST_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PICKUP_REMINDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_REVIEW_REQUEST_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_THANK_YOU_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_CANCELED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_COMPLETED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_OPEN_KEY
import br.com.sos.osmobile.data.repository.StockRepository
import br.com.sos.osmobile.data.repository.WorkOrderChecklistRepository
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderPhotoRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.data.repository.WorkOrderSignatureRepository
import br.com.sos.osmobile.data.repository.WorkOrderWarrantyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkOrderViewModel(
    private val workOrderRepository: WorkOrderRepository,
    private val auditRepository: AuditRepository,
    customerRepository: CustomerRepository,
    serviceProductRepository: ServiceProductRepository,
    settingsRepository: SettingsRepository,
    photoRepository: WorkOrderPhotoRepository,
    signatureRepository: WorkOrderSignatureRepository,
    checklistRepository: WorkOrderChecklistRepository,
    warrantyRepository: WorkOrderWarrantyRepository,
    private val paymentRepository: WorkOrderPaymentRepository,
    stockRepository: StockRepository,
    private val driveSyncRepository: DriveSyncRepository,
) : ViewModel() {

    val uiState: StateFlow<WorkOrderUiState> = combine(
        customerRepository.observeActive(),
        serviceProductRepository.observeActive(),
        workOrderRepository.observeSummaries(),
        settingsRepository.observeAll(),
        stockRepository.observeSummaries(),
    ) { customers, services, workOrders, settings, stockSummaries ->
        val values = settings.associate { it.chave to it.valor }
        WorkOrderUiState(
            customers = customers,
            services = services,
            stockByServiceProductId = stockSummaries.associate { it.id to it.saldo },
            workOrders = workOrders,
            companyName = values[COMPANY_NAME_KEY].orEmpty(),
            pixName = values[PIX_NAME_KEY].orEmpty(),
            pixKey = values[PIX_KEY_KEY].orEmpty(),
            quoteMinAcceptanceValue = values[QUOTE_MIN_ACCEPTANCE_VALUE_KEY].orEmpty(),
            workOrderTemplate = values[TEMPLATE_WORK_ORDER_KEY] ?: MessageTemplateRenderer.workOrderDefaultTemplate,
            workOrderStatusTemplates = mapOf(
                WorkOrderStatus.Open.label to (values[TEMPLATE_WORK_ORDER_OPEN_KEY] ?: MessageTemplateRenderer.workOrderOpenTemplate),
                WorkOrderStatus.InProgress.label to (values[TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY] ?: MessageTemplateRenderer.workOrderInProgressTemplate),
                WorkOrderStatus.Completed.label to (values[TEMPLATE_WORK_ORDER_COMPLETED_KEY] ?: MessageTemplateRenderer.workOrderCompletedTemplate),
                WorkOrderStatus.Canceled.label to (values[TEMPLATE_WORK_ORDER_CANCELED_KEY] ?: MessageTemplateRenderer.workOrderCanceledTemplate),
            ),
            reviewRequestTemplate = values[TEMPLATE_REVIEW_REQUEST_KEY] ?: MessageTemplateRenderer.reviewRequestTemplate,
            pickupReminderTemplate = values[TEMPLATE_PICKUP_REMINDER_KEY] ?: MessageTemplateRenderer.pickupReminderTemplate,
            paymentPendingTemplate = values[TEMPLATE_PAYMENT_PENDING_KEY] ?: MessageTemplateRenderer.paymentPendingTemplate,
            paymentConfirmedTemplate = values[TEMPLATE_PAYMENT_CONFIRMED_KEY] ?: MessageTemplateRenderer.paymentConfirmedTemplate,
            paymentProofRequestTemplate = values[TEMPLATE_PAYMENT_PROOF_REQUEST_KEY] ?: MessageTemplateRenderer.paymentProofRequestTemplate,
            orderSentTemplate = values[TEMPLATE_ORDER_SENT_KEY] ?: MessageTemplateRenderer.orderSentTemplate,
            outForDeliveryTemplate = values[TEMPLATE_OUT_FOR_DELIVERY_KEY] ?: MessageTemplateRenderer.outForDeliveryTemplate,
            deliveredTemplate = values[TEMPLATE_DELIVERED_KEY] ?: MessageTemplateRenderer.deliveredTemplate,
            notDeliveredTemplate = values[TEMPLATE_NOT_DELIVERED_KEY] ?: MessageTemplateRenderer.notDeliveredTemplate,
            thankYouTemplate = values[TEMPLATE_THANK_YOU_KEY] ?: MessageTemplateRenderer.thankYouTemplate,
            photosEnabled = values["modulo_fotos"]?.toBooleanStrictOrNull() ?: false,
            signatureEnabled = values["modulo_assinatura"]?.toBooleanStrictOrNull() ?: false,
            checklistEnabled = values["modulo_checklist"]?.toBooleanStrictOrNull() ?: false,
            warrantyEnabled = values["modulo_garantia"]?.toBooleanStrictOrNull() ?: false,
            financeEnabled = values["modulo_financeiro"]?.toBooleanStrictOrNull() ?: false,
            printBluetoothAddress = values[PRINT_BLUETOOTH_ADDRESS_KEY].orEmpty(),
            printWorkOrderAuto = values[PRINT_WORK_ORDER_AUTO_KEY]?.toBooleanStrictOrNull() ?: false,
            printWorkOrderCopies = (values[PRINT_WORK_ORDER_COPIES_KEY]?.toIntOrNull() ?: 0).coerceIn(0, 9),
            printWorkOrderHeader = values[PRINT_WORK_ORDER_HEADER_KEY] ?: "{empresa}\nOS {os}\n{data}",
            printWorkOrderFooter = values[PRINT_WORK_ORDER_FOOTER_KEY] ?: "Obrigado pela preferencia",
            printWorkOrderStyle = ThermalPrintStyle(
                font = values[PRINT_WORK_ORDER_FONT_KEY] ?: "A",
                textSize = values[PRINT_WORK_ORDER_TEXT_SIZE_KEY] ?: "normal",
                headerBold = values[PRINT_WORK_ORDER_HEADER_BOLD_KEY]?.toBooleanStrictOrNull() ?: true,
                headerAlignment = values[PRINT_WORK_ORDER_HEADER_ALIGN_KEY] ?: "center",
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkOrderUiState())

    private val session = WorkOrderSessionState().apply {
        scope = viewModelScope
    }

    val form: WorkOrderFormController by lazy {
        WorkOrderFormController(
            session = session,
            workOrderRepository = workOrderRepository,
            stockRepository = stockRepository,
            paymentRepository = paymentRepository,
            driveSyncRepository = driveSyncRepository,
            uiStateProvider = { uiState.value },
            postEditLoad = { workOrderId -> postEditLoad(workOrderId) },
        )
    }

    private val attachments = WorkOrderAttachmentController(
        session = session,
        photoRepository = photoRepository,
        signatureRepository = signatureRepository,
        checklistRepository = checklistRepository,
        warrantyRepository = warrantyRepository,
        paymentRepository = paymentRepository,
        driveSyncRepository = driveSyncRepository,
        workOrderRepository = workOrderRepository,
    )

    private val messages = WorkOrderMessageController(
        session = session,
        workOrderRepository = workOrderRepository,
        paymentRepository = paymentRepository,
        uiStateProvider = { uiState.value },
    )

    private val drive = WorkOrderDriveController(
        session = session,
        driveSyncRepository = driveSyncRepository,
        settingsRepository = settingsRepository,
        reloadPhotos = { workOrderId -> attachments.loadPhotos(workOrderId) },
        reloadSignature = { workOrderId -> attachments.loadSignature(workOrderId) },
        refreshStatus = { workOrderId -> form.refreshDriveStatus(workOrderId) },
    )

    private val prints = WorkOrderPrintController(
        session = session,
        workOrderRepository = workOrderRepository,
        uiStateProvider = { uiState.value },
    )

    var formState: WorkOrderFormState
        get() = session.formState
        private set(value) {
            session.formState = value
        }

    var listMessage: String?
        get() = session.listMessage
        private set(value) {
            session.listMessage = value
        }

    var documentText: String?
        get() = session.documentText
        private set(value) {
            session.documentText = value
        }

    var messageText: String?
        get() = session.messageText
        private set(value) {
            session.messageText = value
        }

    var messagePhone: String
        get() = session.messagePhone
        private set(value) {
            session.messagePhone = value
        }

    var driveDebugReport: String
        get() = session.driveDebugReport
        private set(value) {
            session.driveDebugReport = value
        }

    var pendingDesignImportCandidates: List<DriveDesignImportCandidate>
        get() = session.pendingDesignImportCandidates
        private set(value) {
            session.pendingDesignImportCandidates = value
        }

    var historyText: String?
        get() = session.historyText
        private set(value) {
            session.historyText = value
        }

    var photos: List<WorkOrderPhotoEntity>
        get() = session.photos
        private set(value) {
            session.photos = value
        }

    var signature: WorkOrderSignatureEntity?
        get() = session.signature
        private set(value) {
            session.signature = value
        }

    var checklist: List<WorkOrderChecklistItemEntity>
        get() = session.checklist
        private set(value) {
            session.checklist = value
        }

    var warranty: WorkOrderWarrantyEntity?
        get() = session.warranty
        private set(value) {
            session.warranty = value
        }

    var payments: List<WorkOrderPaymentEntity>
        get() = session.payments
        private set(value) {
            session.payments = value
        }

    fun selectCustomer(id: Long) = form.selectCustomer(id)

    fun selectServiceProduct(item: br.com.sos.osmobile.data.local.entity.ServiceProductEntity) =
        form.selectServiceProduct(item)

    fun selectStatus(status: WorkOrderStatus) = form.selectStatus(status)

    fun onQuantityChanged(value: String) = form.onQuantityChanged(value)

    fun onUnitPriceChanged(value: String) = form.onUnitPriceChanged(value)

    fun onDiscountChanged(value: String) = form.onDiscountChanged(value)

    fun onDeliveryTypeChanged(value: String) = form.onDeliveryTypeChanged(value)

    fun onDeliveryStatusChanged(value: String) = form.onDeliveryStatusChanged(value)

    fun onDeliveryAddressChanged(value: String) = form.onDeliveryAddressChanged(value)

    fun onDeliveryFeeChanged(value: String) = form.onDeliveryFeeChanged(value)

    fun onTrackingCodeChanged(value: String) = form.onTrackingCodeChanged(value)

    fun onDeliveryNotesChanged(value: String) = form.onDeliveryNotesChanged(value)

    fun onNotesChanged(value: String) = form.onNotesChanged(value)

    fun addSelectedItem() = form.addSelectedItem()

    fun removeItem(index: Int) = form.removeItem(index)

    fun saveWorkOrder() = form.saveWorkOrder()

    fun saveWorkOrderThen(onSaved: () -> Unit) = form.saveWorkOrderThen(onSaved)

    fun saveWorkOrderThenWithId(
        initialPaymentValue: String = "",
        initialPaymentMethod: String = "",
        initialPaymentNote: String = "",
        onSaved: (Long) -> Unit,
    ) = form.saveWorkOrderThenWithId(initialPaymentValue, initialPaymentMethod, initialPaymentNote, onSaved)

    fun editWorkOrder(workOrderId: Long, message: String? = null) = form.editWorkOrder(workOrderId, message)

    fun cancelEdit() = form.cancelEdit()

    fun updateWorkOrderStatus(workOrderId: Long, status: WorkOrderStatus) =
        form.updateWorkOrderStatus(workOrderId, status)

    fun showDocument(workOrderId: Long) = prints.showDocument(workOrderId)

    fun showDocumentThen(workOrderId: Long, onLoaded: ((String) -> Unit)? = null) =
        prints.showDocumentThen(workOrderId, onLoaded)

    fun showThermalDocumentThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) =
        prints.showThermalDocumentThen(workOrderId, onLoaded)

    fun showShelfLabelThen(workOrderId: Long, onLoaded: (List<ThermalTextBlock>) -> Unit) =
        prints.showShelfLabelThen(workOrderId, onLoaded)

    fun showReceiptThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) =
        prints.showReceiptThen(workOrderId, onLoaded)

    fun showWarrantyThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) =
        prints.showWarrantyThen(workOrderId, onLoaded)

    fun showMessage(workOrder: WorkOrderSummary) = messages.showMessage(workOrder)

    fun showHistory(workOrderId: Long) {
        viewModelScope.launch {
            loadHistory(workOrderId)
        }
    }

    fun addPhoto(uri: Uri, isDocument: Boolean = false, documentDescription: String = "") =
        attachments.addPhoto(uri, isDocument, documentDescription)

    fun deletePhoto(photoId: Long) = attachments.deletePhoto(photoId)

    fun photoUri(photo: WorkOrderPhotoEntity): Uri = attachments.photoUri(photo)

    fun saveSignature(signerName: String, bitmap: Bitmap) = attachments.saveSignature(signerName, bitmap)

    fun deleteSignature() = attachments.deleteSignature()

    fun signatureUri(signature: WorkOrderSignatureEntity): Uri = attachments.signatureUri(signature)

    fun addChecklistItem(description: String) = attachments.addChecklistItem(description)

    fun setChecklistChecked(itemId: Long, checked: Boolean) = attachments.setChecklistChecked(itemId, checked)

    fun deleteChecklistItem(itemId: Long) = attachments.deleteChecklistItem(itemId)

    fun saveWarranty(days: String, terms: String) = attachments.saveWarranty(days, terms)

    fun deleteWarranty() = attachments.deleteWarranty()

    fun addPayment(value: String, method: String, note: String) = attachments.addPayment(value, method, note)

    fun deletePayment(paymentId: Long) = attachments.deletePayment(paymentId)

    fun smartSyncDriveNow() = drive.smartSyncDriveNow()

    fun importDesignFromDriveNow() = drive.importDesignFromDriveNow()

    fun importSelectedDesignFromDriveNow(selectedUris: Set<String>, doNotAlertAgain: Boolean) =
        drive.importSelectedDesignFromDriveNow(selectedUris, doNotAlertAgain)

    fun dismissDesignImportPrompt(doNotAlertAgain: Boolean) = drive.dismissDesignImportPrompt(doNotAlertAgain)

    fun buildDriveDebugReport() = drive.buildDriveDebugReport()

    private suspend fun postEditLoad(workOrderId: Long) {
        loadHistory(workOrderId)
        attachments.loadPhotos(workOrderId)
        attachments.loadSignature(workOrderId)
        attachments.loadChecklist(workOrderId)
        attachments.loadWarranty(workOrderId)
        attachments.loadPayments(workOrderId)
        val validation = driveSyncRepository.validateWorkOrderFiles(workOrderId)
        if (validation.checked) {
            attachments.loadPhotos(workOrderId)
            attachments.loadSignature(workOrderId)
            form.refreshDriveStatus(workOrderId)
            if (validation.missingItems > 0) {
                session.formState = session.formState.copy(
                    message = "Drive verificado: ${validation.missingItems} item(ns) ausente(s) marcado(s) como pendente(s).",
                )
            }
        }
        drive.checkDesignImportCandidatesOnOpen(workOrderId)
    }

    private suspend fun loadHistory(workOrderId: Long) {
        val logs = auditRepository.listForRecord("ordens_servico", workOrderId)
        session.historyText = if (logs.isEmpty()) {
            "Sem historico para esta OS."
        } else {
            logs.joinToString("\n") { "${it.acao}: ${it.detalhes.orEmpty()}" }
        }
    }

    companion object {
        fun factory(
            workOrderRepository: WorkOrderRepository,
            auditRepository: AuditRepository,
            customerRepository: CustomerRepository,
            serviceProductRepository: ServiceProductRepository,
            settingsRepository: SettingsRepository,
            photoRepository: WorkOrderPhotoRepository,
            signatureRepository: WorkOrderSignatureRepository,
            checklistRepository: WorkOrderChecklistRepository,
            warrantyRepository: WorkOrderWarrantyRepository,
            paymentRepository: WorkOrderPaymentRepository,
            stockRepository: StockRepository,
            driveSyncRepository: DriveSyncRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorkOrderViewModel(
                        workOrderRepository = workOrderRepository,
                        auditRepository = auditRepository,
                        customerRepository = customerRepository,
                        serviceProductRepository = serviceProductRepository,
                        settingsRepository = settingsRepository,
                        photoRepository = photoRepository,
                        signatureRepository = signatureRepository,
                        checklistRepository = checklistRepository,
                        warrantyRepository = warrantyRepository,
                        paymentRepository = paymentRepository,
                        stockRepository = stockRepository,
                        driveSyncRepository = driveSyncRepository,
                    ) as T
                }
            }
    }
}
