package br.com.sos.osmobile.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.data.local.entity.CustomerEntity

@Composable
fun CustomerSearchSelector(
    customers: List<CustomerEntity>,
    selectedCustomerId: Long?,
    onCustomerSelected: (Long) -> Unit,
    emptyText: String,
    modifier: Modifier = Modifier,
) {
    if (customers.isEmpty()) {
        Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }

    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }
    val filteredCustomers = remember(customers, query) {
        val normalized = query.trim()
        if (normalized.isBlank()) {
            customers.take(20)
        } else {
            customers
                .filter {
                    it.nome.contains(normalized, ignoreCase = true) ||
                        it.telefone.contains(normalized, ignoreCase = true) ||
                        it.cpfCnpj.orEmpty().contains(normalized, ignoreCase = true) ||
                        it.email.orEmpty().contains(normalized, ignoreCase = true)
                }
                .take(20)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedCustomer?.let { "${it.nome} - ${it.telefone}" } ?: "Selecionar cliente")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 320.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Buscar cliente") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (filteredCustomers.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Nenhum cliente encontrado") },
                    onClick = {},
                    enabled = false,
                )
            } else {
                filteredCustomers.forEach { customer ->
                    DropdownMenuItem(
                        text = { Text("${customer.nome} - ${customer.telefone}") },
                        onClick = {
                            onCustomerSelected(customer.id)
                            query = ""
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
