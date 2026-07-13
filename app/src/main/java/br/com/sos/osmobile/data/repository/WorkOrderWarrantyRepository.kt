package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.WorkOrderWarrantyDao
import br.com.sos.osmobile.data.local.entity.WorkOrderWarrantyEntity

class WorkOrderWarrantyRepository(
    private val warrantyDao: WorkOrderWarrantyDao,
    private val auditRepository: AuditRepository,
) {
    suspend fun findByWorkOrder(workOrderId: Long): WorkOrderWarrantyEntity? =
        warrantyDao.findByWorkOrder(workOrderId)

    suspend fun save(workOrderId: Long, warrantyDays: Int, terms: String): Long {
        val now = Clock.nowMillis()
        val current = warrantyDao.findByWorkOrder(workOrderId)
        val id = warrantyDao.upsert(
            WorkOrderWarrantyEntity(
                id = current?.id ?: 0,
                workOrderId = workOrderId,
                warrantyDays = warrantyDays.coerceAtLeast(0),
                termos = terms.trim().ifBlank { "Garantia conforme politica da empresa." },
                createdAt = current?.createdAt ?: now,
                updatedAt = now,
            ),
        )
        auditRepository.record("Garantia", "Garantia salva", "ordens_servico", workOrderId, details = "$warrantyDays dias")
        return id
    }

    suspend fun delete(workOrderId: Long) {
        warrantyDao.deleteByWorkOrder(workOrderId)
        auditRepository.record("Garantia", "Garantia removida", "ordens_servico", workOrderId)
    }
}
