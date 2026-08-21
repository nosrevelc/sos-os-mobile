package br.com.sos.osmobile.feature.workorders

import android.net.Uri
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderChecklistItemEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderWarrantyEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.entity.StockMovementType
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.drive.DriveDesignImportCandidate
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.message.PixPayloadGenerator
import br.com.sos.osmobile.data.model.DeliveryStatus
import br.com.sos.osmobile.data.model.DeliveryType
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.StockRepository
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
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PICKUP_REMINDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_DELIVERED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_NOT_DELIVERED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_ORDER_SENT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_OUT_FOR_DELIVERY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_CONFIRMED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_PENDING_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_PROOF_REQUEST_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_REVIEW_REQUEST_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_THANK_YOU_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_CANCELED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_COMPLETED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_OPEN_KEY
import br.com.sos.osmobile.data.repository.WorkOrderItemInput
import br.com.sos.osmobile.data.repository.WorkOrderChecklistRepository
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderPhotoRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.data.repository.WorkOrderSignatureRepository
import br.com.sos.osmobile.data.repository.WorkOrderWarrantyRepository
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalPrintStyle
import br.com.sos.osmobile.data.print.ThermalTextBlock
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkOrderDraftItem(
    val serviceProductId: Long,
    val name: String,
    val type: String = ServiceProductType.SERVICE,
    val quantity: Double,
    val unitPrice: Double,
) {
    val subtotal: Double = quantity * unitPrice
}

data class WorkOrderFormState(
    val editingId: Long? = null,
    val editingNumber: String? = null,
    val selectedCustomerId: Long? = null,
    val selectedServiceProductId: Long? = null,
    val status: WorkOrderStatus = WorkOrderStatus.Open,
    val quantity: String = "1",
    val unitPrice: String = "",
    val discount: String = "",
    val deliveryType: String = DeliveryType.PICKUP,
    val deliveryStatus: String = DeliveryStatus.WAITING_PICKUP,
    val deliveryAddress: String = "",
    val deliveryFee: String = "",
    val trackingCode: String = "",
    val deliveryNotes: String = "",
    val notes: String = "",
    val items: List<WorkOrderDraftItem> = emptyList(),
    val originalItems: List<WorkOrderDraftItem> = emptyList(),
    val driveSyncStatus: String = "",
    val driveSyncError: String = "",
    val message: String? = null,
)

data class WorkOrderUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val services: List<ServiceProductEntity> = emptyList(),
    val stockByServiceProductId: Map<Long, Double> = emptyMap(),
    val workOrders: List<WorkOrderSummary> = emptyList(),
    val companyName: String = "",
    val pixName: String = "",
    val pixKey: String = "",
    val quoteMinAcceptanceValue: String = "",
    val workOrderTemplate: String = MessageTemplateRenderer.workOrderDefaultTemplate,
    val workOrderStatusTemplates: Map<String, String> = emptyMap(),
    val reviewRequestTemplate: String = MessageTemplateRenderer.reviewRequestTemplate,
    val pickupReminderTemplate: String = MessageTemplateRenderer.pickupReminderTemplate,
    val paymentPendingTemplate: String = MessageTemplateRenderer.paymentPendingTemplate,
    val paymentConfirmedTemplate: String = MessageTemplateRenderer.paymentConfirmedTemplate,
    val paymentProofRequestTemplate: String = MessageTemplateRenderer.paymentProofRequestTemplate,
    val orderSentTemplate: String = MessageTemplateRenderer.orderSentTemplate,
    val outForDeliveryTemplate: String = MessageTemplateRenderer.outForDeliveryTemplate,
    val deliveredTemplate: String = MessageTemplateRenderer.deliveredTemplate,
    val notDeliveredTemplate: String = MessageTemplateRenderer.notDeliveredTemplate,
    val thankYouTemplate: String = MessageTemplateRenderer.thankYouTemplate,
    val photosEnabled: Boolean = false,
    val signatureEnabled: Boolean = false,
    val checklistEnabled: Boolean = false,
    val warrantyEnabled: Boolean = false,
    val financeEnabled: Boolean = false,
    val printBluetoothAddress: String = "",
    val printWorkOrderAuto: Boolean = false,
    val printWorkOrderCopies: Int = 0,
    val printWorkOrderHeader: String = "{empresa}\nOS {os}\n{data}",
    val printWorkOrderFooter: String = "Obrigado pela preferencia",
    val printWorkOrderStyle: ThermalPrintStyle = ThermalPrintStyle(),
)

