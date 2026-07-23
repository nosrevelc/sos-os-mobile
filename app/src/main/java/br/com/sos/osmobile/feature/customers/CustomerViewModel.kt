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
import br.com.sos.osmobile.data.repository.ContactsRepository
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CONTACTS_GOOGLE_ACCOUNT_KEY
import br.com.sos.osmobile.ui.input.InputMasks
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
    private val contactsRepository: ContactsRepository,
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

    var listMessage by mutableStateOf<String?>(null)
        private set

    var newCustomerContactCheck by mutableStateOf<CustomerEntity?>(null)
        private set

    var addContactPrompt by mutableStateOf<CustomerEntity?>(null)
        private set

    fun onQueryChanged(value: String) {
        query = value
        searchQuery.value = value
    }

    fun onNameChanged(value: String) {
        formState = formState.copy(name = value, message = null)
    }

    fun onPhoneChanged(value: String) {
        formState = formState.copy(phone = InputMasks.phone(value), message = null)
    }

    fun onCpfCnpjChanged(value: String) {
        formState = formState.copy(cpfCnpj = InputMasks.cpfCnpj(value), message = null)
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
            message = "Cliente disponivel para edicao.",
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
                    val id = customerRepository.create(
                        name = formState.name,
                        phone = formState.phone,
                        cpfCnpj = formState.cpfCnpj,
                        email = formState.email,
                        address = formState.address,
                        notes = formState.notes,
                    )
                    formState = CustomerFormState(message = "Cliente cadastrado com sucesso.")
                    newCustomerContactCheck = customerRepository.findById(id)
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
                    formState = CustomerFormState(message = "Edicao concluida com sucesso.")
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

    fun syncContact(customer: CustomerEntity) {
        viewModelScope.launch {
            listMessage = runCatching {
                val account = settingsRepository.getString(CONTACTS_GOOGLE_ACCOUNT_KEY)
                val currentRawContactId = settingsRepository.getContactRawId(customer.id)
                if (currentRawContactId == null && contactsRepository.contactExists(customer)) {
                    return@runCatching "Cliente ja existe na agenda. Nao foi aberto novo cadastro para evitar duplicidade."
                }
                val rawContactId = contactsRepository.syncCustomer(
                    customer = customer,
                    googleAccount = account,
                    existingRawContactId = currentRawContactId,
                )
                settingsRepository.setContactRawId(customer.id, rawContactId)
                account?.takeIf { it.isNotBlank() }
                    ?.let { "Contato salvo na agenda Google configurada." }
                    ?: "Contato salvo na agenda local do aparelho."
            }.getOrElse {
                "Nao foi possivel salvar na agenda: ${it.message ?: "verifique a permissao de contatos"}"
            }
        }
    }

    fun checkNewCustomerContact(customer: CustomerEntity) {
        viewModelScope.launch {
            if (contactsRepository.contactExists(customer)) {
                val message = "Cliente cadastrado com sucesso. Cliente ja existe na agenda; nao foi aberto novo cadastro."
                listMessage = message
                formState = formState.copy(message = message)
            } else {
                formState = formState.copy(message = "Cliente cadastrado com sucesso. Confirme se deseja adicionar na agenda.")
                addContactPrompt = customer
            }
            newCustomerContactCheck = null
        }
    }

    fun consumeNewCustomerContactCheck() {
        newCustomerContactCheck = null
    }

    fun dismissAddContactPrompt() {
        addContactPrompt = null
    }

    companion object {
        fun factory(
            customerRepository: CustomerRepository,
            settingsRepository: SettingsRepository,
            contactsRepository: ContactsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CustomerViewModel(customerRepository, settingsRepository, contactsRepository) as T
                }
            }
    }
}
