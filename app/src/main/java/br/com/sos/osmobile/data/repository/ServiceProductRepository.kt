package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.ServiceProductDao
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import kotlinx.coroutines.flow.Flow

class ServiceProductRepository(
    private val serviceProductDao: ServiceProductDao,
    private val auditRepository: AuditRepository,
) {
    fun observeActive(): Flow<List<ServiceProductEntity>> = serviceProductDao.observeActive()

    suspend fun create(item: ServiceProductEntity): Long {
        val now = Clock.nowMillis()
        val id = serviceProductDao.insert(item.copy(createdAt = now, updatedAt = now))
        auditRepository.record("Servicos", "Servico/produto criado", "servicos_produtos", id)
        return id
    }

    suspend fun archive(id: Long) {
        serviceProductDao.archive(id, Clock.nowMillis())
        auditRepository.record("Servicos", "Servico/produto excluido logicamente", "servicos_produtos", id)
    }
}
