package br.com.sos.osmobile.feature.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.entity.StockMovementType
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.StockRepository
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServiceProductFormState(
    val editingId: Long? = null,
    val code: String = "",
    val name: String = "",
    val type: String = ServiceProductType.SERVICE,
    val category: String = "",
    val description: String = "",
    val unitPrice: String = "",
    val minimumStock: String = "0",
    val ncm: String = "",
    val cfop: String = "",
    val unit: String = "UN",
    val cstCsosn: String = "",
    val message: String? = null,
)

data class ServiceProductStockItem(
    val item: ServiceProductEntity,
    val stock: Double,
)

data class StockMovementFormState(
    val serviceProductId: Long? = null,
    val serviceProductName: String = "",
    val type: String = StockMovementType.IN,
    val quantity: String = "",
    val reason: String = "",
    val message: String? = null,
)

data class ServiceProductUiState(
    val items: List<ServiceProductStockItem> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceProductViewModel(
    private val serviceProductRepository: ServiceProductRepository,
    private val stockRepository: StockRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ServiceProductUiState> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                serviceProductRepository.observeActive()
            } else {
                serviceProductRepository.search(query.trim())
            }
        }
        .combine(stockRepository.observeSummaries()) { services, summaries ->
            val stockById = summaries.associate { it.id to it.saldo }
            ServiceProductUiState(items = services.map { ServiceProductStockItem(it, stockById[it.id] ?: 0.0) })
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceProductUiState())

    var formState by mutableStateOf(ServiceProductFormState())
        private set

    var stockFormState by mutableStateOf(StockMovementFormState())
        private set

    var query by mutableStateOf("")
        private set

    fun onQueryChanged(value: String) {
        query = value
        searchQuery.value = value
    }

    fun onCodeChanged(value: String) {
        formState = formState.copy(code = value, message = null)
    }

    fun onNameChanged(value: String) {
        formState = formState.copy(name = value, message = null)
    }

    fun onTypeChanged(value: String) {
        formState = formState.copy(type = value, message = null)
    }

    fun onCategoryChanged(value: String) {
        formState = formState.copy(category = value, message = null)
    }

    fun onDescriptionChanged(value: String) {
        formState = formState.copy(description = value, message = null)
    }

    fun onUnitPriceChanged(value: String) {
        formState = formState.copy(unitPrice = InputMasks.currency(value), message = null)
    }

    fun onMinimumStockChanged(value: String) {
        formState = formState.copy(minimumStock = value, message = null)
    }

    fun onNcmChanged(value: String) {
        formState = formState.copy(ncm = value.filter(Char::isDigit).take(8), message = null)
    }

    fun onCfopChanged(value: String) {
        formState = formState.copy(cfop = value.filter(Char::isDigit).take(4), message = null)
    }

    fun onUnitChanged(value: String) {
        formState = formState.copy(unit = value.uppercase().take(6), message = null)
    }

    fun onCstCsosnChanged(value: String) {
        formState = formState.copy(cstCsosn = value.filter(Char::isDigit).take(4), message = null)
    }

    fun startEditing(item: ServiceProductEntity) {
        formState = ServiceProductFormState(
            editingId = item.id,
            code = item.codigo,
            name = item.nome,
            type = item.tipo,
            category = item.categoria.orEmpty(),
            description = item.descricao.orEmpty(),
            unitPrice = InputMasks.currencyFromDouble(item.unitPrice),
            minimumStock = formatNumber(item.minimumStock),
            ncm = item.ncm.orEmpty(),
            cfop = item.cfop.orEmpty(),
            unit = item.unidade ?: "UN",
            cstCsosn = item.cstCsosn.orEmpty(),
        )
    }

    fun cancelEditing() {
        formState = ServiceProductFormState()
    }

    fun save() {
        val validationMessage = ServiceProductFormValidator.validate(formState)
        if (validationMessage != null) {
            formState = formState.copy(message = validationMessage)
            return
        }

        val price = ServiceProductFormValidator.parsePrice(formState.unitPrice) ?: return
        val minimumStock = parseQuantity(formState.minimumStock) ?: 0.0
        viewModelScope.launch {
            try {
                val editingId = formState.editingId
                if (editingId == null) {
                    serviceProductRepository.create(
                        name = formState.name,
                        type = formState.type,
                        category = formState.category,
                        description = formState.description,
                        unitPrice = price,
                        minimumStock = minimumStock,
                        ncm = formState.ncm,
                        cfop = formState.cfop,
                        unit = formState.unit,
                        cstCsosn = formState.cstCsosn,
                    )
                    formState = ServiceProductFormState(message = "Servico/produto cadastrado com sucesso.")
                } else {
                    serviceProductRepository.update(
                        id = editingId,
                        code = formState.code,
                        name = formState.name,
                        type = formState.type,
                        category = formState.category,
                        description = formState.description,
                        unitPrice = price,
                        minimumStock = minimumStock,
                        ncm = formState.ncm,
                        cfop = formState.cfop,
                        unit = formState.unit,
                        cstCsosn = formState.cstCsosn,
                    )
                    formState = ServiceProductFormState(message = "Servico/produto atualizado com sucesso.")
                }
            } catch (_: SQLiteConstraintException) {
                formState = formState.copy(message = "Codigo ja cadastrado.")
            }
        }
    }

    fun archive(id: Long) {
        viewModelScope.launch {
            serviceProductRepository.archive(id)
            if (formState.editingId == id) {
                formState = ServiceProductFormState(message = "Servico/produto arquivado.")
            }
        }
    }

    fun startStockMovement(item: ServiceProductEntity, type: String) {
        stockFormState = StockMovementFormState(
            serviceProductId = item.id,
            serviceProductName = item.nome,
            type = type,
        )
    }

    fun cancelStockMovement() {
        stockFormState = StockMovementFormState()
    }

    fun onStockQuantityChanged(value: String) {
        stockFormState = stockFormState.copy(quantity = value, message = null)
    }

    fun onStockReasonChanged(value: String) {
        stockFormState = stockFormState.copy(reason = value, message = null)
    }

    fun saveStockMovement() {
        val serviceProductId = stockFormState.serviceProductId ?: return
        val quantity = parseQuantity(stockFormState.quantity)
        if (quantity == null || quantity == 0.0) {
            stockFormState = stockFormState.copy(message = "Quantidade invalida.")
            return
        }
        if (stockFormState.type != StockMovementType.ADJUST && quantity < 0.0) {
            stockFormState = stockFormState.copy(message = "Use quantidade positiva.")
            return
        }
        viewModelScope.launch {
            stockRepository.move(
                serviceProductId = serviceProductId,
                type = stockFormState.type,
                quantity = quantity,
                reason = stockFormState.reason,
            )
            stockFormState = StockMovementFormState(message = "Movimentacao registrada.")
        }
    }

    companion object {
        fun factory(repository: ServiceProductRepository, stockRepository: StockRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ServiceProductViewModel(repository, stockRepository) as T
                }
            }
    }
}

private fun parseQuantity(value: String): Double? =
    value.trim().replace(",", ".").takeIf { it.isNotBlank() }?.toDoubleOrNull()

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace(".", ",")
