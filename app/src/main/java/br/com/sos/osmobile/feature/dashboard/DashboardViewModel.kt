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
import br.com.sos.osmobile.ui.input.InputMasks
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class DashboardDateRange(
    val startDate: String,
    val endDate: String,
)

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
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val today = LocalDate.now()

    var query by mutableStateOf("")
        private set
    private val queryFlow = MutableStateFlow("")

    var startDate by mutableStateOf(today.withDayOfMonth(1).format(dateFormatter))
        private set

    var endDate by mutableStateOf(today.format(dateFormatter))
        private set

    val todayLabel: String = today.format(displayFormatter)

    private val dateRangeFlow = MutableStateFlow(DashboardDateRange(startDate, endDate))

    val metrics: StateFlow<DashboardMetrics> = combine(
        workOrderRepository.observeSummaries(),
        quoteRepository.observeSummaries(),
        workOrderRepository.observeServiceUsage(),
        dateRangeFlow,
    ) { workOrders, quotes, serviceUsage, range ->
        val start = parseDate(range.startDate) ?: today.withDayOfMonth(1)
        val end = parseDate(range.endDate) ?: today
        val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        DashboardMetricsCalculator.calculate(workOrders, quotes, serviceUsage, startMillis, endMillis)
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

    fun onStartDateChanged(value: String) {
        startDate = InputMasks.dateIso(value)
        dateRangeFlow.value = DashboardDateRange(startDate, endDate)
    }

    fun onEndDateChanged(value: String) {
        endDate = InputMasks.dateIso(value)
        dateRangeFlow.value = DashboardDateRange(startDate, endDate)
    }

    fun resetToCurrentMonth() {
        startDate = today.withDayOfMonth(1).format(dateFormatter)
        endDate = today.format(dateFormatter)
        dateRangeFlow.value = DashboardDateRange(startDate, endDate)
    }

    private fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value, dateFormatter) }.getOrNull()

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
