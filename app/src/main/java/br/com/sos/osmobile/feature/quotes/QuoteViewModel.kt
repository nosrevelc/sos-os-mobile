package br.com.sos.osmobile.feature.quotes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.message.PixPayloadGenerator
import br.com.sos.osmobile.data.model.QuoteStatus
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.QuoteConversionRepository
import br.com.sos.osmobile.data.repository.QuoteConversionResult
import br.com.sos.osmobile.data.repository.QuoteItemInput
import br.com.sos.osmobile.data.repository.QuoteRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.COMPANY_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PIX_KEY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PIX_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_QUOTE_KEY
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class QuoteDraftItem(
    val serviceProductId: Long,
    val name: String,
    val quantity: Double,
    val unitPrice: Double,
) {
    val subtotal: Double = quantity * unitPrice
}

data class QuoteFormState(
    val editingId: Long? = null,
    val editingNumber: String? = null,
    val selectedCustomerId: Long? = null,
    val selectedServiceProductId: Long? = null,
    val status: QuoteStatus = QuoteStatus.Pending,
    val quantity: String = "1",
    val unitPrice: String = "",
    val notes: String = "",
    val items: List<QuoteDraftItem> = emptyList(),
    val message: String? = null,
)

data class QuoteUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val services: List<ServiceProductEntity> = emptyList(),
    val quotes: List<QuoteSummary> = emptyList(),
    val companyName: String = "",
    val pixName: String = "",
    val pixKey: String = "",
    val quoteTemplate: String = MessageTemplateRenderer.quoteDefaultTemplate,
)

