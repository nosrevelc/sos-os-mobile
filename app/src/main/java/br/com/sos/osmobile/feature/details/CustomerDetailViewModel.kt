package br.com.sos.osmobile.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.QuoteRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class CustomerDetailUiState(
    val customer: CustomerEntity? = null,
    val quotes: List<QuoteSummary> = emptyList(),
    val workOrders: List<WorkOrderSummary> = emptyList(),
)

class CustomerDetailViewModel(
    private val customerId: Long,
    private val customerRepository: CustomerRepository,
    private val quoteRepository: QuoteRepository,
    private val workOrderRepository: WorkOrderRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CustomerDetailUiState())
    val uiState: StateFlow<CustomerDetailUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CustomerDetailUiState(
                customer = customerRepository.findById(customerId),
                quotes = quoteRepository.listSummariesByCustomer(customerId),
                workOrders = workOrderRepository.listSummariesByCustomer(customerId),
            )
        }
    }

    companion object {
        fun factory(
            customerId: Long,
            customerRepository: CustomerRepository,
            quoteRepository: QuoteRepository,
            workOrderRepository: WorkOrderRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CustomerDetailViewModel(customerId, customerRepository, quoteRepository, workOrderRepository) as T
            }
    }
}
