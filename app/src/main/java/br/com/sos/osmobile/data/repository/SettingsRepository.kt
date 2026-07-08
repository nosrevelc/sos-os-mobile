package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.SettingsDao
import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingsDao: SettingsDao,
    private val auditRepository: AuditRepository,
) {
    fun observeAll(): Flow<List<AppSettingEntity>> = settingsDao.observeAll()

    fun observeCpfCnpjPolicy(): Flow<CpfCnpjPolicy> =
        settingsDao.observeAll()
            .map { settings ->
                CpfCnpjPolicy.fromStorage(settings.firstOrNull { it.chave == CPF_CNPJ_POLICY_KEY }?.valor)
            }

    suspend fun set(key: String, value: String) {
        settingsDao.upsert(AppSettingEntity(key, value, Clock.nowMillis()))
        auditRepository.record("Configuracoes", "Configuracao alterada", "configuracoes", details = "$key=$value")
    }

    companion object {
        const val CPF_CNPJ_POLICY_KEY = "cpf_cnpj_policy"
    }
}
