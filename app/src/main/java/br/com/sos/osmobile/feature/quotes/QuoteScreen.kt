package br.com.sos.osmobile.feature.quotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.model.QuoteStatus
import br.com.sos.osmobile.ui.components.CustomerSearchSelector
import br.com.sos.osmobile.ui.components.PrintDocumentButton
import br.com.sos.osmobile.ui.components.ShareFileButton
import br.com.sos.osmobile.ui.components.SharePdfButton
import br.com.sos.osmobile.ui.components.ShareTextButton
import br.com.sos.osmobile.ui.components.ServiceProductSearchSelector
import br.com.sos.osmobile.ui.components.WhatsAppTextButton
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@Composable
fun QuoteScreen(viewModel: QuoteViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            QuoteForm(
                form = form,
                customers = uiState.customers,
                services = uiState.services,
                onCustomerSelected = viewModel::selectCustomer,
                onServiceSelected = viewModel::selectServiceProduct,
                onStatusSelected = viewModel::selectStatus,
                onQuantityChanged = viewModel::onQuantityChanged,
                onUnitPriceChanged = viewModel::onUnitPriceChanged,
                onNotesChanged = viewModel::onNotesChanged,
                onAddItem = viewModel::addSelectedItem,
                onRemoveItem = viewModel::removeItem,
                onSave = viewModel::saveQuote,
                onCancelEdit = viewModel::cancelEdit,
            )
        }

        item {
            Text(
                text = "Orcamentos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            viewModel.listMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            viewModel.documentText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
                ShareTextButton(label = "Compartilhar documento", text = it)
                ShareFileButton(label = "Compartilhar arquivo", fileName = "orcamento.txt", text = it)
                SharePdfButton(label = "Compartilhar PDF", fileName = "orcamento.pdf", text = it)
                PrintDocumentButton(label = "Imprimir", jobName = "Orcamento", text = it)
            }
            viewModel.messageText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                ShareTextButton(label = "Compartilhar mensagem", text = it)
                WhatsAppTextButton(phone = viewModel.messagePhone, text = it)
            }
            viewModel.historyText?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }

        if (uiState.quotes.isEmpty()) {
            item {
                Text(
                    text = "Nenhum orcamento cadastrado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.quotes, key = { it.id }) { quote ->
                QuoteRow(
                    quote = quote,
                    onConvert = { viewModel.convertToWorkOrder(quote.id) },
                    onStatusSelected = { viewModel.updateQuoteStatus(quote.id, it) },
                    onShowDocument = { viewModel.showDocument(quote.id) },
                    onShowMessage = { viewModel.showMessage(quote) },
                    onShowHistory = { viewModel.showHistory(quote.id) },
                    onEdit = { viewModel.editQuote(quote.id) },
                )
            }
        }
    }
}

@Composable
private fun QuoteForm(
    form: QuoteFormState,
    customers: List<CustomerEntity>,
    services: List<ServiceProductEntity>,
    onCustomerSelected: (Long) -> Unit,
    onServiceSelected: (ServiceProductEntity) -> Unit,
    onStatusSelected: (QuoteStatus) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (form.editingId == null) "Novo orcamento" else "Editar orcamento",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text("Cliente", style = MaterialTheme.typography.titleSmall)
        CustomerSearchSelector(
            customers = customers,
            selectedCustomerId = form.selectedCustomerId,
            onCustomerSelected = onCustomerSelected,
            emptyText = "Cadastre um cliente antes de criar orcamentos.",
        )

        Text("Status", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuoteStatus.entries.forEach { status ->
                SelectionButton(
                    label = status.label,
                    selected = form.status == status,
                    onClick = { onStatusSelected(status) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text("Itens", style = MaterialTheme.typography.titleSmall)
        ServiceProductSearchSelector(
            services = services,
            selectedServiceProductId = form.selectedServiceProductId,
            onServiceSelected = onServiceSelected,
            emptyText = "Cadastre servicos/produtos antes de adicionar itens.",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = form.quantity,
                onValueChange = onQuantityChanged,
                label = { Text("Qtd") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.unitPrice,
                onValueChange = onUnitPriceChanged,
                label = { Text("Valor") },
                singleLine = true,
                modifier = Modifier.weight(2f),
            )
        }

        OutlinedButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
            Text("Adicionar item")
        }

        form.items.forEachIndexed { index, item ->
            DraftItemRow(index = index, item = item, onRemoveItem = onRemoveItem)
        }

        Text(
            text = "Total: ${formatCurrency(form.items.sumOf { it.subtotal })}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = form.notes,
            onValueChange = onNotesChanged,
            label = { Text("Observacoes") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = form.message.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            if (form.editingId != null) {
                OutlinedButton(onClick = onCancelEdit) {
                    Text("Cancelar")
                }
            }
            Button(onClick = onSave) {
                Text(if (form.editingId == null) "Salvar orcamento" else "Atualizar orcamento")
            }
        }
    }
}

@Composable
private fun SelectionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(if (selected) "$label *" else label)
    }
}

@Composable
private fun DraftItemRow(
    index: Int,
    item: QuoteDraftItem,
    onRemoveItem: (Int) -> Unit,
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
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${item.quantity} x ${formatCurrency(item.unitPrice)} = ${formatCurrency(item.subtotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onRemoveItem(index) }) {
                Text("Remover")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuoteRow(
    quote: QuoteSummary,
    onConvert: () -> Unit,
    onStatusSelected: (QuoteStatus) -> Unit,
    onShowDocument: () -> Unit,
    onShowMessage: () -> Unit,
    onShowHistory: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(quote.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(quote.customerName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${quote.status} - ${quote.itemCount} item(ns) - ${formatCurrency(quote.totalValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDate(quote.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (quote.status != QuoteStatus.Converted.label) {
                    TextButton(onClick = onEdit) {
                        Text("Editar")
                    }
                }
                TextButton(onClick = onShowDocument) {
                    Text("Documento")
                }
                TextButton(onClick = onShowMessage) {
                    Text("Mensagem")
                }
                TextButton(onClick = onShowHistory) {
                    Text("Historico")
                }
                QuoteStatus.entries
                    .filter { it != QuoteStatus.Converted }
                    .forEach { status ->
                        TextButton(onClick = { onStatusSelected(status) }) {
                            Text(status.label)
                        }
                    }
            }
            if (quote.status == QuoteStatus.Approved.label) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onConvert) {
                        Text("Converter em OS")
                    }
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))
