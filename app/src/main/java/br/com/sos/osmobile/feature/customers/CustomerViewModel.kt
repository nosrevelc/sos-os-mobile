package br.com.sos.osmobile.feature.customers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomerFormState(
    val editingId: Long? = null,
    val name: String = "",
    val phone: String = "",
    val cpfCnpj: String = "",
    val email: String = "",
    val address: String = "",
    val notes: String = "",
    val message: String? = null,
)

data class CustomerUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val cpfCnpjPolicy: CpfCnpjPolicy = CpfCnpjPolicy.Optional,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerViewModel(
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<CustomerUiState> = combine(
        searchQuery.flatMapLatest { query ->
            if (query.isBlank()) {
                customerRepository.observeActive()
            } else {
                customerRepository.search(query.trim())
            }
        },
        settingsRepository.observeCpfCnpjPolicy(),
    ) { customers, cpfCnpjPolicy ->
        CustomerUiState(
            customers = customers,
            cpfCnpjPolicy = cpfCnpjPolicy,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CustomerUiState())

    var formState by mutableStateOf(CustomerFormState())
        private set

    var query by mutableStateOf("")
        private set

    fun onQueryChanged(value: String) {
        query = value
        searchQuery.value = value
    }

    fun onNameChanged(value: String) {
        formState = formState.copy(name = value, message = null)
    }

    fun onPhoneChanged(value: String) {
        formState = formState.copy(phone = value, message = null)
    }

    fun onCpfCnpjChanged(value: String) {
        formState = formState.copy(cpfCnpj = value, message = null)
    }

    fun onEmailChanged(value: String) {
        formState = formState.copy(email = value, message = null)
    }

    fun onAddressChanged(value: String) {
        formState = formState.copy(address = value, message = null)
    }

    fun onNotesChanged(value: String) {
        formState = formState.copy(notes = value, message = null)
    }

    fun startEditing(customer: CustomerEntity) {
        formState = CustomerFormState(
            editingId = customer.id,
            name = customer.nome,
            phone = customer.telefone,
            cpfCnpj = customer.cpfCnpj.orEmpty(),
            email = customer.email.orEmpty(),
            address = customer.endereco.orEmpty(),
            notes = customer.observacoes.orEmpty(),
        )
    }

    fun cancelEditing() {
        formState = CustomerFormState()
    }

    fun saveCustomer() {
        val validationMessage = CustomerFormValidator.validate(
            form = formState,
            cpfCnpjPolicy = uiState.value.cpfCnpjPolicy,
        )
        if (validationMessage != null) {
            formState = formState.copy(message = validationMessage)
            return
        }

        viewModelScope.launch {
            try {
                val editingId = formState.editingId
                if (editingId == null) {
                    customerRepository.create(
                        name = formState.name,
                        phone = formState.phone,
                        cpfCnpj = formState.cpfCnpj,
                        email = formState.email,
                        address = formState.address,
                        notes = formState.notes,
                    )
                    formState = CustomerFormState(message = "Cliente cadastrado.")
                } else {
                    customerRepository.update(
                        id = editingId,
                        name = formState.name,
                        phone = formState.phone,
                        cpfCnpj = formState.cpfCnpj,
                        email = formState.email,
                        address = formState.address,
                        notes = formState.notes,
                    )
                    formState = CustomerFormState(message = "Cliente atualizado.")
                }
            } catch (_: SQLiteConstraintException) {
                formState = formState.copy(message = "CPF/CNPJ ja cadastrado.")
            }
        }
    }

    fun archiveCustomer(id: Long) {
        viewModelScope.launch {
            customerRepository.archive(id)
            if (formState.editingId == id) {
                formState = CustomerFormState(message = "Cliente arquivado.")
            }
        }
    }

    companion object {
        fun factory(
            customerRepository: CustomerRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CustomerViewModel(customerRepository, settingsRepository) as T
                }
            }
    }
}