class QuoteViewModel(
    private val quoteRepository: QuoteRepository,
    private val quoteConversionRepository: QuoteConversionRepository,
    private val auditRepository: AuditRepository,
    private val customerRepository: CustomerRepository,
    private val serviceProductRepository: ServiceProductRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<QuoteUiState> = combine(
        customerRepository.observeActive(),
        serviceProductRepository.observeActive(),
        quoteRepository.observeSummaries(),
        settingsRepository.observeAll(),
    ) { customers, services, quotes, settings ->
        val values = settings.associate { it.chave to it.valor }
        QuoteUiState(
            customers = customers,
            services = services,
            quotes = quotes,
            companyName = values[COMPANY_NAME_KEY].orEmpty(),
            pixName = values[PIX_NAME_KEY].orEmpty(),
            pixKey = values[PIX_KEY_KEY].orEmpty(),
            quoteTemplate = values[TEMPLATE_QUOTE_KEY] ?: MessageTemplateRenderer.quoteDefaultTemplate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuoteUiState())

    var formState by mutableStateOf(QuoteFormState())
        private set

    var listMessage by mutableStateOf<String?>(null)
        private set

    var documentText by mutableStateOf<String?>(null)
        private set

    var messageText by mutableStateOf<String?>(null)
        private set

    var messagePhone by mutableStateOf("")
        private set

    var historyText by mutableStateOf<String?>(null)
        private set

    fun selectCustomer(id: Long) {
        formState = formState.copy(selectedCustomerId = id, message = null)
    }

    fun selectServiceProduct(item: ServiceProductEntity) {
        formState = formState.copy(
            selectedServiceProductId = item.id,
            unitPrice = InputMasks.currencyFromDouble(item.unitPrice),
            message = null,
        )
    }

    fun selectStatus(status: QuoteStatus) {
        formState = formState.copy(status = status, message = null)
    }

    fun onQuantityChanged(value: String) {
        formState = formState.copy(quantity = InputMasks.decimal(value, integerDigits = 5, decimalDigits = 2), message = null)
    }

    fun onUnitPriceChanged(value: String) {
        formState = formState.copy(unitPrice = InputMasks.currency(value), message = null)
    }

    fun onNotesChanged(value: String) {
        formState = formState.copy(notes = value, message = null)
    }

    fun addSelectedItem() {
        val service = uiState.value.services.firstOrNull { it.id == formState.selectedServiceProductId }
        if (service == null) {
            formState = formState.copy(message = "Selecione um servico/produto.")
            return
        }
        val validationMessage = QuoteFormValidator.validateItem(formState.quantity, formState.unitPrice)
        if (validationMessage != null) {
            formState = formState.copy(message = validationMessage)
            return
        }
        val quantity = QuoteFormValidator.parseDecimal(formState.quantity) ?: return
        val unitPrice = QuoteFormValidator.parseDecimal(formState.unitPrice) ?: return
        formState = formState.copy(
            items = formState.items + QuoteDraftItem(
                serviceProductId = service.id,
                name = service.nome,
                quantity = quantity,
                unitPrice = unitPrice,
            ),
            selectedServiceProductId = null,
            quantity = "1",
            unitPrice = "",
            message = null,
        )
    }

    fun removeItem(index: Int) {
        formState = formState.copy(
            items = formState.items.filterIndexed { itemIndex, _ -> itemIndex != index },
            message = null,
        )
    }

    fun saveQuote() {
        val validationMessage = QuoteFormValidator.validate(formState)
        if (validationMessage != null) {
            formState = formState.copy(message = validationMessage)
            return
        }

        viewModelScope.launch {
            val items = formState.items.map {
                QuoteItemInput(
                    serviceProductId = it.serviceProductId,
                    quantity = it.quantity,
                    practicedUnitPrice = it.unitPrice,
                )
            }
            val editingId = formState.editingId
            if (editingId == null) {
                quoteRepository.create(
                    customerId = formState.selectedCustomerId ?: return@launch,
                    status = formState.status.label,
                    notes = formState.notes,
                    items = items,
                )
                formState = QuoteFormState(message = "Orcamento criado.")
            } else {
                val updated = quoteRepository.updateContent(
                    id = editingId,
                    customerId = formState.selectedCustomerId ?: return@launch,
                    status = formState.status,
                    notes = formState.notes,
                    items = items,
                )
                formState = QuoteFormState(
                    message = if (updated) "Orcamento atualizado." else "Orcamento convertido nao pode ser editado.",
                )
            }
        }
    }

    fun editQuote(quoteId: Long) {
        viewModelScope.launch {
            val quote = quoteRepository.findById(quoteId) ?: return@launch
            if (quote.status == QuoteStatus.Converted.label) {
                listMessage = "Orcamento convertido nao pode ser editado."
                return@launch
            }
            val items = quoteRepository.listItems(quoteId)
            val services = uiState.value.services
            formState = QuoteFormState(
                editingId = quote.id,
                editingNumber = quote.numero,
                selectedCustomerId = quote.customerId,
                status = statusFromLabel(quote.status),
                notes = quote.observacoes.orEmpty(),
                items = items.map { item ->
                    val service = services.firstOrNull { it.id == item.serviceProductId }
                    QuoteDraftItem(
                        serviceProductId = item.serviceProductId,
                        name = service?.nome ?: "Servico/produto ${item.serviceProductId}",
                        quantity = item.quantidade,
                        unitPrice = item.practicedUnitPrice,
                    )
                },
                message = "Editando orcamento ${quote.numero}.",
            )
        }
    }

    fun cancelEdit() {
        formState = QuoteFormState(message = "Edicao cancelada.")
    }

    fun convertToWorkOrder(quoteId: Long) {
        viewModelScope.launch {
            listMessage = when (quoteConversionRepository.convertApprovedQuoteToWorkOrder(quoteId)) {
                is QuoteConversionResult.Converted -> "Orcamento convertido em OS."
                QuoteConversionResult.QuoteNotApproved -> "Apenas orcamentos aprovados podem ser convertidos."
                QuoteConversionResult.QuoteNotFound -> "Orcamento nao encontrado."
                QuoteConversionResult.QuoteWithoutItems -> "Orcamento sem itens nao pode ser convertido."
            }
        }
    }

    fun updateQuoteStatus(quoteId: Long, status: QuoteStatus) {
        viewModelScope.launch {
            quoteRepository.updateStatus(quoteId, status)
            listMessage = "Status do orcamento alterado para ${status.label}."
        }
    }

    fun showDocument(quoteId: Long) {
        viewModelScope.launch {
            documentText = quoteRepository.generateDocumentText(quoteId) ?: "Documento nao encontrado."
        }
    }

    fun showMessage(quote: QuoteSummary) {
        messagePhone = quote.customerPhone
        messageText = MessageTemplateRenderer.render(
            template = uiState.value.quoteTemplate,
            tokens = mapOf(
                "nome" to quote.customerName,
                "telefone" to quote.customerPhone,
                "cpf" to "",
                "os" to "",
                "orcamento" to quote.number,
                "status" to quote.status,
                "valor" to quote.totalValue.toString(),
                "empresa" to uiState.value.companyName,
                "data" to "",
                "PIX" to PixPayloadGenerator.generate(uiState.value.pixKey, uiState.value.pixName, quote.totalValue),
                "PIX_QR" to "",
            ),
        )
    }

    fun showHistory(quoteId: Long) {
        viewModelScope.launch {
            val logs = auditRepository.listForRecord("orcamentos", quoteId)
            historyText = if (logs.isEmpty()) {
                "Sem historico para este orcamento."
            } else {
                logs.joinToString("\n") { "${it.acao}: ${it.detalhes.orEmpty()}" }
            }
        }
    }

    companion object {
        private fun statusFromLabel(label: String): QuoteStatus =
            QuoteStatus.entries.firstOrNull { it.label == label } ?: QuoteStatus.Pending

        fun factory(
            quoteRepository: QuoteRepository,
            quoteConversionRepository: QuoteConversionRepository,
            auditRepository: AuditRepository,
            customerRepository: CustomerRepository,
            serviceProductRepository: ServiceProductRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QuoteViewModel(
                        quoteRepository = quoteRepository,
                        quoteConversionRepository = quoteConversionRepository,
                        auditRepository = auditRepository,
                        customerRepository = customerRepository,
                        serviceProductRepository = serviceProductRepository,
                        settingsRepository = settingsRepository,
                    ) as T
                }
            }
    }
}
