package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.data.local.dao.QuoteDao
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val auditRepository: AuditRepository,
) {
    fun observeAll(): Flow<List<QuoteEntity>> = quoteDao.observeAll()

    suspend fun create(quote: QuoteEntity): Long {
        val id = quoteDao.insert(quote)
        auditRepository.record("Orcamentos", "Orcamento criado", "orcamentos", id)
        return id
    }
}
