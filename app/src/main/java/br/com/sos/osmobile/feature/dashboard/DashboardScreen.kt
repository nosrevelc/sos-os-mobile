package br.com.sos.osmobile.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HeaderCard(today = viewModel.todayLabel)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Periodo", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = viewModel.startDate,
                        onValueChange = viewModel::onStartDateChanged,
                        label = { Text("Data inicial") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = viewModel.endDate,
                        onValueChange = viewModel::onEndDateChanged,
                        label = { Text("Data final") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Button(onClick = viewModel::resetToCurrentMonth, modifier = Modifier.fillMaxWidth()) {
                    Text("Mes atual")
                }
            }
        }

        OutlinedTextField(
            value = viewModel.query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Buscar cliente, telefone, OS ou orcamento") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (viewModel.query.isNotBlank()) {
            SearchResults(
                search = search,
                onCustomerClick = onCustomerClick,
                onQuoteClick = onQuoteClick,
                onWorkOrderClick = onWorkOrderClick,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                label = "OS no periodo",
                value = metrics.workOrderCount.toString(),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Faturamento OS",
                value = money(metrics.workOrderRevenue),
                color = Color(0xFFDDF7E3),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                label = "OS abertas",
                value = metrics.openWorkOrders.toString(),
                color = Color(0xFFFFF0C2),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Orcamentos",
                value = metrics.quoteCount.toString(),
                color = Color(0xFFDDEBFF),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                label = "Valor orcado",
                value = money(metrics.quoteTotal),
                color = Color(0xFFE8DFFF),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Orc. pendentes",
                value = metrics.pendingQuotes.toString(),
                color = Color(0xFFFFE0E0),
                modifier = Modifier.weight(1f),
            )
        }

        RankSection(
            title = "Top 10 servicos por quantidade",
            items = metrics.topServices,
            valueMode = RankValueMode.Quantity,
            barColor = MaterialTheme.colorScheme.primary,
        )
        RankSection(
            title = "Top 10 clientes por valor",
            items = metrics.topCustomersByValue,
            valueMode = RankValueMode.Value,
            barColor = Color(0xFF2E7D32),
        )
        RankSection(
            title = "Top 10 clientes por quantidade de servicos",
            items = metrics.topCustomersByQuantity,
            valueMode = RankValueMode.Quantity,
            barColor = Color(0xFFEF6C00),
        )
    }
}

@Composable
private fun HeaderCard(today: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Painel", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            Text("Hoje: $today", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimary)
            Text("Dados carregados pelo mes atual, com filtro livre por data.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}

@Composable
private fun SearchResults(
    search: DashboardSearchResult,
    onCustomerClick: (Long) -> Unit,
    onQuoteClick: (Long) -> Unit,
    onWorkOrderClick: (Long) -> Unit,
) {
    Text("Resultados", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF263238))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF101820))
        }
    }
}

private enum class RankValueMode {
    Quantity,
    Value,
}

@Composable
private fun RankSection(
    title: String,
    items: List<DashboardRankItem>,
    valueMode: RankValueMode,
    barColor: Color,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (items.isEmpty()) {
                Text("Sem dados no periodo.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val maxValue = items.maxOf {
                    when (valueMode) {
                        RankValueMode.Quantity -> it.quantity
                        RankValueMode.Value -> it.totalValue
                    }
                }.takeIf { it > 0.0 } ?: 1.0
                items.forEachIndexed { index, item ->
                    val value = when (valueMode) {
                        RankValueMode.Quantity -> item.quantity
                        RankValueMode.Value -> item.totalValue
                    }
                    RankBar(
                        position = index + 1,
                        item = item,
                        value = value,
                        maxValue = maxValue,
                        valueMode = valueMode,
                        barColor = barColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun RankBar(
    position: Int,
    item: DashboardRankItem,
    value: Double,
    maxValue: Double,
    valueMode: RankValueMode,
    barColor: Color,
) {
    val fraction = (value / maxValue).toFloat().coerceIn(0.04f, 1f)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$position. ${item.label}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(
                text = when (valueMode) {
                    RankValueMode.Quantity -> quantity(value)
                    RankValueMode.Value -> money(value)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(barColor),
            )
        }
        if (valueMode == RankValueMode.Quantity) {
            Text("Valor: ${money(item.totalValue)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun money(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

private fun quantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale("pt", "BR"), "%.1f", value)
