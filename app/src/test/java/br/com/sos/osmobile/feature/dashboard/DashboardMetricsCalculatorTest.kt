package br.com.sos.osmobile.feature.dashboard

import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.local.model.WorkOrderServiceUsage
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardMetricsCalculatorTest {
    @Test
    fun calculateIgnoresCanceledWorkOrdersAndRejectedQuotesInTotals() {
        val metrics = DashboardMetricsCalculator.calculate(
            workOrders = listOf(
                WorkOrderSummary(1, "1", "Cliente", "11999999999", "Aberta", 100.0, 1, 1),
                WorkOrderSummary(2, "2", "Cliente", "11999999999", "Cancelada", 50.0, 1, 1),
            ),
            quotes = listOf(
                QuoteSummary(1, "OR1", "Cliente", "11999999999", "Pendente", 80.0, 1, 1),
                QuoteSummary(2, "OR2", "Cliente", "11999999999", "Rejeitado", 20.0, 1, 1),
            ),
            serviceUsage = listOf(
                WorkOrderServiceUsage(1, "Ajuste", 2.0, 100.0, 1),
            ),
            startMillis = 0,
            endMillis = 10,
        )

        assertEquals(2, metrics.workOrderCount)
        assertEquals(100.0, metrics.workOrderRevenue, 0.0)
        assertEquals(80.0, metrics.quoteTotal, 0.0)
        assertEquals(1, metrics.pendingQuotes)
        assertEquals("Ajuste", metrics.topServices.first().label)
    }
}
