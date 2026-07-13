package br.com.sos.osmobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.data.repository.ContactAccount
import br.com.sos.osmobile.data.repository.ContactsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.COMPANY_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CONTACTS_GOOGLE_ACCOUNT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CPF_CNPJ_POLICY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PIX_KEY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PIX_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_BLUETOOTH_ADDRESS_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_AUTO_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_COPIES_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_FONT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_FOOTER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_ALIGN_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_BOLD_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_TEXT_SIZE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PICKUP_REMINDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_QUOTE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_REVIEW_REQUEST_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_CANCELED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_COMPLETED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_OPEN_KEY
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
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
    val companyName: String = "",
    val pixName: String = "",
    val pixKey: String = "",
    val printBluetoothAddress: String = "",
    val printWorkOrderAuto: Boolean = false,
    val printWorkOrderCopies: String = "0",
    val printWorkOrderHeader: String = "{empresa}\nOS {os}\n{data}",
    val printWorkOrderFooter: String = "Obrigado pela preferencia",
    val printWorkOrderFont: String = "A",
    val printWorkOrderTextSize: String = "normal",
    val printWorkOrderHeaderBold: Boolean = true,
    val printWorkOrderHeaderAlign: String = "center",
    val workOrderTemplate: String = MessageTemplateRenderer.workOrderDefaultTemplate,
    val workOrderOpenTemplate: String = MessageTemplateRenderer.workOrderOpenTemplate,
    val workOrderInProgressTemplate: String = MessageTemplateRenderer.workOrderInProgressTemplate,
    val workOrderCompletedTemplate: String = MessageTemplateRenderer.workOrderCompletedTemplate,
    val workOrderCanceledTemplate: String = MessageTemplateRenderer.workOrderCanceledTemplate,
    val reviewRequestTemplate: String = MessageTemplateRenderer.reviewRequestTemplate,
    val pickupReminderTemplate: String = MessageTemplateRenderer.pickupReminderTemplate,
    val quoteTemplate: String = MessageTemplateRenderer.quoteDefaultTemplate,
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
                companyName = rawValues[COMPANY_NAME_KEY].orEmpty(),
                pixName = rawValues[PIX_NAME_KEY].orEmpty(),
                pixKey = rawValues[PIX_KEY_KEY].orEmpty(),
                printBluetoothAddress = rawValues[PRINT_BLUETOOTH_ADDRESS_KEY].orEmpty(),
                printWorkOrderAuto = values[PRINT_WORK_ORDER_AUTO_KEY] ?: false,
                printWorkOrderCopies = rawValues[PRINT_WORK_ORDER_COPIES_KEY]?.takeIf { it.isNotBlank() } ?: "0",
                printWorkOrderHeader = rawValues[PRINT_WORK_ORDER_HEADER_KEY] ?: "{empresa}\nOS {os}\n{data}",
                printWorkOrderFooter = rawValues[PRINT_WORK_ORDER_FOOTER_KEY] ?: "Obrigado pela preferencia",
                printWorkOrderFont = rawValues[PRINT_WORK_ORDER_FONT_KEY] ?: "A",
                printWorkOrderTextSize = rawValues[PRINT_WORK_ORDER_TEXT_SIZE_KEY] ?: "normal",
                printWorkOrderHeaderBold = values[PRINT_WORK_ORDER_HEADER_BOLD_KEY] ?: true,
                printWorkOrderHeaderAlign = rawValues[PRINT_WORK_ORDER_HEADER_ALIGN_KEY] ?: "center",
                workOrderTemplate = rawValues[TEMPLATE_WORK_ORDER_KEY] ?: MessageTemplateRenderer.workOrderDefaultTemplate,
                workOrderOpenTemplate = rawValues[TEMPLATE_WORK_ORDER_OPEN_KEY] ?: MessageTemplateRenderer.workOrderOpenTemplate,
                workOrderInProgressTemplate = rawValues[TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY] ?: MessageTemplateRenderer.workOrderInProgressTemplate,
                workOrderCompletedTemplate = rawValues[TEMPLATE_WORK_ORDER_COMPLETED_KEY] ?: MessageTemplateRenderer.workOrderCompletedTemplate,
                workOrderCanceledTemplate = rawValues[TEMPLATE_WORK_ORDER_CANCELED_KEY] ?: MessageTemplateRenderer.workOrderCanceledTemplate,
                reviewRequestTemplate = rawValues[TEMPLATE_REVIEW_REQUEST_KEY] ?: MessageTemplateRenderer.reviewRequestTemplate,
                pickupReminderTemplate = rawValues[TEMPLATE_PICKUP_REMINDER_KEY] ?: MessageTemplateRenderer.pickupReminderTemplate,
                quoteTemplate = rawValues[TEMPLATE_QUOTE_KEY] ?: MessageTemplateRenderer.quoteDefaultTemplate,
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

    fun setCompanyName(value: String) {
        viewModelScope.launch {
            settingsRepository.set(COMPANY_NAME_KEY, value.trim())
        }
    }

    fun setPixData(name: String, key: String) {
        viewModelScope.launch {
            settingsRepository.set(PIX_NAME_KEY, name.trim())
            settingsRepository.set(PIX_KEY_KEY, key.trim())
        }
    }

    fun setPrintSettings(
        bluetoothAddress: String,
        autoWorkOrder: Boolean,
        workOrderCopies: String,
        workOrderHeader: String,
        workOrderFooter: String,
        workOrderFont: String,
        workOrderTextSize: String,
        workOrderHeaderBold: Boolean,
        workOrderHeaderAlign: String,
    ) {
        viewModelScope.launch {
            settingsRepository.set(PRINT_BLUETOOTH_ADDRESS_KEY, bluetoothAddress.trim())
            settingsRepository.set(PRINT_WORK_ORDER_AUTO_KEY, autoWorkOrder.toString())
            settingsRepository.set(PRINT_WORK_ORDER_COPIES_KEY, (workOrderCopies.toIntOrNull() ?: 0).coerceIn(0, 9).toString())
            settingsRepository.set(PRINT_WORK_ORDER_HEADER_KEY, workOrderHeader.trim())
            settingsRepository.set(PRINT_WORK_ORDER_FOOTER_KEY, workOrderFooter.trim())
            settingsRepository.set(PRINT_WORK_ORDER_FONT_KEY, workOrderFont)
            settingsRepository.set(PRINT_WORK_ORDER_TEXT_SIZE_KEY, workOrderTextSize)
            settingsRepository.set(PRINT_WORK_ORDER_HEADER_BOLD_KEY, workOrderHeaderBold.toString())
            settingsRepository.set(PRINT_WORK_ORDER_HEADER_ALIGN_KEY, workOrderHeaderAlign)
        }
    }

    fun setWorkOrderTemplate(value: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_WORK_ORDER_KEY, value.trim())
        }
    }

    fun setQuoteTemplate(value: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_QUOTE_KEY, value.trim())
        }
    }

    fun setReviewRequestTemplate(value: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_REVIEW_REQUEST_KEY, value.trim())
        }
    }

    fun setPickupReminderTemplate(value: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_PICKUP_REMINDER_KEY, value.trim())
        }
    }

    fun setWorkOrderStatusTemplates(open: String, inProgress: String, completed: String, canceled: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_WORK_ORDER_OPEN_KEY, open.trim())
            settingsRepository.set(TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY, inProgress.trim())
            settingsRepository.set(TEMPLATE_WORK_ORDER_COMPLETED_KEY, completed.trim())
            settingsRepository.set(TEMPLATE_WORK_ORDER_CANCELED_KEY, canceled.trim())
        }
    }

    fun resetTemplates() {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_WORK_ORDER_KEY, MessageTemplateRenderer.workOrderDefaultTemplate)
            settingsRepository.set(TEMPLATE_WORK_ORDER_OPEN_KEY, MessageTemplateRenderer.workOrderOpenTemplate)
            settingsRepository.set(TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY, MessageTemplateRenderer.workOrderInProgressTemplate)
            settingsRepository.set(TEMPLATE_WORK_ORDER_COMPLETED_KEY, MessageTemplateRenderer.workOrderCompletedTemplate)
            settingsRepository.set(TEMPLATE_WORK_ORDER_CANCELED_KEY, MessageTemplateRenderer.workOrderCanceledTemplate)
            settingsRepository.set(TEMPLATE_REVIEW_REQUEST_KEY, MessageTemplateRenderer.reviewRequestTemplate)
            settingsRepository.set(TEMPLATE_PICKUP_REMINDER_KEY, MessageTemplateRenderer.pickupReminderTemplate)
            settingsRepository.set(TEMPLATE_QUOTE_KEY, MessageTemplateRenderer.quoteDefaultTemplate)
        }
    }

    fun loadContactAccounts() {
        viewModelScope.launch {
            runCatching {
                contactsRepository.listContactAccounts()
            }.fold(
                onSuccess = {
                    contactAccounts.value = it
                    contactsMessage.value = if (it.isEmpty()) {
                        "Nenhuma conta de contatos encontrada. Use agenda local ou verifique a sincronizacao do aparelho."
                    } else {
                        "${it.size} agenda(s) encontrada(s)."
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
