package br.com.sos.osmobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.data.repository.ContactAccount
import br.com.sos.osmobile.data.repository.ContactsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CONTACTS_GOOGLE_ACCOUNT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CPF_CNPJ_POLICY_KEY
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
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
    val contactsGoogleAccount: String = "",
    val contactAccounts: List<ContactAccount> = emptyList(),
    val contactsMessage: String? = null,
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val contactsRepository: ContactsRepository,
) : ViewModel() {
    private val contactAccounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    private val contactsMessage = MutableStateFlow<String?>(null)

    val settings = combine(
        settingsRepository.observeAll(),
        contactAccounts,
        contactsMessage,
    ) { entities, accounts, message ->
        Triple(entities, accounts, message)
    }
        .map { entities ->
            val settingsEntities = entities.first
            val values = settingsEntities.associate { it.chave to it.valor.toBooleanStrictOrNull() }
            val rawValues = settingsEntities.associate { it.chave to it.valor }
            SettingsUiState(
                orcamento = values["modulo_orcamento"] ?: true,
                fotos = values["modulo_fotos"] ?: false,
                assinatura = values["modulo_assinatura"] ?: false,
                checklist = values["modulo_checklist"] ?: false,
                garantia = values["modulo_garantia"] ?: false,
                financeiro = values["modulo_financeiro"] ?: false,
                cpfCnpjPolicy = CpfCnpjPolicy.fromStorage(
                    settingsEntities.firstOrNull { it.chave == CPF_CNPJ_POLICY_KEY }?.valor,
                ),
                contactsGoogleAccount = rawValues[CONTACTS_GOOGLE_ACCOUNT_KEY].orEmpty(),
                contactAccounts = entities.second,
                contactsMessage = entities.third,
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

    fun setContactsGoogleAccount(value: String) {
        viewModelScope.launch {
            settingsRepository.set(CONTACTS_GOOGLE_ACCOUNT_KEY, value.trim())
        }
    }

    fun loadContactAccounts() {
        viewModelScope.launch {
            runCatching {
                contactsRepository.listGoogleContactAccounts()
            }.fold(
                onSuccess = {
                    contactAccounts.value = it
                    contactsMessage.value = if (it.isEmpty()) {
                        "Nenhuma conta Google de contatos encontrada. Use agenda local ou verifique a sincronizacao do aparelho."
                    } else {
                        "${it.size} agenda(s) Google encontrada(s)."
                    }
                },
                onFailure = {
                    contactsMessage.value = "Nao foi possivel ler as agendas: ${it.message ?: "verifique a permissao"}"
                },
            )
        }
    }

    companion object {
        fun factory(
            repository: SettingsRepository,
            contactsRepository: ContactsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository, contactsRepository) as T
                }
            }
    }
}
