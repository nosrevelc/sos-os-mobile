package br.com.sos.osmobile.feature.workorders

import br.com.sos.osmobile.data.local.entity.ServiceProductType

object WorkOrderStockTotals {

    fun of(
        items: List<WorkOrderDraftItem>,
        serviceTypes: Map<Long, String>,
    ): Map<Long, Double> =
        items.filter { (serviceTypes[it.serviceProductId] ?: it.type) != ServiceProductType.SERVICE }
            .groupBy { it.serviceProductId }
            .mapValues { entry -> entry.value.sumOf { it.quantity } }
}
