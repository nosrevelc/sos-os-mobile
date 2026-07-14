package br.com.sos.osmobile.feature.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.entity.StockMovementType
import br.com.sos.osmobile.ui.input.InputMasks
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ServiceProductScreen(viewModel: ServiceProductViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState
    val stockForm = viewModel.stockFormState

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ServiceProductForm(
                form = form,
                onCodeChanged = viewModel::onCodeChanged,
                onNameChanged = viewModel::onNameChanged,
                onTypeChanged = viewModel::onTypeChanged,
                onCategoryChanged = viewModel::onCategoryChanged,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onUnitPriceChanged = viewModel::onUnitPriceChanged,
                onMinimumStockChanged = viewModel::onMinimumStockChanged,
                onNcmChanged = viewModel::onNcmChanged,
                onCfopChanged = viewModel::onCfopChanged,
                onUnitChanged = viewModel::onUnitChanged,
                onCstCsosnChanged = viewModel::onCstCsosnChanged,
                onSubmit = viewModel::save,
                onCancel = viewModel::cancelEditing,
            )
        }

        if (stockForm.serviceProductId != null || stockForm.message != null) {
            item {
                StockMovementForm(
                    form = stockForm,
                    onQuantityChanged = viewModel::onStockQuantityChanged,
                    onReasonChanged = viewModel::onStockReasonChanged,
                    onSave = viewModel::saveStockMovement,
                    onCancel = viewModel::cancelStockMovement,
                )
            }
        }

        item {
            OutlinedTextField(
                value = viewModel.query,
                onValueChange = viewModel::onQueryChanged,
                label = { Text("Buscar por codigo, nome, categoria ou descricao") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item {
            Text(
                text = "Servicos e produtos ativos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (uiState.items.isEmpty()) {
            item {
                Text(
                    text = "Nenhum servico/produto encontrado.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(uiState.items, key = { it.item.id }) { item ->
                ServiceProductRow(
                    item = item,
                    onEdit = { viewModel.startEditing(item.item) },
                    onArchive = { viewModel.archive(item.item.id) },
                    onStockAction = { type -> viewModel.startStockMovement(item.item, type) },
                )
            }
        }
    }
}

@Composable
private fun ServiceProductForm(
    form: ServiceProductFormState,
    onCodeChanged: (String) -> Unit,
    onNameChanged: (String) -> Unit,
    onTypeChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
    onMinimumStockChanged: (String) -> Unit,
    onNcmChanged: (String) -> Unit,
    onCfopChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onCstCsosnChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    var unitPriceField by remember { mutableStateOf(TextFieldValue(form.unitPrice, TextRange(form.unitPrice.length))) }

    LaunchedEffect(form.unitPrice) {
        if (unitPriceField.text != form.unitPrice) {
            unitPriceField = TextFieldValue(form.unitPrice, TextRange(form.unitPrice.length))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (form.editingId == null) "Novo servico/produto" else "Editar servico/produto",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
            value = form.code,
            onValueChange = {},
            label = { Text("Codigo automatico") },
            singleLine = true,
            enabled = form.editingId != null,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.name,
            onValueChange = onNameChanged,
            label = { Text("Nome") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ServiceProductTypeSelector(
            selected = form.type,
            onSelected = onTypeChanged,
        )
        OutlinedTextField(
            value = form.category,
            onValueChange = onCategoryChanged,
            label = { Text("Categoria") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = unitPriceField,
            onValueChange = {
                val masked = InputMasks.currency(it.text)
                unitPriceField = TextFieldValue(masked, TextRange(masked.length))
                onUnitPriceChanged(masked)
            },
            label = { Text("Valor padrao") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = form.description,
            onValueChange = onDescriptionChanged,
            label = { Text("Descricao") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        if (form.type != ServiceProductType.SERVICE) {
            OutlinedTextField(
                value = form.minimumStock,
                onValueChange = onMinimumStockChanged,
                label = { Text("Estoque minimo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Fiscal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = form.ncm,
                    onValueChange = onNcmChanged,
                    label = { Text("NCM") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.cfop,
                    onValueChange = onCfopChanged,
                    label = { Text("CFOP") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = form.unit,
                    onValueChange = onUnitChanged,
                    label = { Text("Unidade") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.cstCsosn,
                    onValueChange = onCstCsosnChanged,
                    label = { Text("CST/CSOSN") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        form.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (form.editingId != null) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancelar")
                }
            }
            Button(onClick = onSubmit) {
                Text(if (form.editingId == null) "Cadastrar" else "Salvar")
            }
        }
    }
}

@Composable
private fun ServiceProductRow(
    item: ServiceProductStockItem,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
    onStockAction: (String) -> Unit,
) {
    val service = item.item
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(service.nome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text("${service.codigo} - ${service.tipo} - ${formatCurrency(service.unitPrice)}", style = MaterialTheme.typography.bodyMedium)
            if (service.tipo != ServiceProductType.SERVICE) {
                val lowStock = service.minimumStock > 0.0 && item.stock <= service.minimumStock
                Text(
                    "Saldo: ${formatQuantity(item.stock)} | Minimo: ${formatQuantity(service.minimumStock)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (lowStock) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    "Fiscal: NCM ${service.ncm ?: "-"} | CFOP ${service.cfop ?: "-"} | ${service.unidade ?: "UN"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            service.categoria?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            service.descricao?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (service.tipo != ServiceProductType.SERVICE) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onStockAction(StockMovementType.IN) }, modifier = Modifier.weight(1f)) {
                        Text("Entrada")
                    }
                    OutlinedButton(onClick = { onStockAction(StockMovementType.OUT) }, modifier = Modifier.weight(1f)) {
                        Text("Saida")
                    }
                    OutlinedButton(onClick = { onStockAction(StockMovementType.ADJUST) }, modifier = Modifier.weight(1f)) {
                        Text("Ajuste")
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Text("Editar")
                }
                TextButton(onClick = onArchive) {
                    Text("Arquivar")
                }
            }
        }
    }
}

@Composable
private fun StockMovementForm(
    form: StockMovementFormState,
    onQuantityChanged: (String) -> Unit,
    onReasonChanged: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("${form.type}: ${form.serviceProductName}", fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = form.quantity,
                onValueChange = onQuantityChanged,
                label = { Text(if (form.type == StockMovementType.ADJUST) "Quantidade (+ ou -)" else "Quantidade") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.reason,
                onValueChange = onReasonChanged,
                label = { Text("Motivo/observacao") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            form.message?.let {
                Text(it, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel) {
                    Text("Cancelar")
                }
                Button(onClick = onSave, enabled = form.serviceProductId != null) {
                    Text("Registrar")
                }
            }
        }
    }
}

@Composable
private fun ServiceProductTypeSelector(
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Tipo: $selected")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ServiceProductType.all.forEach { type ->
                DropdownMenuItem(
                    text = { Text(if (type == selected) "$type *" else type) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale("pt", "BR"), "%.2f", value)
