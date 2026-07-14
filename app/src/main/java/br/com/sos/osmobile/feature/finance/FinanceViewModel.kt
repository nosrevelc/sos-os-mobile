package br.com.sos.osmobile.feature.finance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FinancePendingItem(
    val workOrderId: Long,
    val number: String,
    val customerName: String,
    val total: Double,
    val paid: Double,
) {
    val balance: Double = (total - paid).coerceAtLeast(0.0)
}

data class FinanceUiState(
    val paidInPeriod: Double = 0.0,
    val totalInPeriod: Double = 0.0,
    val pendingInPeriod: Double = 0.0,
    val paidToday: Double = 0.0,
    val paidThisMonth: Double = 0.0,
    val byMethod: Map<String, Double> = emptyMap(),
    val pendingItems: List<FinancePendingItem> = emptyList(),
)

class FinanceViewModel(
    workOrderRepository: WorkOrderRepository,
    paymentRepository: WorkOrderPaymentRepository,
) : ViewModel() {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone)

    var startDate by mutableStateOf(today.withDayOfMonth(1).format(formatter))
        private set
    var endDate by mutableStateOf(today.format(formatter))
        private set

    val uiState: StateFlow<FinanceUiState> = combine(
        workOrderRepository.observeSummaries(),
        paymentRepository.observeAll(),
    ) { workOrders, payments ->
        buildState(workOrders, payments)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinanceUiState())

    fun onStartDateChanged(value: String) {
        startDate = value
    }

    fun onEndDateChanged(value: String) {
        endDate = value
    }

    fun useToday() {
        startDate = today.format(formatter)
        endDate = today.format(formatter)
    }

    fun useCurrentMonth() {
        startDate = today.withDayOfMonth(1).format(formatter)
        endDate = today.format(formatter)
    }

    private fun buildState(
        workOrders: List<WorkOrderSummary>,
        payments: List<WorkOrderPaymentEntity>,
    ): FinanceUiState {
        val start = parseDate(startDate) ?: today.withDayOfMonth(1)
        val end = parseDate(endDate) ?: today
        val startMillis = start.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMillis = end.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val todayEnd = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val paidByWorkOrder = payments.groupBy { it.workOrderId }.mapValues { entry -> entry.value.sumOf { it.valor } }
        val periodPayments = payments.filter { it.paidAt in startMillis..endMillis }
        val periodOrders = workOrders.filter { it.openedAt in startMillis..endMillis }
        val pendingItems = periodOrders.mapNotNull { order ->
            val paid = paidByWorkOrder[order.id] ?: 0.0
            FinancePendingItem(order.id, order.number, order.customerName, order.totalValue, paid).takeIf { it.balance > 0.0 }
        }

        return FinanceUiState(
            paidInPeriod = periodPayments.sumOf { it.valor },
            totalInPeriod = periodOrders.sumOf { it.totalValue },
            pendingInPeriod = pendingItems.sumOf { it.balance },
            paidToday = payments.filter { it.paidAt in todayStart..todayEnd }.sumOf { it.valor },
            paidThisMonth = payments.filter { it.paidAt >= monthStart }.sumOf { it.valor },
            byMethod = periodPayments.groupBy { it.forma }.mapValues { entry -> entry.value.sumOf { it.valor } },
            pendingItems = pendingItems.sortedByDescending { it.balance },
        )
    }

    private fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value, formatter) }.getOrNull()

    companion object {
        fun factory(
            workOrderRepository: WorkOrderRepository,
            paymentRepository: WorkOrderPaymentRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FinanceViewModel(workOrderRepository, paymentRepository) as T
            }
    }
}
