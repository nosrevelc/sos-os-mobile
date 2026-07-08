package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.SettingsDao
import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow

class SettingsRepository(
    private val settingsDao: SettingsDao,
    private val auditRepository: AuditRepository,
) {
    fun observeAll(): Flow<List<AppSettingEntity>> = settingsDao.observeAll()

    suspend fun set(key: String, value: String) {
        settingsDao.upsert(AppSettingEntity(key, value, Clock.nowMillis()))
        auditRepository.record("Configuracoes", "Configuracao alterada", "configuracoes", details = "$key=$value")
    }
}
