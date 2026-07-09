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
import br.com.sos.osmobile.data.model.QuoteStatus
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.QuoteConversionRepository
import br.com.sos.osmobile.data.repository.QuoteConversionResult
import br.com.sos.osmobile.data.repository.QuoteItemInput
import br.com.sos.osmobile.data.repository.QuoteRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
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
)

class QuoteViewModel(
    private val quoteRepository: QuoteRepository,
    private val quoteConversionRepository: QuoteConversionRepository,
    private val customerRepository: CustomerRepository,
    private val serviceProductRepository: ServiceProductRepository,
) : ViewModel() {
    val uiState: StateFlow<QuoteUiState> = combine(
        customerRepository.observeActive(),
        serviceProductRepository.observeActive(),
        quoteRepository.observeSummaries(),
    ) { customers, services, quotes ->
        QuoteUiState(customers = customers, services = services, quotes = quotes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuoteUiState())

    var formState by mutableStateOf(QuoteFormState())
        private set

    var listMessage by mutableStateOf<String?>(null)
        private set

    fun selectCustomer(id: Long) {
        formState = formState.copy(selectedCustomerId = id, message = null)
    }

    fun selectServiceProduct(item: ServiceProductEntity) {
        formState = formState.copy(
            selectedServiceProductId = item.id,
            unitPrice = item.unitPrice.toString().replace(".", ","),
            message = null,
        )
    }

    fun selectStatus(status: QuoteStatus) {
        formState = formState.copy(status = status, message = null)
    }

    fun onQuantityChanged(value: String) {
        formState = formState.copy(quantity = value, message = null)
    }

    fun onUnitPriceChanged(value: String) {
        formState = formState.copy(unitPrice = value, message = null)
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
            quoteRepository.create(
                customerId = formState.selectedCustomerId ?: return@launch,
                status = formState.status.label,
                notes = formState.notes,
                items = formState.items.map {
                    QuoteItemInput(
                        serviceProductId = it.serviceProductId,
                        quantity = it.quantity,
                        practicedUnitPrice = it.unitPrice,
                    )
                },
            )
            formState = QuoteFormState(message = "Orcamento criado.")
        }
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

    companion object {
        fun factory(
            quoteRepository: QuoteRepository,
            quoteConversionRepository: QuoteConversionRepository,
            customerRepository: CustomerRepository,
            serviceProductRepository: ServiceProductRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QuoteViewModel(
                        quoteRepository = quoteRepository,
                        quoteConversionRepository = quoteConversionRepository,
                        customerRepository = customerRepository,
                        serviceProductRepository = serviceProductRepository,
                    ) as T
                }
            }
    }
}
