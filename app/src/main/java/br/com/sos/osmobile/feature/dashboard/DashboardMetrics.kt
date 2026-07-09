package br.com.sos.osmobile.feature.dashboard

import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.local.model.WorkOrderSummary

data class DashboardMetrics(
    val workOrderCount: Int,
    val workOrderRevenue: Double,
    val quoteCount: Int,
    val quoteTotal: Double,
    val openWorkOrders: Int,
    val pendingQuotes: Int,
)

object DashboardMetricsCalculator {
    fun calculate(
        workOrders: List<WorkOrderSummary>,
        quotes: List<QuoteSummary>,
    ): DashboardMetrics =
        DashboardMetrics(
            workOrderCount = workOrders.size,
            workOrderRevenue = workOrders
                .filter { it.status != "Cancelada" }
                .sumOf { it.totalValue },
            quoteCount = quotes.size,
            quoteTotal = quotes
                .filter { it.status != "Rejeitado" }
                .sumOf { it.totalValue },
            openWorkOrders = workOrders.count { it.status == "Aberta" || it.status == "Em andamento" },
            pendingQuotes = quotes.count { it.status == "Pendente" },
        )
}
