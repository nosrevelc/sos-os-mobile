package br.com.sos.osmobile.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.ui.components.ShareFileButton
import br.com.sos.osmobile.ui.components.ShareTextButton

@Composable
fun BackupScreen(viewModel: BackupViewModel) {
    var driveBackupMenuExpanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Backup completo", style = MaterialTheme.typography.titleMedium)
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
        Text("Backup no Google Drive", style = MaterialTheme.typography.titleMedium)
        Text(
            "Use quando o cliente trocar ou perder o telefone: configure a mesma pasta do Drive, busque os backups e restaure o arquivo mais recente.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = viewModel::saveFullBackupToDrive, modifier = Modifier.fillMaxWidth()) {
            Text("Gerar backup completo no Drive")
        }
        OutlinedButton(onClick = viewModel::loadDriveBackups, modifier = Modifier.fillMaxWidth()) {
            Text("Buscar backups no Drive")
        }
        OutlinedButton(
            onClick = { driveBackupMenuExpanded = true },
            enabled = viewModel.driveBackups.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(viewModel.selectedDriveBackup?.name ?: "Nenhum backup selecionado")
        }
        DropdownMenu(expanded = driveBackupMenuExpanded, onDismissRequest = { driveBackupMenuExpanded = false }) {
            viewModel.driveBackups.forEach { backup ->
                DropdownMenuItem(
                    text = { Text(backup.name) },
                    onClick = {
                        viewModel.selectDriveBackup(backup)
                        driveBackupMenuExpanded = false
                    },
                )
            }
        }
        OutlinedTextField(
            value = viewModel.driveRestoreConfirmation,
            onValueChange = viewModel::onDriveRestoreConfirmationChanged,
            label = { Text("Digite RESTAURAR para restaurar do Drive") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = viewModel::restoreSelectedDriveBackup,
            enabled = viewModel.selectedDriveBackup != null && viewModel.driveRestoreConfirmation == "RESTAURAR",
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Restaurar backup selecionado do Drive")
        }
        viewModel.driveMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
        Text("Backup das configuracoes", style = MaterialTheme.typography.titleMedium)
        Button(onClick = viewModel::exportSettingsJson, modifier = Modifier.fillMaxWidth()) {
            Text("Gerar backup das configuracoes")
        }
        if (viewModel.settingsExportText.isBlank()) {
            Text("Nenhum backup de configuracoes gerado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ShareTextButton(label = "Compartilhar configuracoes", text = viewModel.settingsExportText)
            ShareFileButton(
                label = "Compartilhar arquivo de configuracoes",
                fileName = "os-mobile-configuracoes.json",
                text = viewModel.settingsExportText,
                mimeType = "application/json",
            )
            Text(viewModel.settingsExportText, style = MaterialTheme.typography.bodySmall)
        }
        Text("Restaurar backup", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = viewModel.importText,
            onValueChange = viewModel::onImportTextChanged,
            label = { Text("Cole aqui JSON ou CSV com cabecalho") },
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
        Button(
            onClick = viewModel::importSettingsJson,
            enabled = viewModel.importText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Restaurar apenas configuracoes")
        }
        viewModel.importMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
        viewModel.settingsImportMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
        Text("Importar CSV", style = MaterialTheme.typography.titleMedium)
        Text(
            "Use o mesmo campo acima. CSV aceito para clientes, servicos/produtos, configuracoes e templates.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = viewModel::importCustomersCsv,
            enabled = viewModel.importText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Importar clientes CSV")
        }
        Button(
            onClick = viewModel::importServiceProductsCsv,
            enabled = viewModel.importText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Importar servicos/produtos CSV")
        }
        Button(
            onClick = viewModel::importSettingsCsv,
            enabled = viewModel.importText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Importar configuracoes CSV")
        }
        Button(
            onClick = viewModel::importMessageTemplatesCsv,
            enabled = viewModel.importText.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Importar templates de mensagens CSV")
        }
        viewModel.csvImportMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
