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

    fun search(query: String): Flow<List<ServiceProductEntity>> = serviceProductDao.search(query)

    suspend fun create(
        code: String,
        name: String,
        category: String?,
        description: String?,
        unitPrice: Double,
    ): Long {
        val now = Clock.nowMillis()
        val id = serviceProductDao.insert(
            ServiceProductEntity(
                codigo = code.trim(),
                nome = name.trim(),
                categoria = category?.trim()?.takeIf { it.isNotBlank() },
                descricao = description?.trim()?.takeIf { it.isNotBlank() },
                unitPrice = unitPrice,
                createdAt = now,
                updatedAt = now,
            ),
        )
        auditRepository.record("Servicos", "Servico/produto criado", "servicos_produtos", id)
        return id
    }

    suspend fun update(
        id: Long,
        code: String,
        name: String,
        category: String?,
        description: String?,
        unitPrice: Double,
    ) {
        val current = serviceProductDao.findById(id) ?: return
        serviceProductDao.update(
            current.copy(
                codigo = code.trim(),
                nome = name.trim(),
                categoria = category?.trim()?.takeIf { it.isNotBlank() },
                descricao = description?.trim()?.takeIf { it.isNotBlank() },
                unitPrice = unitPrice,
                updatedAt = Clock.nowMillis(),
            ),
        )
        auditRepository.record("Servicos", "Servico/produto atualizado", "servicos_produtos", id)
    }

    suspend fun archive(id: Long) {
        serviceProductDao.archive(id, Clock.nowMillis())
        auditRepository.record("Servicos", "Servico/produto excluido logicamente", "servicos_produtos", id)
    }
}
