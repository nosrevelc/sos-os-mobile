package br.com.sos.osmobile.feature.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import br.com.sos.osmobile.ui.components.CustomerSearchSelector
import br.com.sos.osmobile.ui.components.ServiceProductSearchSelector
import java.text.NumberFormat
import java.util.Locale

@Composable
fun SaleScreen(viewModel: SaleViewModel) {
    val state by viewModel.uiState.collectAsState()
    val form = viewModel.formState

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Nova venda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                CustomerSearchSelector(
                    customers = state.customers,
                    selectedCustomerId = form.selectedCustomerId,
                    onCustomerSelected = viewModel::selectCustomer,
                    emptyText = "Cadastre um cliente antes da venda.",
                )
                ServiceProductSearchSelector(
                    services = state.services,
                    selectedServiceProductId = form.selectedServiceProductId,
                    onServiceSelected = viewModel::selectService,
                    emptyText = "Cadastre produtos/servicos antes da venda.",
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = form.quantity,
                        onValueChange = viewModel::onQuantityChanged,
                        label = { Text("Qtd") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.unitPrice,
                        onValueChange = viewModel::onUnitPriceChanged,
                        label = { Text("Valor") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(onClick = viewModel::addSelectedItem, modifier = Modifier.fillMaxWidth()) {
                    Text("Adicionar item")
                }
                form.items.forEachIndexed { index, item ->
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("${item.name} ${item.quantity} x ${money(item.unitPrice)}")
                        TextButton(onClick = { viewModel.removeItem(index) }) {
                            Text("Remover")
                        }
                    }
                }
                Text("Total: ${money(form.items.sumOf { it.subtotal })}", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = form.paymentMethod,
                        onValueChange = viewModel::onPaymentMethodChanged,
                        label = { Text("Pagamento") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = form.paidValue,
                        onValueChange = viewModel::onPaidValueChanged,
                        label = { Text("Pago") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
                form.message?.let { Text(it, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold) }
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text("Registrar venda")
                }
            }
        }

        item {
            Text("Ultimas vendas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        items(state.sales, key = { it.id }) { sale ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${sale.number} - ${sale.customerName}", fontWeight = FontWeight.SemiBold)
                    Text("${money(sale.totalValue)} | Pago ${money(sale.paidValue)} | ${sale.paymentMethod}")
                    Text("Fiscal: ${sale.fiscalStatus}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
