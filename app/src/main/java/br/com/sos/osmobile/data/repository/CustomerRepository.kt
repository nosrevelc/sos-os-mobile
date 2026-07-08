package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.CustomerDao
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

class CustomerRepository(
    private val customerDao: CustomerDao,
    private val auditRepository: AuditRepository,
) {
    fun observeActive(): Flow<List<CustomerEntity>> = customerDao.observeActive()

    fun search(query: String): Flow<List<CustomerEntity>> = customerDao.search(query, includeInactive = false)

    suspend fun create(
        name: String,
        phone: String,
        cpfCnpj: String?,
        email: String?,
        address: String?,
        notes: String?,
    ): Long {
        val now = Clock.nowMillis()
        val id = customerDao.insert(
            CustomerEntity(
                nome = name.trim(),
                telefone = phone.trim(),
                cpfCnpj = cpfCnpj?.trim()?.takeIf { it.isNotBlank() },
                email = email?.trim()?.takeIf { it.isNotBlank() },
                endereco = address?.trim()?.takeIf { it.isNotBlank() },
                observacoes = notes?.trim()?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now,
            ),
        )
        auditRepository.record("Clientes", "Cliente criado", "clientes", id)
        return id
    }

    suspend fun update(customer: CustomerEntity) {
        customerDao.update(customer.copy(updatedAt = Clock.nowMillis()))
        auditRepository.record("Clientes", "Cliente atualizado", "clientes", customer.id)
    }

    suspend fun archive(id: Long) {
        customerDao.archive(id, Clock.nowMillis())
        auditRepository.record("Clientes", "Cliente excluido logicamente", "clientes", id)
    }
}
