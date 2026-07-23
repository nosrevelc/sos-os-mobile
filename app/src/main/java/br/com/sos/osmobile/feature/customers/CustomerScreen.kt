package br.com.sos.osmobile.feature.customers

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.content.ContextCompat
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.launch

@Composable
fun CustomerScreen(viewModel: CustomerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState
    val context = LocalContext.current
    var pendingContact by remember { mutableStateOf<CustomerEntity?>(null) }
    var pendingAction by remember { mutableStateOf<ContactAction?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val canWrite = grants[Manifest.permission.WRITE_CONTACTS] == true
        val canRead = grants[Manifest.permission.READ_CONTACTS] == true
        when (pendingAction) {
            ContactAction.CheckAfterCreate -> if (canRead) pendingContact?.let(viewModel::checkNewCustomerContact)
            ContactAction.Sync -> if (canRead && canWrite) pendingContact?.let(viewModel::syncContact)
            null -> Unit
        }
        pendingContact = null
        pendingAction = null
    }
    val newCustomerContactCheck = viewModel.newCustomerContactCheck
    LaunchedEffect(newCustomerContactCheck) {
        newCustomerContactCheck?.let { customer ->
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                viewModel.checkNewCustomerContact(customer)
            } else {
                pendingContact = customer
                pendingAction = ContactAction.CheckAfterCreate
                contactPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CustomerForm(
                form = form,
                cpfCnpjPolicy = uiState.cpfCnpjPolicy,
                onNameChanged = viewModel::onNameChanged,
                onPhoneChanged = viewModel::onPhoneChanged,
                onCpfCnpjChanged = viewModel::onCpfCnpjChanged,
                onEmailChanged = viewModel::onEmailChanged,
                onAddressChanged = viewModel::onAddressChanged,
                onNotesChanged = viewModel::onNotesChanged,
                onSubmit = viewModel::saveCustomer,
                onCancel = viewModel::cancelEditing,
            )
        }

        item {
            viewModel.listMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChanged,
                label = { Text("Buscar por nome, telefone, CPF/CNPJ, email ou endereco") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text(
                text = "Clientes ativos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (uiState.customers.isEmpty()) {
            item {
                Text(
                    text = "Nenhum cliente encontrado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.customers, key = { it.id }) { customer ->
                CustomerRow(
                    customer = customer,
                    onEdit = {
                        viewModel.startEditing(customer)
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    onArchive = { viewModel.archiveCustomer(customer.id) },
                    onSyncContact = {
                        if (
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                        ) {
                            viewModel.syncContact(customer)
                        } else {
                            pendingContact = customer
                            pendingAction = ContactAction.Sync
                            contactPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
                        }
                    },
                )
            }
        }
    }

    viewModel.addContactPrompt?.let { customer ->
        AlertDialog(
            onDismissRequest = viewModel::dismissAddContactPrompt,
            title = { Text("Adicionar na agenda?") },
            text = { Text("Deseja salvar este cliente nos contatos do aparelho?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissAddContactPrompt()
                    if (
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CONTACTS) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                    ) {
                        viewModel.syncContact(customer)
                    } else {
                        pendingContact = customer
                        pendingAction = ContactAction.Sync
                        contactPermissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS))
                    }
                }) {
                    Text("Adicionar")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAddContactPrompt) {
                    Text("Agora nao")
                }
            },
        )
    }
}

private enum class ContactAction {
    CheckAfterCreate,
    Sync,
}

@Composable
private fun CustomerForm(
    form: CustomerFormState,
    cpfCnpjPolicy: CpfCnpjPolicy,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onCpfCnpjChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onAddressChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    var phoneField by remember { mutableStateOf(TextFieldValue(form.phone, TextRange(form.phone.length))) }
    var cpfCnpjField by remember { mutableStateOf(TextFieldValue(form.cpfCnpj, TextRange(form.cpfCnpj.length))) }
    LaunchedEffect(form.phone) {
        if (phoneField.text != form.phone) phoneField = TextFieldValue(form.phone, TextRange(form.phone.length))
    }
    LaunchedEffect(form.cpfCnpj) {
        if (cpfCnpjField.text != form.cpfCnpj) cpfCnpjField = TextFieldValue(form.cpfCnpj, TextRange(form.cpfCnpj.length))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (form.editingId == null) "Novo cliente" else "Editar cliente",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChanged,
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = phoneField,
            onValueChange = {
                val masked = InputMasks.phone(it.text)
                phoneField = TextFieldValue(masked, TextRange(masked.length))
                onPhoneChanged(masked)
            },
            label = { Text("Telefone") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        if (cpfCnpjPolicy != CpfCnpjPolicy.NotUsed) {
            OutlinedTextField(
                value = cpfCnpjField,
                onValueChange = {
                    val masked = InputMasks.cpfCnpj(it.text)
                    cpfCnpjField = TextFieldValue(masked, TextRange(masked.length))
                    onCpfCnpjChanged(masked)
                },
                label = { Text(if (cpfCnpjPolicy == CpfCnpjPolicy.Required) "CPF/CNPJ obrigatorio" else "CPF/CNPJ") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedTextField(
            value = form.email,
            onValueChange = onEmailChanged,
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.address,
            onValueChange = onAddressChanged,
            label = { Text("Endereco") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.notes,
            onValueChange = onNotesChanged,
            label = { Text("Observacoes") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        form.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (form.editingId != null) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }
            Button(onClick = onSubmit) {
                Text(if (form.editingId == null) "Cadastrar" else "Salvar")
            }
        }
    }
}

@Composable
private fun CustomerRow(
    customer: CustomerEntity,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onSyncContact: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(customer.nome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(customer.telefone, style = MaterialTheme.typography.bodyMedium)
            customer.email?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            customer.cpfCnpj?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            customer.endereco?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onSyncContact) {
                    Text("Agenda")
                }
                TextButton(onClick = onEdit) {
                    Text("Editar")
                }
                TextButton(onClick = onArchive) {
                    Text("Arquivar")
                }
            }
        }
    }
}
