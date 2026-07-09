package br.com.sos.osmobile.feature.workorders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.WorkOrderItemInput
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WorkOrderDraftItem(
    val serviceProductId: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
) {
    val subtotal: Double = quantity * unitPrice
}

data class WorkOrderFormState(
    val editingId: Long? = null,
    val selectedCustomerId: Long? = null,
    val selectedServiceProductId: Long? = null,
    val status: WorkOrderStatus = WorkOrderStatus.Open,
    val quantity: String = "1",
    val unitPrice: String = "",
    val notes: String = "",
    val items: List<WorkOrderDraftItem> = emptyList(),
    val message: String? = null,
)

data class WorkOrderUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val services: List<ServiceProductEntity> = emptyList(),
    val workOrders: List<WorkOrderSummary> = emptyList(),
)

class WorkOrderViewModel(
    private val workOrderRepository: WorkOrderRepository,
    private val auditRepository: AuditRepository,
    private val customerRepository: CustomerRepository,
    private val serviceProductRepository: ServiceProductRepository,
) : ViewModel() {
    val uiState: StateFlow<WorkOrderUiState> = combine(
        customerRepository.observeActive(),
        serviceProductRepository.observeActive(),
        workOrderRepository.observeSummaries(),
    ) { customers, services, workOrders ->
        WorkOrderUiState(customers = customers, services = services, workOrders = workOrders)
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

    var historyText by mutableStateOf<String?>(null)
        private set

    fun selectCustomer(id: Long) {
        formState = formState.copy(selectedCustomerId = id, message = null)
    }

    fun selectServiceProduct(item: ServiceProductEntity) {
        formState = formState.copy(
            selectedServiceProductId = item.id,
            unitPrice = item.unitPrice.toString().replace(".", ","),
            message = null,
        )
    }

    fun selectStatus(status: WorkOrderStatus) {
        formState = formState.copy(status = status, message = null)
    }

    fun onQuantityChanged(value: String) {
        formState = formState.copy(quantity = value, message = null)
    }

    fun onUnitPriceChanged(value: String) {
        formState = formState.copy(unitPrice = value, message = null)
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
        formState = formState.copy(
            items = formState.items + WorkOrderDraftItem(
                serviceProductId = service.id,
                name = service.nome,
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
        val validationMessage = WorkOrderFormValidator.validate(formState)
        if (validationMessage != null) {
            formState = formState.copy(message = validationMessage)
            return
        }

        viewModelScope.launch {
            val items = formState.items.map {
                WorkOrderItemInput(
                    serviceProductId = it.serviceProductId,
                    quantity = it.quantity,
                    practicedUnitPrice = it.unitPrice,
                )
            }
            val editingId = formState.editingId
            if (editingId == null) {
                workOrderRepository.create(
                    customerId = formState.selectedCustomerId ?: return@launch,
                    status = formState.status.label,
                    notes = formState.notes,
                    items = items,
                )
                formState = WorkOrderFormState(message = "OS criada.")
            } else {
                val updated = workOrderRepository.updateContent(
                    id = editingId,
                    customerId = formState.selectedCustomerId ?: return@launch,
                    status = formState.status,
                    notes = formState.notes,
                    items = items,
                )
                if (updated) {
                    editWorkOrder(editingId, "OS atualizada.")
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
            formState = WorkOrderFormState(
                editingId = workOrder.id,
                selectedCustomerId = workOrder.customerId,
                status = statusFromLabel(workOrder.status),
                notes = workOrder.observacoes.orEmpty(),
                items = items.map { item ->
                    val service = services.firstOrNull { it.id == item.serviceProductId }
                    WorkOrderDraftItem(
                        serviceProductId = item.serviceProductId,
                        name = service?.nome ?: "Servico/produto ${item.serviceProductId}",
                        quantity = item.quantidade,
                        unitPrice = item.practicedUnitPrice,
                    )
                },
                message = message ?: "Editando OS ${workOrder.numero}.",
            )
            loadHistory(workOrderId)
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
        viewModelScope.launch {
            documentText = workOrderRepository.generateDocumentText(workOrderId) ?: "Documento nao encontrado."
        }
    }

    fun showMessage(workOrder: WorkOrderSummary) {
        messagePhone = workOrder.customerPhone
        messageText = MessageTemplateRenderer.render(
            template = MessageTemplateRenderer.workOrderDefaultTemplate,
            tokens = mapOf(
                "nome" to workOrder.customerName,
                "os" to workOrder.number,
                "status" to workOrder.status,
                "valor" to workOrder.totalValue.toString(),
            ),
        )
    }

    fun showHistory(workOrderId: Long) {
        viewModelScope.launch {
            loadHistory(workOrderId)
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

    companion object {
        private fun statusFromLabel(label: String): WorkOrderStatus =
            WorkOrderStatus.entries.firstOrNull { it.label == label } ?: WorkOrderStatus.Open

        fun factory(
            workOrderRepository: WorkOrderRepository,
            auditRepository: AuditRepository,
            customerRepository: CustomerRepository,
            serviceProductRepository: ServiceProductRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return WorkOrderViewModel(
                        workOrderRepository = workOrderRepository,
                        auditRepository = auditRepository,
                        customerRepository = customerRepository,
                        serviceProductRepository = serviceProductRepository,
                    ) as T
                }
            }
    }
}
