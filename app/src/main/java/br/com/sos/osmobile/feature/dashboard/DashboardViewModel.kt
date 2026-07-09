package br.com.sos.osmobile.feature.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DashboardSearchResult(
    val customers: List<CustomerEntity> = emptyList(),
    val quotes: List<QuoteSummary> = emptyList(),
    val workOrders: List<WorkOrderSummary> = emptyList(),
)

class DashboardViewModel(
    workOrderRepository: WorkOrderRepository,
    quoteRepository: QuoteRepository,
    customerRepository: CustomerRepository,
) : ViewModel() {
    var query by mutableStateOf("")
        private set
    private val queryFlow = MutableStateFlow("")

    val metrics: StateFlow<DashboardMetrics> = combine(
        workOrderRepository.observeSummaries(),
        quoteRepository.observeSummaries(),
    ) { workOrders, quotes ->
        DashboardMetricsCalculator.calculate(workOrders, quotes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardMetrics(0, 0.0, 0, 0.0, 0, 0))

    val searchResult: StateFlow<DashboardSearchResult> = combine(
        customerRepository.observeActive(),
        quoteRepository.observeSummaries(),
        workOrderRepository.observeSummaries(),
        queryFlow,
    ) { customers, quotes, workOrders, currentQuery ->
        val term = currentQuery.lowercase().trim()
        if (term.isBlank()) {
            DashboardSearchResult()
        } else {
            DashboardSearchResult(
                customers = customers.filter {
                    it.nome.lowercase().contains(term) || it.telefone.contains(term) || it.cpfCnpj.orEmpty().contains(term)
                },
                quotes = quotes.filter {
                    it.number.lowercase().contains(term) || it.customerName.lowercase().contains(term) || it.customerPhone.contains(term) || it.status.lowercase().contains(term)
                },
                workOrders = workOrders.filter {
                    it.number.lowercase().contains(term) || it.customerName.lowercase().contains(term) || it.customerPhone.contains(term) || it.status.lowercase().contains(term)
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardSearchResult())

    fun onQueryChanged(value: String) {
        query = value
        queryFlow.value = value
    }

    companion object {
        fun factory(
            workOrderRepository: WorkOrderRepository,
            quoteRepository: QuoteRepository,
            customerRepository: CustomerRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    DashboardViewModel(workOrderRepository, quoteRepository, customerRepository) as T
            }
    }
}
