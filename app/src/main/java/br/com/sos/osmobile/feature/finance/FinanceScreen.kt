package br.com.sos.osmobile.feature.finance

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun FinanceScreen(viewModel: FinanceViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Financeiro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = viewModel.startDate,
                        onValueChange = viewModel::onStartDateChanged,
                        label = { Text("Inicio") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = viewModel.endDate,
                        onValueChange = viewModel::onEndDateChanged,
                        label = { Text("Fim") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = viewModel::useToday, modifier = Modifier.weight(1f)) {
                        Text("Hoje")
                    }
                    Button(onClick = viewModel::useCurrentMonth, modifier = Modifier.weight(1f)) {
                        Text("Mes atual")
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Recebido", money(state.paidInPeriod), Modifier.weight(1f))
                MetricCard("Pendente", money(state.pendingInPeriod), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Total OS", money(state.totalInPeriod), Modifier.weight(1f))
                MetricCard("Hoje", money(state.paidToday), Modifier.weight(1f))
            }
        }
        item {
            MetricCard("Mes", money(state.paidThisMonth), Modifier.fillMaxWidth())
        }

        item {
            Text("Por forma de pagamento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (state.byMethod.isEmpty()) {
                Text("Sem pagamentos no periodo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(state.byMethod.entries.toList(), key = { it.key }) { entry ->
            Text("${entry.key}: ${money(entry.value)}")
        }

        item {
            Text("OS com saldo pendente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (state.pendingItems.isEmpty()) {
                Text("Nenhuma pendencia no periodo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(state.pendingItems, key = { it.workOrderId }) { item ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${item.number} - ${item.customerName}", fontWeight = FontWeight.SemiBold)
                    Text("Total ${money(item.total)} | Pago ${money(item.paid)}")
                    Text("Saldo ${money(item.balance)}", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
