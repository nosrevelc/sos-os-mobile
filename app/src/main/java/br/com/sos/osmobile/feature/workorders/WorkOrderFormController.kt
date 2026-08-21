package br.com.sos.osmobile.feature.workorders

import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.entity.StockMovementType
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.repository.StockRepository
import br.com.sos.osmobile.data.repository.WorkOrderItemInput
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.core.format.Formatters
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.launch

class WorkOrderFormController(
    private val session: WorkOrderSessionState,
    private val workOrderRepository: WorkOrderRepository,
    private val stockRepository: StockRepository,
    private val paymentRepository: WorkOrderPaymentRepository,
    private val driveSyncRepository: DriveSyncRepository,
    private val uiStateProvider: () -> WorkOrderUiState,
    private val postEditLoad: suspend (Long) -> Unit = {},
) {

    fun selectCustomer(id: Long) {
        session.formState = session.formState.copy(selectedCustomerId = id, message = null)
    }

    fun selectServiceProduct(item: ServiceProductEntity) {
        session.formState = session.formState.copy(
            selectedServiceProductId = item.id,
            unitPrice = InputMasks.currencyFromDouble(item.unitPrice),
            message = null,
        )
    }

    fun selectStatus(status: WorkOrderStatus) {
        session.formState = session.formState.copy(status = status, message = null)
    }

    fun onQuantityChanged(value: String) {
        session.formState = session.formState.copy(quantity = InputMasks.decimal(value, integerDigits = 5, decimalDigits = 2), message = null)
    }

    fun onUnitPriceChanged(value: String) {
        session.formState = session.formState.copy(unitPrice = InputMasks.currency(value), message = null)
    }

    fun onDiscountChanged(value: String) {
        session.formState = session.formState.copy(discount = InputMasks.currency(value), message = null)
    }

    fun onDeliveryTypeChanged(value: String) {
        session.formState = session.formState.copy(deliveryType = value, message = null)
    }

    fun onDeliveryStatusChanged(value: String) {
        session.formState = session.formState.copy(deliveryStatus = value, message = null)
    }

    fun onDeliveryAddressChanged(value: String) {
        session.formState = session.formState.copy(deliveryAddress = value, message = null)
    }

    fun onDeliveryFeeChanged(value: String) {
        session.formState = session.formState.copy(deliveryFee = InputMasks.currency(value), message = null)
    }

    fun onTrackingCodeChanged(value: String) {
        session.formState = session.formState.copy(trackingCode = value, message = null)
    }

    fun onDeliveryNotesChanged(value: String) {
        session.formState = session.formState.copy(deliveryNotes = value, message = null)
    }

    fun onNotesChanged(value: String) {
        session.formState = session.formState.copy(notes = value, message = null)
    }

    fun addSelectedItem() {
        val formState = session.formState
        val service = uiStateProvider().services.firstOrNull { it.id == formState.selectedServiceProductId }
        if (service == null) {
            session.formState = formState.copy(message = "Selecione um servico/produto.")
            return
        }
        val validationMessage = WorkOrderFormValidator.validateItem(formState.quantity, formState.unitPrice)
        if (validationMessage != null) {
            session.formState = formState.copy(message = validationMessage)
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
            val available = (uiStateProvider().stockByServiceProductId[service.id] ?: 0.0) + originalReserved
            if (alreadyInDraft + quantity > available) {
                session.formState = formState.copy(message = "Saldo insuficiente para ${service.nome}. Disponivel: ${Formatters.quantity(available)}.")
                return
            }
        }
        session.formState = formState.copy(
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
        session.formState = session.formState.copy(
            items = session.formState.items.filterIndexed { itemIndex, _ -> itemIndex != index },
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
        val validationMessage = WorkOrderFormValidator.validate(session.formState)
        if (validationMessage != null) {
            session.formState = session.formState.copy(message = validationMessage)
            return
        }
        val initialPayment = WorkOrderFormValidator.parseDecimal(initialPaymentValue).takeIf { it != null && it > 0.0 }

        val scope = session.scope ?: return
        scope.launch {
            val formState = session.formState
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
                    session.formState = formState.copy(message = "Nao foi possivel atualizar a OS.")
                }
            }
        }
    }

    fun editWorkOrder(workOrderId: Long, message: String? = null) {
        val scope = session.scope ?: return
        scope.launch {
            val workOrder = workOrderRepository.findById(workOrderId) ?: return@launch
            val items = workOrderRepository.listItems(workOrderId)
            val services = uiStateProvider().services
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
            session.formState = WorkOrderFormState(
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
            postEditLoad(workOrderId)
        }
    }

    fun cancelEdit() {
        session.formState = WorkOrderFormState(message = "Edicao cancelada.")
    }

    fun updateWorkOrderStatus(workOrderId: Long, status: WorkOrderStatus) {
        val scope = session.scope ?: return
        scope.launch {
            workOrderRepository.updateStatus(workOrderId, status)
            session.listMessage = "Status da OS alterado para ${status.label}."
        }
    }

    suspend fun refreshDriveStatus(workOrderId: Long) {
        val workOrder = workOrderRepository.findById(workOrderId) ?: return
        session.formState = session.formState.copy(
            driveSyncStatus = workOrder.driveSyncStatus,
            driveSyncError = workOrder.driveSyncError.orEmpty(),
        )
    }

    private suspend fun applyStockMovements(
        workOrderId: Long,
        originalItems: List<WorkOrderDraftItem>,
        newItems: List<WorkOrderDraftItem>,
    ) {
        val serviceTypes = uiStateProvider().services.associate { it.id to it.tipo }
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

    companion object {
        fun statusFromLabel(label: String): WorkOrderStatus =
            WorkOrderStatus.entries.firstOrNull { it.label == label } ?: WorkOrderStatus.Open
    }
}
