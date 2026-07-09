package br.com.sos.osmobile.feature.workorders

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
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.ui.components.ShareFileButton
import br.com.sos.osmobile.ui.components.SharePdfButton
import br.com.sos.osmobile.ui.components.ShareTextButton
import br.com.sos.osmobile.ui.components.WhatsAppTextButton
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@Composable
fun WorkOrderScreen(viewModel: WorkOrderViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WorkOrderForm(
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
                onSave = viewModel::saveWorkOrder,
                onCancelEdit = viewModel::cancelEdit,
            )
        }

        item {
            Text(
                text = "Ordens de servico",
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
                ShareFileButton(label = "Compartilhar arquivo", fileName = "ordem_servico.txt", text = it)
                SharePdfButton(label = "Compartilhar PDF", fileName = "ordem_servico.pdf", text = it)
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

        if (uiState.workOrders.isEmpty()) {
            item {
                Text(
                    text = "Nenhuma OS cadastrada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.workOrders, key = { it.id }) { workOrder ->
                WorkOrderRow(
                    workOrder = workOrder,
                    onStatusSelected = { viewModel.updateWorkOrderStatus(workOrder.id, it) },
                    onShowDocument = { viewModel.showDocument(workOrder.id) },
                    onShowMessage = { viewModel.showMessage(workOrder) },
                    onShowHistory = { viewModel.showHistory(workOrder.id) },
                    onEdit = { viewModel.editWorkOrder(workOrder.id) },
                )
            }
        }
    }
}

@Composable
private fun WorkOrderForm(
    form: WorkOrderFormState,
    customers: List<CustomerEntity>,
    services: List<ServiceProductEntity>,
    onCustomerSelected: (Long) -> Unit,
    onServiceSelected: (ServiceProductEntity) -> Unit,
    onStatusSelected: (WorkOrderStatus) -> Unit,
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
            text = if (form.editingId == null) "Nova OS" else "Editar OS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text("Cliente", style = MaterialTheme.typography.titleSmall)
        if (customers.isEmpty()) {
            Text("Cadastre um cliente antes de criar OS.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            var customerExpanded by remember { mutableStateOf(false) }
            val selectedCustomer = customers.firstOrNull { it.id == form.selectedCustomerId }
            OutlinedButton(onClick = { customerExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selectedCustomer?.let { "${it.nome} - ${it.telefone}" } ?: "Selecionar cliente")
            }
            DropdownMenu(expanded = customerExpanded, onDismissRequest = { customerExpanded = false }) {
                customers.forEach { customer ->
                    DropdownMenuItem(
                        text = { Text("${customer.nome} - ${customer.telefone}") },
                        onClick = {
                            onCustomerSelected(customer.id)
                            customerExpanded = false
                        },
                    )
                }
            }
        }

        Text("Status", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WorkOrderStatus.entries.forEach { status ->
                SelectionButton(
                    label = status.label,
                    selected = form.status == status,
                    onClick = { onStatusSelected(status) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Text("Itens", style = MaterialTheme.typography.titleSmall)
        if (services.isEmpty()) {
            Text("Cadastre servicos/produtos antes de adicionar itens.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            var expanded by remember { mutableStateOf(false) }
            val selected = services.firstOrNull { it.id == form.selectedServiceProductId }
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.let { "${it.codigo} - ${it.nome}" } ?: "Selecionar servico/produto")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                services.forEach { service ->
                    DropdownMenuItem(
                        text = { Text("${service.codigo} - ${service.nome}") },
                        onClick = {
                            onServiceSelected(service)
                            expanded = false
                        },
                    )
                }
            }
        }

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
                Text(if (form.editingId == null) "Salvar OS" else "Atualizar OS")
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
    item: WorkOrderDraftItem,
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

@Composable
private fun WorkOrderRow(
    workOrder: WorkOrderSummary,
    onStatusSelected: (WorkOrderStatus) -> Unit,
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
            Text(workOrder.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(workOrder.customerName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${workOrder.status} - ${workOrder.itemCount} item(ns) - ${formatCurrency(workOrder.totalValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDate(workOrder.openedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (workOrder.status != WorkOrderStatus.Completed.label && workOrder.status != WorkOrderStatus.Canceled.label) {
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
                WorkOrderStatus.entries.forEach { status ->
                    TextButton(onClick = { onStatusSelected(status) }) {
                        Text(status.label)
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
