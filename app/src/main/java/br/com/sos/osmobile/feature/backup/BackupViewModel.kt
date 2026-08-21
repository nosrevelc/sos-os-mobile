package br.com.sos.osmobile.feature.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.backup.BackupRepository
import br.com.sos.osmobile.data.backup.DriveBackupFile
import kotlinx.coroutines.launch

class BackupViewModel(
    private val backupRepository: BackupRepository,
) : ViewModel() {
    var exportText by mutableStateOf("")
        private set

    var settingsExportText by mutableStateOf("")
        private set

    var importText by mutableStateOf("")
        private set

    var importMessage by mutableStateOf<String?>(null)
        private set

    var settingsImportMessage by mutableStateOf<String?>(null)
        private set

    var csvImportMessage by mutableStateOf<String?>(null)
        private set

    var driveBackups by mutableStateOf(emptyList<DriveBackupFile>())
        private set

    var selectedDriveBackup by mutableStateOf<DriveBackupFile?>(null)
        private set

    var driveMessage by mutableStateOf<String?>(null)
        private set

    var driveRestoreConfirmation by mutableStateOf("")
        private set

    fun exportJson() {
        viewModelScope.launch {
            exportText = backupRepository.exportJson()
        }
    }

    fun exportSettingsJson() {
        viewModelScope.launch {
            settingsExportText = backupRepository.exportSettingsJson()
        }
    }

    fun onImportTextChanged(value: String) {
        importText = value
        importMessage = null
        settingsImportMessage = null
        csvImportMessage = null
    }

    fun importJson() {
        viewModelScope.launch {
            importMessage = runCatching {
                backupRepository.importJson(importText)
            }.fold(
                onSuccess = {
                    "Backup restaurado: ${it.customers} cliente(s), ${it.services} servico(s), ${it.quotes} orcamento(s), ${it.workOrders} OS."
                },
                onFailure = {
                    "Nao foi possivel restaurar o backup: ${it.message ?: "JSON invalido"}"
                },
            )
        }
    }

    fun saveFullBackupToDrive() {
        viewModelScope.launch {
            driveMessage = "Gerando e enviando backup para o Drive..."
            runCatching { backupRepository.saveFullBackupToDrive() }.fold(
                onSuccess = {
                    runCatching { backupRepository.listDriveBackups() }.onSuccess { backups ->
                        driveBackups = backups
                        selectedDriveBackup = backups.firstOrNull { file -> file.name == it.name } ?: backups.firstOrNull()
                    }
                    driveMessage = "Backup salvo no Drive: ${it.name} (${formatBytes(it.sizeBytes)})."
                },
                onFailure = {
                    driveMessage = "Falha ao salvar backup no Drive: ${it.message ?: "verifique a pasta configurada"}"
                },
            )
        }
    }

    fun loadDriveBackups() {
        viewModelScope.launch {
            driveMessage = "Buscando backups no Drive..."
            runCatching { backupRepository.listDriveBackups() }.fold(
                onSuccess = {
                    driveBackups = it
                    selectedDriveBackup = selectedDriveBackup?.let { selected -> it.firstOrNull { file -> file.uri == selected.uri } }
                        ?: it.firstOrNull()
                    driveMessage = if (it.isEmpty()) {
                        "Nenhum backup encontrado na pasta Backups."
                    } else {
                        "${it.size} backup(s) encontrado(s). Selecione um para restaurar."
                    }
                },
                onFailure = {
                    driveMessage = "Falha ao buscar backups no Drive: ${it.message ?: "verifique a pasta configurada"}"
                },
            )
        }
    }

    fun selectDriveBackup(file: DriveBackupFile) {
        selectedDriveBackup = file
        driveRestoreConfirmation = ""
        driveMessage = "Backup selecionado: ${file.name}"
    }

    fun onDriveRestoreConfirmationChanged(value: String) {
        driveRestoreConfirmation = value.uppercase().take(9)
    }

    fun restoreSelectedDriveBackup() {
        val backup = selectedDriveBackup ?: run {
            driveMessage = "Selecione um backup do Drive."
            return
        }
        if (driveRestoreConfirmation != "RESTAURAR") {
            driveMessage = "Digite RESTAURAR para confirmar."
            return
        }
        viewModelScope.launch {
            driveMessage = "Restaurando backup do Drive..."
            runCatching { backupRepository.restoreDriveBackup(backup.uri) }.fold(
                onSuccess = {
                    driveRestoreConfirmation = ""
                    driveMessage = "Backup restaurado do Drive: ${it.customers} cliente(s), ${it.services} servico(s), ${it.quotes} orcamento(s), ${it.workOrders} OS."
                },
                onFailure = {
                    driveMessage = "Falha ao restaurar backup do Drive: ${it.message ?: "backup invalido"}"
                },
            )
        }
    }

    fun importSettingsJson() {
        viewModelScope.launch {
            settingsImportMessage = runCatching {
                backupRepository.importSettingsJson(importText)
            }.fold(
                onSuccess = {
                    "Configuracoes restauradas com sucesso: ${it.settings} item(ns)."
                },
                onFailure = {
                    "Nao foi possivel restaurar as configuracoes: ${it.message ?: "JSON invalido"}"
                },
            )
        }
    }

    fun importCustomersCsv() {
        importCsv("clientes") { backupRepository.importCustomersCsv(importText) }
    }

    fun importServiceProductsCsv() {
        importCsv("servicos/produtos") { backupRepository.importServiceProductsCsv(importText) }
    }

    fun importSettingsCsv() {
        importCsv("configuracoes") { backupRepository.importSettingsCsv(importText) }
    }

    fun importMessageTemplatesCsv() {
        importCsv("templates de mensagens") { backupRepository.importMessageTemplatesCsv(importText) }
    }

    private fun importCsv(
        label: String,
        action: suspend () -> br.com.sos.osmobile.data.backup.CsvImportResult,
    ) {
        viewModelScope.launch {
            csvImportMessage = runCatching { action() }.fold(
                onSuccess = {
                    "CSV de $label importado: ${it.imported} registro(s), ${it.ignored} ignorado(s)."
                },
                onFailure = {
                    "Falha ao importar CSV de $label: ${it.message ?: "CSV invalido"}"
                },
            )
        }
    }

    private fun formatBytes(bytes: Long): String =
        when {
            bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
            bytes >= 1024L -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }

    companion object {
        fun factory(repository: BackupRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BackupViewModel(repository) as T
            }
    }
}
