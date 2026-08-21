package br.com.sos.osmobile.feature.workorders

import br.com.sos.osmobile.data.local.entity.ServiceProductType
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkOrderStockTotalsTest {

    private fun item(
        serviceProductId: Long,
        quantity: Double,
        type: String = ServiceProductType.PRODUCT,
    ) = WorkOrderDraftItem(
        serviceProductId = serviceProductId,
        name = "Item $serviceProductId",
        type = type,
        quantity = quantity,
        unitPrice = 10.0,
    )

    @Test
    fun sumsQuantitiesPerStockControlledItem() {
        val totals = WorkOrderStockTotals.of(
            items = listOf(item(1, 2.0), item(1, 3.0), item(2, 1.5)),
            serviceTypes = emptyMap(),
        )

        assertEquals(mapOf(1L to 5.0, 2L to 1.5), totals)
    }

    @Test
    fun ignoresServices() {
        val totals = WorkOrderStockTotals.of(
            items = listOf(
                item(1, 2.0, type = ServiceProductType.SERVICE),
                item(2, 4.0),
            ),
            serviceTypes = mapOf(1L to ServiceProductType.SERVICE),
        )

        assertEquals(mapOf(2L to 4.0), totals)
    }

    @Test
    fun serviceTypeLookupOverridesItemType() {
        val totals = WorkOrderStockTotals.of(
            items = listOf(item(1, 2.0, type = ServiceProductType.PRODUCT)),
            serviceTypes = mapOf(1L to ServiceProductType.SERVICE),
        )

        assertEquals(emptyMap<Long, Double>(), totals)
    }

    @Test
    fun fallsBackToItemTypeWhenServiceUnknown() {
        val totals = WorkOrderStockTotals.of(
            items = listOf(item(1, 2.0, type = ServiceProductType.SUPPLY)),
            serviceTypes = emptyMap(),
        )

        assertEquals(mapOf(1L to 2.0), totals)
    }

    @Test
    fun emptyItemsProduceEmptyTotals() {
        val totals = WorkOrderStockTotals.of(items = emptyList(), serviceTypes = emptyMap())

        assertEquals(emptyMap<Long, Double>(), totals)
    }
}
