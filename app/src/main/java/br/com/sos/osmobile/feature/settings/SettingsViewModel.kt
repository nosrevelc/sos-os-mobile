package br.com.sos.osmobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CPF_CNPJ_POLICY_KEY
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val orcamento: Boolean = true,
    val fotos: Boolean = false,
    val assinatura: Boolean = false,
    val checklist: Boolean = false,
    val garantia: Boolean = false,
    val financeiro: Boolean = false,
    val cpfCnpjPolicy: CpfCnpjPolicy = CpfCnpjPolicy.Optional,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val settings = settingsRepository.observeAll()
        .map { entities ->
            val values = entities.associate { it.chave to it.valor.toBooleanStrictOrNull() }
            SettingsUiState(
                orcamento = values["modulo_orcamento"] ?: true,
                fotos = values["modulo_fotos"] ?: false,
                assinatura = values["modulo_assinatura"] ?: false,
                checklist = values["modulo_checklist"] ?: false,
                garantia = values["modulo_garantia"] ?: false,
                financeiro = values["modulo_financeiro"] ?: false,
                cpfCnpjPolicy = CpfCnpjPolicy.fromStorage(
                    entities.firstOrNull { it.chave == CPF_CNPJ_POLICY_KEY }?.valor,
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setModule(key: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.set(key, enabled.toString())
        }
    }

    fun setCpfCnpjPolicy(policy: CpfCnpjPolicy) {
        viewModelScope.launch {
            settingsRepository.set(CPF_CNPJ_POLICY_KEY, policy.storageValue)
        }
    }

    companion object {
        fun factory(repository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository) as T
                }
            }
    }
}
