package br.com.sos.osmobile.feature.sales

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.entity.StockMovementType
import br.com.sos.osmobile.data.local.model.SaleSummary
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.SaleItemInput
import br.com.sos.osmobile.data.repository.SaleRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.StockRepository
import br.com.sos.osmobile.feature.workorders.WorkOrderFormValidator
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SaleDraftItem(
    val serviceProductId: Long,
    val name: String,
    val type: String,
    val quantity: Double,
    val unitPrice: Double,
) {
    val subtotal: Double = quantity * unitPrice
}

data class SaleFormState(
    val selectedCustomerId: Long? = null,
    val selectedServiceProductId: Long? = null,
    val quantity: String = "1",
    val unitPrice: String = "",
    val paymentMethod: String = "PIX",
    val paidValue: String = "",
    val items: List<SaleDraftItem> = emptyList(),
    val message: String? = null,
)

data class SaleUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val services: List<ServiceProductEntity> = emptyList(),
    val stockByServiceProductId: Map<Long, Double> = emptyMap(),
    val sales: List<SaleSummary> = emptyList(),
)

class SaleViewModel(
    private val saleRepository: SaleRepository,
    private val customerRepository: CustomerRepository,
    private val serviceProductRepository: ServiceProductRepository,
    private val stockRepository: StockRepository,
) : ViewModel() {
    val uiState: StateFlow<SaleUiState> = combine(
        customerRepository.observeActive(),
        serviceProductRepository.observeActive(),
        stockRepository.observeSummaries(),
        saleRepository.observeSummaries(),
    ) { customers, services, stock, sales ->
        SaleUiState(customers, services, stock.associate { it.id to it.saldo }, sales)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SaleUiState())

    var formState by mutableStateOf(SaleFormState())
        private set

    fun selectCustomer(id: Long) {
        formState = formState.copy(selectedCustomerId = id, message = null)
    }

    fun selectService(item: ServiceProductEntity) {
        formState = formState.copy(
            selectedServiceProductId = item.id,
            unitPrice = InputMasks.currencyFromDouble(item.unitPrice),
            message = null,
        )
    }

    fun onQuantityChanged(value: String) {
        formState = formState.copy(quantity = InputMasks.decimal(value, integerDigits = 5, decimalDigits = 2), message = null)
    }

    fun onUnitPriceChanged(value: String) {
        formState = formState.copy(unitPrice = InputMasks.currency(value), message = null)
    }

    fun onPaidValueChanged(value: String) {
        formState = formState.copy(paidValue = InputMasks.currency(value), message = null)
    }

    fun onPaymentMethodChanged(value: String) {
        formState = formState.copy(paymentMethod = value, message = null)
    }

    fun addSelectedItem() {
        val service = uiState.value.services.firstOrNull { it.id == formState.selectedServiceProductId }
        if (service == null) {
            formState = formState.copy(message = "Selecione um produto/servico.")
            return
        }
        val quantity = WorkOrderFormValidator.parseDecimal(formState.quantity)
        val unitPrice = WorkOrderFormValidator.parseDecimal(formState.unitPrice)
        if (quantity == null || quantity <= 0.0 || unitPrice == null || unitPrice < 0.0) {
            formState = formState.copy(message = "Item invalido.")
            return
        }
        if (service.tipo != ServiceProductType.SERVICE) {
            val already = formState.items.filter { it.serviceProductId == service.id }.sumOf { it.quantity }
            val available = uiState.value.stockByServiceProductId[service.id] ?: 0.0
            if (already + quantity > available) {
                formState = formState.copy(message = "Saldo insuficiente para ${service.nome}.")
                return
            }
        }
        formState = formState.copy(
            items = formState.items + SaleDraftItem(service.id, service.nome, service.tipo, quantity, unitPrice),
            selectedServiceProductId = null,
            quantity = "1",
            unitPrice = "",
            message = null,
        )
    }

    fun removeItem(index: Int) {
        formState = formState.copy(items = formState.items.filterIndexed { itemIndex, _ -> itemIndex != index })
    }

    fun save() {
        val customerId = formState.selectedCustomerId
        if (customerId == null || formState.items.isEmpty()) {
            formState = formState.copy(message = "Informe cliente e itens.")
            return
        }
        val total = formState.items.sumOf { it.subtotal }
        val paid = WorkOrderFormValidator.parseDecimal(formState.paidValue) ?: total
        viewModelScope.launch {
            val saleId = saleRepository.create(
                customerId = customerId,
                paymentMethod = formState.paymentMethod,
                paidValue = paid.coerceAtMost(total),
                items = formState.items.map { SaleItemInput(it.serviceProductId, it.quantity, it.unitPrice) },
            )
            formState.items.filter { it.type != ServiceProductType.SERVICE }.forEach {
                stockRepository.move(it.serviceProductId, StockMovementType.OUT, it.quantity, "Baixa pela venda $saleId")
            }
            formState = SaleFormState(message = "Venda registrada.")
        }
    }

    companion object {
        fun factory(
            saleRepository: SaleRepository,
            customerRepository: CustomerRepository,
            serviceProductRepository: ServiceProductRepository,
            stockRepository: StockRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    SaleViewModel(saleRepository, customerRepository, serviceProductRepository, stockRepository) as T
            }
    }
}