class WorkOrderViewModel(
    private val workOrderRepository: WorkOrderRepository,
    private val auditRepository: AuditRepository,
    private val customerRepository: CustomerRepository,
    private val serviceProductRepository: ServiceProductRepository,
    private val settingsRepository: SettingsRepository,
    private val photoRepository: WorkOrderPhotoRepository,
    private val signatureRepository: WorkOrderSignatureRepository,
    private val checklistRepository: WorkOrderChecklistRepository,
    private val warrantyRepository: WorkOrderWarrantyRepository,
    private val paymentRepository: WorkOrderPaymentRepository,
    private val stockRepository: StockRepository,
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

    var formState by mutableStateOf(WorkOrderFormState())
        private set

    var listMessage by mutableStateOf<String?>(null)
        private set

    var documentText by mutableStateOf<String?>(null)
        private set

    var messageText by mutableStateOf<String?>(null)
        private set

    var messagePhone by mutableStateOf("")
        private set

    var driveDebugReport by mutableStateOf("")
        private set

    var pendingDesignImportCandidates by mutableStateOf<List<DriveDesignImportCandidate>>(emptyList())
        private set

    var historyText by mutableStateOf<String?>(null)
        private set

    var photos by mutableStateOf<List<WorkOrderPhotoEntity>>(emptyList())
        private set

    var signature by mutableStateOf<WorkOrderSignatureEntity?>(null)
        private set

    var checklist by mutableStateOf<List<WorkOrderChecklistItemEntity>>(emptyList())
        private set

    var warranty by mutableStateOf<WorkOrderWarrantyEntity?>(null)
        private set

    var payments by mutableStateOf<List<WorkOrderPaymentEntity>>(emptyList())
        private set

    fun selectCustomer(id: Long) {
        formState = formState.copy(selectedCustomerId = id, message = null)
    }

    fun selectServiceProduct(item: ServiceProductEntity) {
        formState = formState.copy(
            selectedServiceProductId = item.id,
            unitPrice = InputMasks.currencyFromDouble(item.unitPrice),
            message = null,
        )
    }

    fun selectStatus(status: WorkOrderStatus) {
        formState = formState.copy(status = status, message = null)
    }

    fun onQuantityChanged(value: String) {
        formState = formState.copy(quantity = InputMasks.decimal(value, integerDigits = 5, decimalDigits = 2), message = null)
    }

    fun onUnitPriceChanged(value: String) {
        formState = formState.copy(unitPrice = InputMasks.currency(value), message = null)
    }

    fun onDiscountChanged(value: String) {
        formState = formState.copy(discount = InputMasks.currency(value), message = null)
    }

    fun onDeliveryTypeChanged(value: String) {
        formState = formState.copy(deliveryType = value, message = null)
    }

    fun onDeliveryStatusChanged(value: String) {
        formState = formState.copy(deliveryStatus = value, message = null)
    }

    fun onDeliveryAddressChanged(value: String) {
        formState = formState.copy(deliveryAddress = value, message = null)
    }

    fun onDeliveryFeeChanged(value: String) {
        formState = formState.copy(deliveryFee = InputMasks.currency(value), message = null)
    }

    fun onTrackingCodeChanged(value: String) {
        formState = formState.copy(trackingCode = value, message = null)
    }

    fun onDeliveryNotesChanged(value: String) {
        formState = formState.copy(deliveryNotes = value, message = null)
    }

    fun onNotesChanged(value: String) {
        formState = formState.copy(notes = value, message = null)
    }

    fun addSelectedItem() {
        val service = uiState.value.services.firstOrNull { it.id == formState.selectedServiceProductId }
        if (service == null) {
            formState = formState.copy(message = "Selecione um servico/produto.")
            return
        }
        val validationMessage = WorkOrderFormValidator.validateItem(formState.quantity, formState.unitPrice)
        if (validationMessage != null) {
            formState = formState.copy(message = validationMessage)
            return
        }
        val quantity = WorkOrderFormValidator.parseDecimal(formState.quantity) ?: return
        val unitPrice = WorkOrderFormValidator.parseDecimal(formState.unitPrice) ?: return
        if (service.tipo != ServiceProductType.SERVICE) {
            val alreadyInDraft = formState.items
                .filter { it.serviceProductId == service.id }
                .sumOf { it.quantity }
            val originalReserved = formState.originalItems
                .filter { it.serviceProductId == service.id }
                .sumOf { it.quantity }
            val available = (uiState.value.stockByServiceProductId[service.id] ?: 0.0) + originalReserved
            if (alreadyInDraft + quantity > available) {
                formState = formState.copy(message = "Saldo insuficiente para ${service.nome}. Disponivel: ${formatQuantity(available)}.")
                return
            }
        }
        formState = formState.copy(
            items = formState.items + WorkOrderDraftItem(
                serviceProductId = service.id,
                name = service.nome,
                type = service.tipo,
                quantity = quantity,
                unitPrice = unitPrice,
            ),
            selectedServiceProductId = null,
            quantity = "1",
            unitPrice = "",
            message = null,
        )
    }

    fun removeItem(index: Int) {
        formState = formState.copy(
            items = formState.items.filterIndexed { itemIndex, _ -> itemIndex != index },
            message = null,
        )
    }

    fun saveWorkOrder() {
        saveWorkOrderInternal()
    }

    fun saveWorkOrderThen(onSaved: () -> Unit) {
        saveWorkOrderInternal { onSaved() }
    }

    fun saveWorkOrderThenWithId(
        initialPaymentValue: String = "",
        initialPaymentMethod: String = "",
        initialPaymentNote: String = "",
        onSaved: (Long) -> Unit,
    ) {
        saveWorkOrderInternal(
            initialPaymentValue = initialPaymentValue,
            initialPaymentMethod = initialPaymentMethod,
            initialPaymentNote = initialPaymentNote,
            onSaved = onSaved,
        )
    }

    private fun saveWorkOrderInternal(
        initialPaymentValue: String = "",
        initialPaymentMethod: String = "",
        initialPaymentNote: String = "",
        onSaved: ((Long) -> Unit)? = null,
    ) {
        val validationMessage = WorkOrderFormValidator.validate(formState)
        if (validationMessage != null) {
            formState = formState.copy(message = validationMessage)
            return
        }
        val initialPayment = WorkOrderFormValidator.parseDecimal(initialPaymentValue).takeIf { it != null && it > 0.0 }

        viewModelScope.launch {
            val items = formState.items.map {
                WorkOrderItemInput(
                    serviceProductId = it.serviceProductId,
                    quantity = it.quantity,
                    practicedUnitPrice = it.unitPrice,
                )
            }
            val discount = WorkOrderFormValidator.parseDecimal(formState.discount) ?: 0.0
            val deliveryFee = WorkOrderFormValidator.parseDecimal(formState.deliveryFee) ?: 0.0
            val editingId = formState.editingId
            if (editingId == null) {
                val createdId = workOrderRepository.create(
                    customerId = formState.selectedCustomerId ?: return@launch,
                    status = formState.status.label,
                    notes = formState.notes,
                    items = items,
                    discountValue = discount,
                    deliveryType = formState.deliveryType,
                    deliveryStatus = formState.deliveryStatus,
                    deliveryAddress = formState.deliveryAddress,
                    deliveryFee = deliveryFee,
                    trackingCode = formState.trackingCode,
                    deliveryNotes = formState.deliveryNotes,
                )
                applyStockMovements(createdId, emptyList(), formState.items)
                if (initialPayment != null) {
                    paymentRepository.addPayment(createdId, initialPayment, initialPaymentMethod, initialPaymentNote)
                }
                editWorkOrder(createdId, "OS criada com sucesso.")
                driveSyncRepository.syncWorkOrder(createdId)
                refreshDriveStatus(createdId)
                onSaved?.invoke(createdId)
            } else {
                val updated = workOrderRepository.updateContent(
                    id = editingId,
                    customerId = formState.selectedCustomerId ?: return@launch,
                    status = formState.status,
                    notes = formState.notes,
                    items = items,
                    discountValue = discount,
                    deliveryType = formState.deliveryType,
                    deliveryStatus = formState.deliveryStatus,
                    deliveryAddress = formState.deliveryAddress,
                    deliveryFee = deliveryFee,
                    trackingCode = formState.trackingCode,
                    deliveryNotes = formState.deliveryNotes,
                )
                if (updated) {
                    applyStockMovements(editingId, formState.originalItems, formState.items)
                    editWorkOrder(editingId, "OS atualizada.")
                    onSaved?.invoke(editingId)
                } else {
                    formState = formState.copy(message = "Nao foi possivel atualizar a OS.")
                }
            }
        }
    }

    fun editWorkOrder(workOrderId: Long, message: String? = null) {
        viewModelScope.launch {
            val workOrder = workOrderRepository.findById(workOrderId) ?: return@launch
            val items = workOrderRepository.listItems(workOrderId)
            val services = uiState.value.services
            val draftItems = items.map { item ->
                val service = services.firstOrNull { it.id == item.serviceProductId }
                WorkOrderDraftItem(
                    serviceProductId = item.serviceProductId,
                    name = service?.nome ?: "Servico/produto ${item.serviceProductId}",
                    type = service?.tipo ?: ServiceProductType.SERVICE,
                    quantity = item.quantidade,
                    unitPrice = item.practicedUnitPrice,
                )
            }
            formState = WorkOrderFormState(
                editingId = workOrder.id,
                editingNumber = workOrder.numero,
                selectedCustomerId = workOrder.customerId,
                status = statusFromLabel(workOrder.status),
                notes = workOrder.observacoes.orEmpty(),
                discount = InputMasks.currencyFromDouble(workOrder.discountValue),
                deliveryType = workOrder.deliveryType,
                deliveryStatus = workOrder.deliveryStatus,
                deliveryAddress = workOrder.deliveryAddress.orEmpty(),
                deliveryFee = InputMasks.currencyFromDouble(workOrder.deliveryFee),
                trackingCode = workOrder.trackingCode.orEmpty(),
                deliveryNotes = workOrder.deliveryNotes.orEmpty(),
                items = draftItems,
                originalItems = draftItems,
                driveSyncStatus = workOrder.driveSyncStatus,
                driveSyncError = workOrder.driveSyncError.orEmpty(),
                message = message ?: "Editando OS ${workOrder.numero}.",
            )
            loadHistory(workOrderId)
            loadPhotos(workOrderId)
            loadSignature(workOrderId)
            loadChecklist(workOrderId)
            loadWarranty(workOrderId)
            loadPayments(workOrderId)
            val validation = driveSyncRepository.validateWorkOrderFiles(workOrderId)
            if (validation.checked) {
                loadPhotos(workOrderId)
                loadSignature(workOrderId)
                refreshDriveStatus(workOrderId)
                if (validation.missingItems > 0) {
                    formState = formState.copy(
                        message = "Drive verificado: ${validation.missingItems} item(ns) ausente(s) marcado(s) como pendente(s).",
                    )
                }
            }
            checkDesignImportCandidatesOnOpen(workOrderId)
        }
    }

    fun cancelEdit() {
        formState = WorkOrderFormState(message = "Edicao cancelada.")
    }

    fun updateWorkOrderStatus(workOrderId: Long, status: WorkOrderStatus) {
        viewModelScope.launch {
            workOrderRepository.updateStatus(workOrderId, status)
            listMessage = "Status da OS alterado para ${status.label}."
        }
    }

    fun showDocument(workOrderId: Long) {
        showDocumentThen(workOrderId)
    }

    fun showDocumentThen(workOrderId: Long, onLoaded: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            val text = workOrderRepository.generateDocumentText(workOrderId) ?: "Documento nao encontrado."
            documentText = text
            onLoaded?.invoke(text)
        }
    }

    fun showThermalDocumentThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) {
        viewModelScope.launch {
            val settings = uiState.value
            val content = workOrderRepository.generateThermalPrintContent(
                id = workOrderId,
                headerTemplate = settings.printWorkOrderHeader,
                footerTemplate = settings.printWorkOrderFooter,
                companyName = settings.companyName,
            ) ?: ThermalPrintContent(body = "Documento nao encontrado.")
            documentText = content.asText()
            onLoaded(content)
        }
    }

    fun showShelfLabelThen(workOrderId: Long, onLoaded: (List<ThermalTextBlock>) -> Unit) {
        viewModelScope.launch {
            val blocks = workOrderRepository.generateShelfLabelBlocks(workOrderId)
                ?: listOf(ThermalTextBlock(text = "Etiqueta nao encontrada."))
            documentText = blocks.joinToString(separator = "\n") { it.text }
            onLoaded(blocks)
        }
    }

    fun showReceiptThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) {
        viewModelScope.launch {
            val settings = uiState.value
            val content = workOrderRepository.generateReceiptPrintContent(
                id = workOrderId,
                headerTemplate = settings.printWorkOrderHeader,
                footerTemplate = settings.printWorkOrderFooter,
                companyName = settings.companyName,
            ) ?: ThermalPrintContent(body = "Recibo nao encontrado.")
            documentText = content.asText()
            onLoaded(content)
        }
    }

    fun showWarrantyThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) {
        viewModelScope.launch {
            val settings = uiState.value
            val content = workOrderRepository.generateWarrantyPrintContent(
                id = workOrderId,
                headerTemplate = settings.printWorkOrderHeader,
                footerTemplate = settings.printWorkOrderFooter,
                companyName = settings.companyName,
                warrantyDays = warranty?.warrantyDays,
                warrantyTerms = warranty?.termos,
            ) ?: ThermalPrintContent(body = "Garantia nao encontrada.")
            documentText = content.asText()
            onLoaded(content)
        }
    }

    fun showMessage(workOrder: WorkOrderSummary) {
        viewModelScope.launch {
            val paidTotal = paymentRepository.listByWorkOrder(workOrder.id).sumOf { it.valor }
            val entity = workOrderRepository.findById(workOrder.id)
            val discount = entity?.discountValue ?: 0.0
            val itemTokens = MessageTemplateRenderer.itemTokens(workOrderRepository.listDocumentItems(workOrder.id))
            messagePhone = workOrder.customerPhone
            messageText = MessageTemplateRenderer.render(
                template = uiState.value.workOrderStatusTemplates[workOrder.status] ?: uiState.value.workOrderTemplate,
                tokens = workOrderMessageTokens(
                    customerName = workOrder.customerName,
                    customerPhone = workOrder.customerPhone,
                    workOrderNumber = workOrder.number,
                    status = workOrder.status,
                    totalValue = workOrder.totalValue,
                    subtotalValue = workOrder.totalValue + discount,
                    discountValue = discount,
                    deliveryType = entity?.deliveryType.orEmpty(),
                    deliveryStatus = entity?.deliveryStatus.orEmpty(),
                    deliveryAddress = entity?.deliveryAddress.orEmpty(),
                    deliveryFee = entity?.deliveryFee ?: 0.0,
                    trackingCode = entity?.trackingCode.orEmpty(),
                    paidTotal = paidTotal,
                ) + itemTokens,
            )
        }
    }

    fun showHistory(workOrderId: Long) {
        viewModelScope.launch {
            loadHistory(workOrderId)
        }
    }

    fun addPhoto(uri: Uri, isDocument: Boolean = false, documentDescription: String = "") {
        val workOrderId = formState.editingId ?: run {
            formState = formState.copy(message = "Salve a OS antes de adicionar anexos.")
            return
        }
        viewModelScope.launch {
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
                formState = formState.copy(
                    message = listOfNotNull(syncError?.let { "$savedMessage Sync pendente: $it" } ?: savedMessage, driveWarning).joinToString(" "),
                )
            }.onFailure {
                formState = formState.copy(message = "Nao foi possivel adicionar o anexo: ${it.message.orEmpty()}")
            }
        }
    }

    fun smartSyncDriveNow() {
        val workOrderId = formState.editingId ?: run {
            formState = formState.copy(message = "Salve a OS antes de sincronizar.")
            return
        }
        viewModelScope.launch {
            formState = formState.copy(message = "Sincronizando Drive...")
            val result = driveSyncRepository.smartSyncWorkOrder(workOrderId)
            loadPhotos(workOrderId)
            loadSignature(workOrderId)
            refreshDriveStatus(workOrderId)
            formState = formState.copy(
                message = result.fold(
                    onSuccess = {
                        val baseMessage = if (it.rebuilt) "Drive reconstruido e sincronizado." else "Drive sincronizado."
                        listOfNotNull(baseMessage, it.warning).joinToString(" ")
                    },
                    onFailure = { "Falha ao sincronizar Drive: ${it.message.orEmpty()}" },
                ),
            )
        }
    }

    fun importDesignFromDriveNow() {
        val workOrderId = formState.editingId ?: run {
            formState = formState.copy(message = "Salve a OS antes de importar arquivos do Drive.")
            return
        }
        viewModelScope.launch {
            formState = formState.copy(message = "Buscando arquivos na pasta Design do Drive...")
            val result = driveSyncRepository.importDesignFiles(workOrderId)
            loadPhotos(workOrderId)
            refreshDriveStatus(workOrderId)
            formState = formState.copy(
                message = result.fold(
                    onSuccess = {
                        if (it.foundFiles == 0) {
                            "Pasta Design pronta no Drive. Coloque os arquivos nela e toque em importar novamente."
                        } else {
                            "Design importado: ${it.importedFiles} novo(s), ${it.alreadyImportedFiles} ja existente(s)."
                        }
                    },
                    onFailure = { "Falha ao importar Design do Drive: ${it.message.orEmpty()}" },
                ),
            )
        }
    }

    fun importSelectedDesignFromDriveNow(selectedUris: Set<String>, doNotAlertAgain: Boolean) {
        val workOrderId = formState.editingId ?: return
        viewModelScope.launch {
            if (doNotAlertAgain) disableDesignImportAlert(workOrderId)
            pendingDesignImportCandidates = emptyList()
            if (selectedUris.isEmpty()) {
                formState = formState.copy(message = "Nenhum arquivo Design selecionado.")
                return@launch
            }
            formState = formState.copy(message = "Importando arquivos selecionados do Drive...")
            val result = driveSyncRepository.importDesignFiles(workOrderId, selectedUris)
            loadPhotos(workOrderId)
            refreshDriveStatus(workOrderId)
            formState = formState.copy(
                message = result.fold(
                    onSuccess = { "Design importado: ${it.importedFiles} novo(s), ${it.alreadyImportedFiles} ja existente(s)." },
                    onFailure = { "Falha ao importar Design do Drive: ${it.message.orEmpty()}" },
                ),
            )
        }
    }

    fun dismissDesignImportPrompt(doNotAlertAgain: Boolean) {
        val workOrderId = formState.editingId
        viewModelScope.launch {
            if (doNotAlertAgain && workOrderId != null) disableDesignImportAlert(workOrderId)
            pendingDesignImportCandidates = emptyList()
        }
    }

    fun buildDriveDebugReport() {
        val workOrderId = formState.editingId ?: run {
            formState = formState.copy(message = "Salve a OS antes de gerar debug do Drive.")
            return
        }
        viewModelScope.launch {
            formState = formState.copy(message = "Gerando debug do Drive...")
            driveDebugReport = driveSyncRepository.buildWorkOrderDebugReport(workOrderId)
            formState = formState.copy(message = "Debug do Drive gerado. Copie e envie para analise.")
        }
    }

    private suspend fun checkDesignImportCandidatesOnOpen(workOrderId: Long) {
        if (settingsRepository.getString(designImportAlertDisabledKey(workOrderId)) == "true") return
        driveSyncRepository.listDesignImportCandidates(workOrderId)
            .onSuccess { candidates ->
                pendingDesignImportCandidates = candidates
            }
    }

    private suspend fun disableDesignImportAlert(workOrderId: Long) {
        settingsRepository.set(designImportAlertDisabledKey(workOrderId), "true")
    }

    private fun designImportAlertDisabledKey(workOrderId: Long): String =
        "drive_design_alert_disabled_$workOrderId"

    fun deletePhoto(photoId: Long) {
        val workOrderId = formState.editingId ?: return
        viewModelScope.launch {
            photoRepository.deletePhoto(photoId)
            loadPhotos(workOrderId)
            formState = formState.copy(message = "Foto removida.")
        }
    }

    fun photoUri(photo: WorkOrderPhotoEntity): Uri = photoRepository.uriFor(photo)

    fun saveSignature(signerName: String, bitmap: Bitmap) {
        val workOrderId = formState.editingId ?: run {
            formState = formState.copy(message = "Salve a OS antes de assinar.")
            return
        }
        viewModelScope.launch {
            var syncError: String? = null
            signatureRepository.saveSignature(workOrderId, signerName, bitmap)
            syncError = driveSyncRepository.syncSignature(workOrderId).exceptionOrNull()?.message
            loadSignature(workOrderId)
            refreshDriveStatus(workOrderId)
            formState = formState.copy(
                message = syncError?.let { "Assinatura salva. Sync pendente: $it" } ?: "Assinatura salva e sincronizada.",
            )
        }
    }

    fun deleteSignature() {
        val workOrderId = formState.editingId ?: return
        viewModelScope.launch {
            signatureRepository.deleteSignature(workOrderId)
            loadSignature(workOrderId)
            formState = formState.copy(message = "Assinatura removida.")
        }
    }

    fun signatureUri(signature: WorkOrderSignatureEntity): Uri = signatureRepository.uriFor(signature)

    fun addChecklistItem(description: String) {
        val workOrderId = formState.editingId ?: run {
            formState = formState.copy(message = "Salve a OS antes de adicionar checklist.")
            return
        }
        if (description.isBlank()) {
            formState = formState.copy(message = "Informe o item do checklist.")
            return
        }
        viewModelScope.launch {
            checklistRepository.addItem(workOrderId, description)
            loadChecklist(workOrderId)
            formState = formState.copy(message = "Item adicionado ao checklist.")
        }
    }

    fun setChecklistChecked(itemId: Long, checked: Boolean) {
        val workOrderId = formState.editingId ?: return
        viewModelScope.launch {
            checklistRepository.setChecked(itemId, checked)
            loadChecklist(workOrderId)
        }
    }

    fun deleteChecklistItem(itemId: Long) {
        val workOrderId = formState.editingId ?: return
        viewModelScope.launch {
            checklistRepository.deleteItem(itemId)
            loadChecklist(workOrderId)
        }
    }

    fun saveWarranty(days: String, terms: String) {
        val workOrderId = formState.editingId ?: run {
            formState = formState.copy(message = "Salve a OS antes de criar garantia.")
            return
        }
        viewModelScope.launch {
            warrantyRepository.save(workOrderId, days.toIntOrNull() ?: 0, terms)
            loadWarranty(workOrderId)
            formState = formState.copy(message = "Garantia salva.")
        }
    }

    fun deleteWarranty() {
        val workOrderId = formState.editingId ?: return
        viewModelScope.launch {
            warrantyRepository.delete(workOrderId)
            loadWarranty(workOrderId)
            formState = formState.copy(message = "Garantia removida.")
        }
    }

    fun addPayment(value: String, method: String, note: String) {
        val workOrderId = formState.editingId ?: run {
            formState = formState.copy(message = "Salve a OS antes de registrar pagamento.")
            return
        }
        val parsedValue = WorkOrderFormValidator.parseDecimal(value)
        if (parsedValue == null || parsedValue <= 0.0) {
            formState = formState.copy(message = "Informe um valor de pagamento valido.")
            return
        }
        viewModelScope.launch {
            paymentRepository.addPayment(workOrderId, parsedValue, method, note)
            loadPayments(workOrderId)
            formState = formState.copy(message = "Pagamento registrado.")
        }
    }

    fun deletePayment(paymentId: Long) {
        val workOrderId = formState.editingId ?: return
        viewModelScope.launch {
            paymentRepository.deletePayment(paymentId)
            loadPayments(workOrderId)
        }
    }

    private suspend fun loadHistory(workOrderId: Long) {
        val logs = auditRepository.listForRecord("ordens_servico", workOrderId)
        historyText = if (logs.isEmpty()) {
            "Sem historico para esta OS."
        } else {
            logs.joinToString("\n") { "${it.acao}: ${it.detalhes.orEmpty()}" }
        }
    }

    private suspend fun applyStockMovements(
        workOrderId: Long,
        originalItems: List<WorkOrderDraftItem>,
        newItems: List<WorkOrderDraftItem>,
    ) {
        val serviceTypes = uiState.value.services.associate { it.id to it.tipo }
        val originalByService = WorkOrderStockTotals.of(originalItems, serviceTypes)
        val newByService = WorkOrderStockTotals.of(newItems, serviceTypes)
        (originalByService.keys + newByService.keys).forEach { serviceProductId ->
            val delta = (newByService[serviceProductId] ?: 0.0) - (originalByService[serviceProductId] ?: 0.0)
            when {
                delta > 0.0 -> stockRepository.move(
                    serviceProductId = serviceProductId,
                    type = StockMovementType.OUT,
                    quantity = delta,
                    reason = "Baixa pela OS $workOrderId",
                    workOrderId = workOrderId,
                )
                delta < 0.0 -> stockRepository.move(
                    serviceProductId = serviceProductId,
                    type = StockMovementType.IN,
                    quantity = -delta,
                    reason = "Correcao pela OS $workOrderId",
                    workOrderId = workOrderId,
                )
            }
        }
    }

    private suspend fun loadPhotos(workOrderId: Long) {
        photos = photoRepository.listByWorkOrder(workOrderId)
    }

    private suspend fun loadSignature(workOrderId: Long) {
        signature = signatureRepository.findByWorkOrder(workOrderId)
    }

    private suspend fun loadChecklist(workOrderId: Long) {
        checklist = checklistRepository.listByWorkOrder(workOrderId)
    }

    private suspend fun loadWarranty(workOrderId: Long) {
        warranty = warrantyRepository.findByWorkOrder(workOrderId)
    }

    private suspend fun loadPayments(workOrderId: Long) {
        payments = paymentRepository.listByWorkOrder(workOrderId)
    }

    private suspend fun refreshDriveStatus(workOrderId: Long) {
        val workOrder = workOrderRepository.findById(workOrderId) ?: return
        formState = formState.copy(
            driveSyncStatus = workOrder.driveSyncStatus,
            driveSyncError = workOrder.driveSyncError.orEmpty(),
        )
    }

    private fun workOrderMessageTokens(
        customerName: String,
        customerPhone: String,
        workOrderNumber: String,
        status: String,
        totalValue: Double,
        subtotalValue: Double,
        discountValue: Double,
        deliveryType: String,
        deliveryStatus: String,
        deliveryAddress: String,
        deliveryFee: Double,
        trackingCode: String,
        paidTotal: Double,
    ): Map<String, String> {
        val balance = (totalValue - paidTotal).coerceAtLeast(0.0)
        val paymentStatus = when {
            totalValue <= 0.0 || paidTotal <= 0.0 -> "Pendente"
            paidTotal + 0.009 >= totalValue -> "Pago"
            else -> "Parcial"
        }
        return mapOf(
            "nome" to customerName,
            "telefone" to customerPhone,
            "cpf" to "",
            "os" to workOrderNumber,
            "orcamento" to "",
            "status" to status,
            "valor" to InputMasks.currencyFromDouble(totalValue),
            "subtotal" to InputMasks.currencyFromDouble(subtotalValue),
            "desconto" to InputMasks.currencyFromDouble(discountValue),
            "linha_desconto" to if (discountValue > 0.0) "Desconto: ${InputMasks.currencyFromDouble(discountValue)}" else "",
            "valor_minimo_aceite" to uiState.value.quoteMinAcceptanceValue,
            "tipo_entrega" to deliveryType,
            "status_entrega" to deliveryStatus,
            "endereco_entrega" to deliveryAddress,
            "taxa_entrega" to InputMasks.currencyFromDouble(deliveryFee),
            "codigo_rastreio" to trackingCode,
            "valor_pago" to InputMasks.currencyFromDouble(paidTotal),
            "saldo" to InputMasks.currencyFromDouble(balance),
            "status_pagamento" to paymentStatus,
            "empresa" to uiState.value.companyName,
            "data" to "",
            "PIX" to PixPayloadGenerator.generate(uiState.value.pixKey, uiState.value.pixName, balance.takeIf { it > 0.0 } ?: totalValue),
            "PIX_SEM_VALOR" to PixPayloadGenerator.generateOpenAmount(uiState.value.pixKey, uiState.value.pixName),
            "PIX_QR" to "",
        )
    }

    companion object {
        private fun statusFromLabel(label: String): WorkOrderStatus =
            WorkOrderStatus.entries.firstOrNull { it.label == label } ?: WorkOrderStatus.Open

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

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
