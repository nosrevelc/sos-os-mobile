package br.com.sos.osmobile.feature.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.repository.CustomerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CustomerFormState(
    val name: String = "",
    val phone: String = "",
    val cpfCnpj: String = "",
    val message: String? = null,
)

class CustomerViewModel(
    private val customerRepository: CustomerRepository,
) : ViewModel() {
    val customers: StateFlow<List<CustomerEntity>> = customerRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    var formState by mutableStateOf(CustomerFormState())
        private set

    fun onNameChanged(value: String) {
        formState = formState.copy(name = value, message = null)
    }

    fun onPhoneChanged(value: String) {
        formState = formState.copy(phone = value, message = null)
    }

    fun onCpfCnpjChanged(value: String) {
        formState = formState.copy(cpfCnpj = value, message = null)
    }

    fun createCustomer() {
        val name = formState.name.trim()
        val phone = formState.phone.trim()
        if (name.isBlank() || phone.isBlank()) {
            formState = formState.copy(message = "Nome e telefone sao obrigatorios.")
            return
        }

        viewModelScope.launch {
            customerRepository.create(
                name = name,
                phone = phone,
                cpfCnpj = formState.cpfCnpj,
                email = null,
                address = null,
                notes = null,
            )
            formState = CustomerFormState(message = "Cliente cadastrado.")
        }
    }

    fun archiveCustomer(id: Long) {
        viewModelScope.launch {
            customerRepository.archive(id)
        }
    }

    companion object {
        fun factory(repository: CustomerRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CustomerViewModel(repository) as T
                }
            }
    }
}
