package br.com.sos.osmobile.feature.dashboard

import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.local.model.WorkOrderServiceUsage
import br.com.sos.osmobile.data.local.model.WorkOrderSummary

data class DashboardRankItem(
    val label: String,
    val quantity: Double,
    val totalValue: Double,
)

data class DashboardMetrics(
    val workOrderCount: Int,
    val workOrderRevenue: Double,
    val quoteCount: Int,
    val quoteTotal: Double,
    val openWorkOrders: Int,
    val pendingQuotes: Int,
    val topServices: List<DashboardRankItem> = emptyList(),
    val topCustomersByValue: List<DashboardRankItem> = emptyList(),
    val topCustomersByQuantity: List<DashboardRankItem> = emptyList(),
)

object DashboardMetricsCalculator {
    fun calculate(
        workOrders: List<WorkOrderSummary>,
        quotes: List<QuoteSummary>,
        serviceUsage: List<WorkOrderServiceUsage>,
        startMillis: Long,
        endMillis: Long,
    ): DashboardMetrics {
        val filteredWorkOrders = workOrders.filter { it.openedAt in startMillis..endMillis }
        val filteredQuotes = quotes.filter { it.createdAt in startMillis..endMillis }
        val filteredServices = serviceUsage.filter { it.openedAt in startMillis..endMillis }

        return DashboardMetrics(
            workOrderCount = filteredWorkOrders.size,
            workOrderRevenue = filteredWorkOrders
                .filter { it.status != "Cancelada" }
                .sumOf { it.totalValue },
            quoteCount = filteredQuotes.size,
            quoteTotal = filteredQuotes
                .filter { it.status != "Rejeitado" }
                .sumOf { it.totalValue },
            openWorkOrders = filteredWorkOrders.count { it.status == "Aberta" || it.status == "Em andamento" },
            pendingQuotes = filteredQuotes.count { it.status == "Pendente" },
            topServices = filteredServices
                .groupBy { it.serviceName }
                .map { (name, items) ->
                    DashboardRankItem(
                        label = name,
                        quantity = items.sumOf { it.quantity },
                        totalValue = items.sumOf { it.totalValue },
                    )
                }
                .sortedByDescending { it.quantity }
                .take(10),
            topCustomersByValue = filteredWorkOrders
                .filter { it.status != "Cancelada" }
                .groupBy { "${it.customerName}|${it.customerPhone}" }
                .map { (_, items) ->
                    DashboardRankItem(
                        label = items.first().customerName,
                        quantity = items.sumOf { it.itemCount }.toDouble(),
                        totalValue = items.sumOf { it.totalValue },
                    )
                }
                .sortedByDescending { it.totalValue }
                .take(10),
            topCustomersByQuantity = filteredWorkOrders
                .filter { it.status != "Cancelada" }
                .groupBy { "${it.customerName}|${it.customerPhone}" }
                .map { (_, items) ->
                    DashboardRankItem(
                        label = items.first().customerName,
                        quantity = items.sumOf { it.itemCount }.toDouble(),
                        totalValue = items.sumOf { it.totalValue },
                    )
                }
                .sortedByDescending { it.quantity }
                .take(10),
        )
    }
}
