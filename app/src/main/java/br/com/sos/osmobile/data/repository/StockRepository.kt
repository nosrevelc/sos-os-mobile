package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.StockMovementDao
import br.com.sos.osmobile.data.local.entity.StockMovementEntity
import br.com.sos.osmobile.data.local.entity.StockMovementType
import br.com.sos.osmobile.data.local.model.ServiceProductStockSummary
import kotlinx.coroutines.flow.Flow

class StockRepository(
    private val stockMovementDao: StockMovementDao,
    private val auditRepository: AuditRepository,
) {
    fun observeSummaries(): Flow<List<ServiceProductStockSummary>> =
        stockMovementDao.observeSummaries()

    fun observeHistory(serviceProductId: Long): Flow<List<StockMovementEntity>> =
        stockMovementDao.observeByServiceProduct(serviceProductId)

    suspend fun move(
        serviceProductId: Long,
        type: String,
        quantity: Double,
        reason: String?,
        workOrderId: Long? = null,
    ): Long {
        require(type in StockMovementType.all)
        require(quantity != 0.0)
        val storedQuantity = if (type == StockMovementType.ADJUST) quantity else kotlin.math.abs(quantity)
        val id = stockMovementDao.insert(
            StockMovementEntity(
                serviceProductId = serviceProductId,
                tipo = type,
                quantidade = storedQuantity,
                motivo = reason?.trim()?.takeIf { it.isNotBlank() },
                workOrderId = workOrderId,
                createdAt = Clock.nowMillis(),
            ),
        )
        auditRepository.record("Estoque", "Movimentacao de estoque: $type", "movimentacoes_estoque", id)
        return id
    }
}
