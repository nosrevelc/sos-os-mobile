package br.com.sos.osmobile.feature.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class QuickMessageTemplate(
    val label: String,
    val settingsKey: String,
    val defaultText: String,
)

class QuickMessagesViewModel(
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    var query by mutableStateOf("")
        private set

    var customers by mutableStateOf<List<CustomerEntity>>(emptyList())
        private set

    var selectedCustomer by mutableStateOf<CustomerEntity?>(null)
        private set

    var selectedTemplate by mutableStateOf(templates.first())
        private set

    var customText by mutableStateOf("")
        private set

    private var settings = emptyMap<String, String>()

    val renderedText: String
        get() {
            val customer = selectedCustomer
            val base = customText.ifBlank {
                settings[selectedTemplate.settingsKey] ?: selectedTemplate.defaultText
            }
            return MessageTemplateRenderer.render(
                base,
                mapOf(
                    "nome" to (customer?.nome ?: ""),
                    "telefone" to (customer?.telefone ?: ""),
                    "email" to (customer?.email ?: ""),
                    "empresa" to (settings[SettingsRepository.COMPANY_NAME_KEY] ?: "OS Mobile"),
                ),
            )
        }

    init {
        viewModelScope.launch {
            customerRepository.observeActive().collectLatest { customers = it }
        }
        viewModelScope.launch {
            settingsRepository.observeAll().collectLatest { list ->
                settings = list.associate { it.chave to it.valor }
            }
        }
    }

    fun onQueryChanged(value: String) {
        query = value
    }

    fun selectCustomer(customer: CustomerEntity) {
        selectedCustomer = customer
        query = customer.nome
    }

    fun selectTemplate(template: QuickMessageTemplate) {
        selectedTemplate = template
        customText = settings[template.settingsKey] ?: template.defaultText
    }

    fun onCustomTextChanged(value: String) {
        customText = value
    }

    fun clearCustomer() {
        selectedCustomer = null
        query = ""
    }

    val filteredCustomers: List<CustomerEntity>
        get() {
            val term = query.trim()
            if (term.isBlank()) return customers.take(8)
            return customers.filter {
                it.nome.contains(term, ignoreCase = true) ||
                    it.telefone.contains(term) ||
                    it.email.orEmpty().contains(term, ignoreCase = true)
            }.take(12)
        }

    companion object {
        val templates = listOf(
            QuickMessageTemplate("Comunicado", SettingsRepository.TEMPLATE_ANNOUNCEMENT_KEY, MessageTemplateRenderer.announcementTemplate),
            QuickMessageTemplate("Boas-vindas", SettingsRepository.TEMPLATE_WELCOME_KEY, MessageTemplateRenderer.welcomeTemplate),
            QuickMessageTemplate("Agradecimento", SettingsRepository.TEMPLATE_THANK_YOU_KEY, MessageTemplateRenderer.thankYouTemplate),
            QuickMessageTemplate("Solicitar avaliacao", SettingsRepository.TEMPLATE_REVIEW_REQUEST_KEY, MessageTemplateRenderer.reviewRequestTemplate),
        )

        fun factory(
            customerRepository: CustomerRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    QuickMessagesViewModel(customerRepository, settingsRepository) as T
            }
    }
}
