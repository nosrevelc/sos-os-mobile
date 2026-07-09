package br.com.sos.osmobile.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
            Text(
                text = "Resultados",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            search.customers.forEach {
                ResultCard("Cliente", it.nome, it.telefone) { onCustomerClick(it.id) }
            }
            search.workOrders.forEach {
                ResultCard("OS ${it.number}", it.customerName, it.status) { onWorkOrderClick(it.id) }
            }
            search.quotes.forEach {
                ResultCard("Orcamento ${it.number}", it.customerName, it.status) { onQuoteClick(it.id) }
            }
            if (search.customers.isEmpty() && search.workOrders.isEmpty() && search.quotes.isEmpty()) {
                Text("Nenhum resultado encontrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun ResultCard(label: String, title: String, detail: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("Abrir", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
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
