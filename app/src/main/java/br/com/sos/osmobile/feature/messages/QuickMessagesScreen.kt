package br.com.sos.osmobile.feature.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.ui.components.MessageActionButtons

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickMessagesScreen(viewModel: QuickMessagesViewModel) {
    val customer = viewModel.selectedCustomer
    val message = viewModel.renderedText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Cliente", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = viewModel.query,
            onValueChange = viewModel::onQueryChanged,
            label = { Text("Buscar por nome, telefone ou email") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (customer != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${customer.nome} - ${customer.telefone}", modifier = Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::clearCustomer) {
                    Text("Trocar")
                }
            }
        } else {
            viewModel.filteredCustomers.forEach {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectCustomer(it) },
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(it.nome, style = MaterialTheme.typography.titleSmall)
                        Text(it.telefone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Text("Mensagem", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickMessagesViewModel.templates.forEach { template ->
                AssistChip(
                    onClick = { viewModel.selectTemplate(template) },
                    label = { Text(template.label) },
                )
            }
        }
        OutlinedTextField(
            value = message,
            onValueChange = viewModel::onCustomTextChanged,
            label = { Text("Texto") },
            minLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        if (customer == null) {
            Text("Selecione um cliente para enviar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            MessageActionButtons(
                phone = customer.telefone,
                email = customer.email,
                subject = viewModel.selectedTemplate.label,
                text = message,
            )
        }
    }
}
