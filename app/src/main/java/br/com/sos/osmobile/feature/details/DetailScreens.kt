package br.com.sos.osmobile.feature.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CustomerDetailScreen(
    viewModel: CustomerDetailViewModel,
    onQuoteClick: (Long) -> Unit,
    onWorkOrderClick: (Long) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    DetailColumn {
        state.customer?.let {
            Title(it.nome)
            Text(it.telefone)
            Text(it.email.orEmpty())
            Text(it.endereco.orEmpty())
        } ?: Text("Cliente nao encontrado.")
        Title("OS do cliente")
        state.workOrders.forEach {
            DetailButton("${it.number} - ${it.status}") { onWorkOrderClick(it.id) }
        }
        Title("Orcamentos do cliente")
        state.quotes.forEach {
            DetailButton("${it.number} - ${it.status}") { onQuoteClick(it.id) }
        }
    }
}

@Composable
fun QuoteDetailScreen(viewModel: QuoteDetailViewModel) {
    val state by viewModel.uiState.collectAsState()
    DetailColumn {
        state.quote?.let {
            Title("Orcamento ${it.number}")
            Text(it.customerName)
            Text("${it.status} - R$ ${it.totalValue}")
        } ?: Text("Orcamento nao encontrado.")
        Title("Itens")
        state.items.forEach { SimpleCard("${it.name}: ${it.quantity} x R$ ${it.unitPrice}") }
        Title("Historico")
        state.history.forEach { Text(it) }
    }
}

@Composable
fun WorkOrderDetailScreen(viewModel: WorkOrderDetailViewModel) {
    val state by viewModel.uiState.collectAsState()
    DetailColumn {
        state.workOrder?.let {
            Title("OS ${it.number}")
            Text(it.customerName)
            Text("${it.status} - R$ ${it.totalValue}")
        } ?: Text("OS nao encontrada.")
        Title("Itens")
        state.items.forEach { SimpleCard("${it.name}: ${it.quantity} x R$ ${it.unitPrice}") }
        Title("Historico")
        state.history.forEach { Text(it) }
    }
}

@Composable
private fun DetailColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

@Composable
private fun Title(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun SimpleCard(text: String) {
    Card { Text(text, modifier = Modifier.padding(12.dp)) }
}

@Composable
private fun DetailButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text)
    }
}
