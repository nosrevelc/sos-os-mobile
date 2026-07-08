package br.com.sos.osmobile.feature.customers

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.data.local.entity.CustomerEntity

@Composable
fun CustomerScreen(viewModel: CustomerViewModel) {
    val customers by viewModel.customers.collectAsState()
    val form = viewModel.formState

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CustomerForm(
                form = form,
                onNameChanged = viewModel::onNameChanged,
                onPhoneChanged = viewModel::onPhoneChanged,
                onCpfCnpjChanged = viewModel::onCpfCnpjChanged,
                onSubmit = viewModel::createCustomer,
            )
        }

        item {
            Text(
                text = "Clientes ativos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (customers.isEmpty()) {
            item {
                Text(
                    text = "Nenhum cliente cadastrado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(customers, key = { it.id }) { customer ->
                CustomerRow(
                    customer = customer,
                    onArchive = { viewModel.archiveCustomer(customer.id) },
                )
            }
        }
    }
}

@Composable
private fun CustomerForm(
    form: CustomerFormState,
    onNameChanged: (String) -> Unit,
    onPhoneChanged: (String) -> Unit,
    onCpfCnpjChanged: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChanged,
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.phone,
            onValueChange = onPhoneChanged,
            label = { Text("Telefone") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.cpfCnpj,
            onValueChange = onCpfCnpjChanged,
            label = { Text("CPF/CNPJ") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = form.message.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = onSubmit) {
                Text("Cadastrar")
            }
        }
    }
}

@Composable
private fun CustomerRow(
    customer: CustomerEntity,
    onArchive: () -> Unit,
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
            customer.cpfCnpj?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onArchive) {
                    Text("Arquivar")
                }
            }
        }
    }
}
