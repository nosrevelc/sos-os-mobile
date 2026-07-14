package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.SaleDao
import br.com.sos.osmobile.data.local.entity.SaleEntity
import br.com.sos.osmobile.data.local.entity.SaleItemEntity
import br.com.sos.osmobile.data.local.model.SaleSummary
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class SaleItemInput(
    val serviceProductId: Long,
    val quantity: Double,
    val unitPrice: Double,
)

class SaleRepository(
    private val saleDao: SaleDao,
    private val auditRepository: AuditRepository,
) {
    fun observeSummaries(): Flow<List<SaleSummary>> = saleDao.observeSummaries()

    suspend fun create(
        customerId: Long,
        paymentMethod: String,
        paidValue: Double,
        items: List<SaleItemInput>,
    ): Long {
        val now = Clock.nowMillis()
        val saleItems = items.map {
            SaleItemEntity(
                saleId = 0,
                serviceProductId = it.serviceProductId,
                quantidade = it.quantity,
                unitPrice = it.unitPrice,
                subtotal = it.quantity * it.unitPrice,
            )
        }
        val id = saleDao.insertWithItems(
            SaleEntity(
                numero = nextNumber(now),
                customerId = customerId,
                totalValue = saleItems.sumOf { it.subtotal },
                paidValue = paidValue,
                paymentMethod = paymentMethod.ifBlank { "Nao informado" },
                createdAt = now,
                updatedAt = now,
            ),
            saleItems,
        )
        auditRepository.record("Vendas", "Venda criada", "vendas", id)
        return id
    }

    private suspend fun nextNumber(nowMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val start = date.toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return "VD-${date.format(DateTimeFormatter.ofPattern("yyMMdd"))}${(saleDao.countCreatedBetween(start, end) + 1).toString().padStart(4, '0')}"
    }
}
