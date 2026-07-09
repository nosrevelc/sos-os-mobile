package br.com.sos.osmobile.feature.workorders

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
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
fun WorkOrderScreen(
    viewModel: WorkOrderViewModel,
    initialEditId: Long? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState

    LaunchedEffect(initialEditId) {
        initialEditId?.let { viewModel.editWorkOrder(it) }
    }

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
                text = "Documento e mensagens",
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
                PrintDocumentButton(label = "Imprimir", jobName = "Ordem de Servico", text = it)
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
                Text("Historico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun WorkOrderListScreen(
    viewModel: WorkOrderViewModel,
    onEdit: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var visibleCount by remember { mutableStateOf(20) }
    val visibleItems = uiState.workOrders.take(visibleCount)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Lista de OS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Mais recentes primeiro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.workOrders.isEmpty()) {
            item {
                Text("Nenhuma OS cadastrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(visibleItems, key = { it.id }) { workOrder ->
                WorkOrderListRow(workOrder = workOrder, onClick = { onEdit(workOrder.id) })
            }
            if (visibleCount < uiState.workOrders.size) {
                item {
                    OutlinedButton(
                        onClick = { visibleCount += 20 },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ver mais")
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkOrderListRow(
    workOrder: WorkOrderSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(workOrder.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${workOrder.number} - ${workOrder.status}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${formatCurrency(workOrder.totalValue)} - ${formatDate(workOrder.openedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
        CustomerSearchSelector(
            customers = customers,
            selectedCustomerId = form.selectedCustomerId,
            onCustomerSelected = onCustomerSelected,
            emptyText = "Cadastre um cliente antes de criar OS.",
        )

        Text("Status", style = MaterialTheme.typography.titleSmall)
        WorkOrderStatusSelector(status = form.status, onStatusSelected = onStatusSelected)

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
                Text(if (form.editingId == null) "Salvar OS" else "Atualizar OS")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkOrderStatusSelector(
    status: WorkOrderStatus,
    onStatusSelected: (WorkOrderStatus) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        WorkOrderStatus.entries.forEach { option ->
            val selected = option == status
            Button(
                onClick = {
                    onStatusSelected(option)
                },
                colors = if (selected) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text(option.label)
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

@OptIn(ExperimentalLayoutApi::class)
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
