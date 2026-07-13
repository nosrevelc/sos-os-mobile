package br.com.sos.osmobile.feature.services

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServiceProductFormState(
    val editingId: Long? = null,
    val code: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val unitPrice: String = "",
    val message: String? = null,
)

data class ServiceProductUiState(
    val items: List<ServiceProductEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceProductViewModel(
    private val serviceProductRepository: ServiceProductRepository,
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
        .map { ServiceProductUiState(items = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServiceProductUiState())

    var formState by mutableStateOf(ServiceProductFormState())
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

    fun onCategoryChanged(value: String) {
        formState = formState.copy(category = value, message = null)
    }

    fun onDescriptionChanged(value: String) {
        formState = formState.copy(description = value, message = null)
    }

    fun onUnitPriceChanged(value: String) {
        formState = formState.copy(unitPrice = InputMasks.currency(value), message = null)
    }

    fun startEditing(item: ServiceProductEntity) {
        formState = ServiceProductFormState(
            editingId = item.id,
            code = item.codigo,
            name = item.nome,
            category = item.categoria.orEmpty(),
            description = item.descricao.orEmpty(),
            unitPrice = InputMasks.currencyFromDouble(item.unitPrice),
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
        viewModelScope.launch {
            try {
                val editingId = formState.editingId
                if (editingId == null) {
                    serviceProductRepository.create(
                        name = formState.name,
                        category = formState.category,
                        description = formState.description,
                        unitPrice = price,
                    )
                    formState = ServiceProductFormState(message = "Servico/produto cadastrado com sucesso.")
                } else {
                    serviceProductRepository.update(
                        id = editingId,
                        code = formState.code,
                        name = formState.name,
                        category = formState.category,
                        description = formState.description,
                        unitPrice = price,
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

    companion object {
        fun factory(repository: ServiceProductRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ServiceProductViewModel(repository) as T
                }
            }
    }
}
