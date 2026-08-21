package br.com.sos.osmobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.padding
import br.com.sos.osmobile.core.format.Formatters
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.ui.input.InputMasks

@Composable
fun CustomerSection(
    customers: List<br.com.sos.osmobile.data.local.entity.CustomerEntity>,
    selectedCustomerId: Long?,
    onCustomerSelected: (Long) -> Unit,
    emptyText: String,
) {
    Text("Cliente", style = MaterialTheme.typography.titleSmall)
    CustomerSearchSelector(
        customers = customers,
        selectedCustomerId = selectedCustomerId,
        onCustomerSelected = onCustomerSelected,
        emptyText = emptyText,
    )
}

@Composable
fun DocumentItemsEditor(
    services: List<ServiceProductEntity>,
    selectedServiceProductId: Long?,
    onServiceSelected: (ServiceProductEntity) -> Unit,
    emptyServicesText: String,
    quantity: String,
    unitPrice: String,
    onQuantityChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
    onAddItem: () -> Unit,
    showAddIcon: Boolean = false,
    rows: @Composable () -> Unit = {},
) {
    var quantityField by remember { mutableStateOf(TextFieldValue(quantity, TextRange(quantity.length))) }
    var unitPriceField by remember { mutableStateOf(TextFieldValue(unitPrice, TextRange(unitPrice.length))) }

    LaunchedEffect(quantity) {
        if (quantityField.text != quantity) {
            quantityField = TextFieldValue(quantity, TextRange(quantity.length))
        }
    }
    LaunchedEffect(unitPrice) {
        if (unitPriceField.text != unitPrice) {
            unitPriceField = TextFieldValue(unitPrice, TextRange(unitPrice.length))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Itens", style = MaterialTheme.typography.titleSmall)
        ServiceProductSearchSelector(
            services = services,
            selectedServiceProductId = selectedServiceProductId,
            onServiceSelected = onServiceSelected,
            emptyText = emptyServicesText,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = quantityField,
                onValueChange = {
                    val masked = InputMasks.decimal(it.text, integerDigits = 5, decimalDigits = 2)
                    quantityField = TextFieldValue(masked, TextRange(masked.length))
                    onQuantityChanged(masked)
                },
                label = { Text("Qtd") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = unitPriceField,
                onValueChange = {
                    val masked = InputMasks.currency(it.text)
                    unitPriceField = TextFieldValue(masked, TextRange(masked.length))
                    onUnitPriceChanged(masked)
                },
                label = { Text("Valor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(2f),
            )
        }

        OutlinedButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
            if (showAddIcon) {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
            Text("Adicionar item")
        }

        rows()
    }
}

@Composable
fun DocumentDraftItemRow(
    name: String,
    quantity: String,
    unitPrice: Double,
    subtotal: Double,
    onRemoveItem: () -> Unit,
    showDeleteIcon: Boolean = false,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "$quantity x ${Formatters.currency(unitPrice)} = ${Formatters.currency(subtotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onRemoveItem) {
                if (showDeleteIcon) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                }
                Text("Remover")
            }
        }
    }
}
