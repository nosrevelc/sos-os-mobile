package br.com.sos.osmobile.feature.backup

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.backup.BackupRepository
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

    companion object {
        fun factory(repository: BackupRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    BackupViewModel(repository) as T
            }
    }
}
