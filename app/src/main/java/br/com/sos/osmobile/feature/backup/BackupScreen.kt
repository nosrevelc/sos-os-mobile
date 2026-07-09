package br.com.sos.osmobile.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.ui.components.ShareFileButton
import br.com.sos.osmobile.ui.components.ShareTextButton

@Composable
fun BackupScreen(viewModel: BackupViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = viewModel::exportJson, modifier = Modifier.fillMaxWidth()) {
            Text("Gerar backup JSON")
        }
        if (viewModel.exportText.isBlank()) {
            Text("Nenhum backup gerado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ShareTextButton(label = "Compartilhar backup", text = viewModel.exportText)
            ShareFileButton(label = "Compartilhar arquivo JSON", fileName = "os-mobile-backup.json", text = viewModel.exportText, mimeType = "application/json")
            Text(viewModel.exportText, style = MaterialTheme.typography.bodySmall)
        }
        Text("Restaurar backup", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = viewModel.importText,
            onValueChange = viewModel::onImportTextChanged,
            label = { Text("Cole aqui o JSON do backup") },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = viewModel::importJson,
            enabled = viewModel.importText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Substituir dados pelo backup")
        }
        viewModel.importMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
