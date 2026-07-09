package br.com.sos.osmobile.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCustomerClick: (Long) -> Unit,
    onQuoteClick: (Long) -> Unit,
    onWorkOrderClick: (Long) -> Unit,
) {
    val metrics by viewModel.metrics.collectAsState()
    val search by viewModel.searchResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = viewModel.query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Buscar cliente, telefone, OS ou orcamento") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (viewModel.query.isNotBlank()) {
            search.customers.forEach {
                ResultButton("Cliente", "${it.nome} - ${it.telefone}") { onCustomerClick(it.id) }
            }
            search.workOrders.forEach {
                ResultButton("OS ${it.number}", "${it.customerName} - ${it.status}") { onWorkOrderClick(it.id) }
            }
            search.quotes.forEach {
                ResultButton("Orcamento ${it.number}", "${it.customerName} - ${it.status}") { onQuoteClick(it.id) }
            }
        }
        MetricCard("OS cadastradas", metrics.workOrderCount.toString())
        MetricCard("Faturamento em OS", money(metrics.workOrderRevenue))
        MetricCard("OS abertas/em andamento", metrics.openWorkOrders.toString())
        MetricCard("Orcamentos cadastrados", metrics.quoteCount.toString())
        MetricCard("Valor em orcamentos", money(metrics.quoteTotal))
        MetricCard("Orcamentos pendentes", metrics.pendingQuotes.toString())
    }
}

@Composable
private fun ResultButton(label: String, value: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("$label: $value")
    }
}

@Composable
private fun MetricCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
