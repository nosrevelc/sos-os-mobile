package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.data.local.dao.WorkOrderDao
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import kotlinx.coroutines.flow.Flow

class WorkOrderRepository(
    private val workOrderDao: WorkOrderDao,
    private val auditRepository: AuditRepository,
) {
    fun observeAll(): Flow<List<WorkOrderEntity>> = workOrderDao.observeAll()

    suspend fun create(workOrder: WorkOrderEntity): Long {
        val id = workOrderDao.insert(workOrder)
        auditRepository.record("Ordens de Servico", "OS criada", "ordens_servico", id)
        return id
    }
}
