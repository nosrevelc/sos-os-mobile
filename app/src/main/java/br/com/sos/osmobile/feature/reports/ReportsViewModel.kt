package br.com.sos.osmobile.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import br.com.sos.osmobile.data.local.model.ServiceProductStockSummary
import br.com.sos.osmobile.data.local.model.WorkOrderServiceUsage
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.repository.SaleRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.StockRepository
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ReportLine(val label: String, val value: String)

data class ReportsUiState(
    val lowStock: List<ReportLine> = emptyList(),
    val topServices: List<ReportLine> = emptyList(),
    val topCustomers: List<ReportLine> = emptyList(),
    val financial: List<ReportLine> = emptyList(),
)

private data class ReportsBase(
    val services: List<ServiceProductEntity>,
    val stock: List<ServiceProductStockSummary>,
    val usage: List<WorkOrderServiceUsage>,
    val orders: List<WorkOrderSummary>,
    val payments: List<WorkOrderPaymentEntity>,
)

class ReportsViewModel(
    serviceProductRepository: ServiceProductRepository,
    stockRepository: StockRepository,
    workOrderRepository: WorkOrderRepository,
    paymentRepository: WorkOrderPaymentRepository,
    saleRepository: SaleRepository,
) : ViewModel() {
    private val base = combine(
        serviceProductRepository.observeActive(),
        stockRepository.observeSummaries(),
        workOrderRepository.observeServiceUsage(),
        workOrderRepository.observeSummaries(),
        paymentRepository.observeAll(),
    ) { services, stock, usage, orders, payments ->
        ReportsBase(services, stock, usage, orders, payments)
    }

    val uiState: StateFlow<ReportsUiState> = combine(base, saleRepository.observeSummaries()) { base, sales ->
        val stockById = base.stock.associate { it.id to it.saldo }
        ReportsUiState(
            lowStock = base.services
                .filter { it.tipo != ServiceProductType.SERVICE && it.minimumStock > 0.0 && (stockById[it.id] ?: 0.0) <= it.minimumStock }
                .map { ReportLine(it.nome, "${stockById[it.id] ?: 0.0} / min ${it.minimumStock}") },
            topServices = base.usage
                .groupBy { it.serviceName }
                .map { (name, lines) -> name to lines.sumOf { it.quantity } }
                .sortedByDescending { it.second }
                .take(10)
                .map { ReportLine(it.first, formatQty(it.second)) },
            topCustomers = base.orders
                .groupBy { it.customerName }
                .map { (name, lines) -> name to lines.sumOf { it.totalValue } }
                .sortedByDescending { it.second }
                .take(10)
                .map { ReportLine(it.first, money(it.second)) },
            financial = listOf(
                ReportLine("OS abertas", base.orders.size.toString()),
                ReportLine("Valor em OS", money(base.orders.sumOf { it.totalValue })),
                ReportLine("Recebido em OS", money(base.payments.sumOf { it.valor })),
                ReportLine("Vendas", money(sales.sumOf { it.totalValue })),
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportsUiState())

    companion object {
        fun factory(
            serviceProductRepository: ServiceProductRepository,
            stockRepository: StockRepository,
            workOrderRepository: WorkOrderRepository,
            paymentRepository: WorkOrderPaymentRepository,
            saleRepository: SaleRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ReportsViewModel(serviceProductRepository, stockRepository, workOrderRepository, paymentRepository, saleRepository) as T
            }
    }
}

private fun money(value: Double): String = "R$ %.2f".format(value)
private fun formatQty(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(value)
