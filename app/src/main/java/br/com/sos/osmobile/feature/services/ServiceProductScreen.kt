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
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.ui.input.InputMasks
import java.text.NumberFormat
import java.util.Locale

@Composable
fun ServiceProductScreen(viewModel: ServiceProductViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState

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
                onCategoryChanged = viewModel::onCategoryChanged,
                onDescriptionChanged = viewModel::onDescriptionChanged,
                onUnitPriceChanged = viewModel::onUnitPriceChanged,
                onSubmit = viewModel::save,
                onCancel = viewModel::cancelEditing,
            )
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
            items(uiState.items, key = { it.id }) { item ->
                ServiceProductRow(
                    item = item,
                    onEdit = { viewModel.startEditing(item) },
                    onArchive = { viewModel.archive(item.id) },
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
    onCategoryChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
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
    item: ServiceProductEntity,
    onEdit: () -> Unit,
    onArchive: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(item.nome, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text("${item.codigo} - ${formatCurrency(item.unitPrice)}", style = MaterialTheme.typography.bodyMedium)
            item.categoria?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item.descricao?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
