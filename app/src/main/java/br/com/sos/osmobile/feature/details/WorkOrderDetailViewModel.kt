package br.com.sos.osmobile.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.model.DocumentItem
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class WorkOrderDetailUiState(
    val workOrder: WorkOrderSummary? = null,
    val items: List<DocumentItem> = emptyList(),
    val history: List<String> = emptyList(),
)

class WorkOrderDetailViewModel(
    private val workOrderId: Long,
    private val workOrderRepository: WorkOrderRepository,
    private val auditRepository: AuditRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkOrderDetailUiState())
    val uiState: StateFlow<WorkOrderDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = WorkOrderDetailUiState(
                workOrder = workOrderRepository.findSummaryById(workOrderId),
                items = workOrderRepository.listDocumentItems(workOrderId),
                history = auditRepository.listForRecord("ordens_servico", workOrderId).map { it.acao },
            )
        }
    }

    companion object {
        fun factory(workOrderId: Long, workOrderRepository: WorkOrderRepository, auditRepository: AuditRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    WorkOrderDetailViewModel(workOrderId, workOrderRepository, auditRepository) as T
            }
    }
}
