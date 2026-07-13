package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.WorkOrderChecklistDao
import br.com.sos.osmobile.data.local.entity.WorkOrderChecklistItemEntity

class WorkOrderChecklistRepository(
    private val checklistDao: WorkOrderChecklistDao,
    private val auditRepository: AuditRepository,
) {
    suspend fun listByWorkOrder(workOrderId: Long): List<WorkOrderChecklistItemEntity> =
        checklistDao.listByWorkOrder(workOrderId)

    suspend fun addItem(workOrderId: Long, description: String): Long {
        val now = Clock.nowMillis()
        val id = checklistDao.insert(
            WorkOrderChecklistItemEntity(
                workOrderId = workOrderId,
                descricao = description.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        auditRepository.record("Checklist", "Item adicionado", "ordens_servico", workOrderId, details = description)
        return id
    }

    suspend fun setChecked(id: Long, checked: Boolean) {
        val current = checklistDao.findById(id) ?: return
        checklistDao.update(current.copy(concluido = checked, updatedAt = Clock.nowMillis()))
        auditRepository.record("Checklist", "Item alterado", "ordens_servico", current.workOrderId, details = "${current.descricao}: $checked")
    }

    suspend fun deleteItem(id: Long) {
        val current = checklistDao.findById(id) ?: return
        checklistDao.deleteById(id)
        auditRepository.record("Checklist", "Item removido", "ordens_servico", current.workOrderId, details = current.descricao)
    }
}
