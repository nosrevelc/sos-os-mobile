package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.AuditLogDao
import br.com.sos.osmobile.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

class AuditRepository(
    private val auditLogDao: AuditLogDao,
) {
    fun observeRecent(): Flow<List<AuditLogEntity>> = auditLogDao.observeRecent()

    suspend fun listForRecord(table: String, recordId: Long): List<AuditLogEntity> =
        auditLogDao.listForRecord(table, recordId)

    suspend fun record(
        module: String,
        action: String,
        table: String? = null,
        recordId: Long? = null,
        details: String? = null,
        user: String? = null,
    ) {
        auditLogDao.insert(
            AuditLogEntity(
                timestamp = Clock.nowMillis(),
                usuario = user,
                modulo = module,
                acao = action,
                affectedTable = table,
                affectedRecordId = recordId,
                detalhes = details,
            ),
        )
    }
}
