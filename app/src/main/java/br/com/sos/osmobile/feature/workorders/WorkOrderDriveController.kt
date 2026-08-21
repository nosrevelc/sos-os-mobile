package br.com.sos.osmobile.feature.workorders

import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import kotlinx.coroutines.launch

class WorkOrderDriveController(
    private val session: WorkOrderSessionState,
    private val driveSyncRepository: DriveSyncRepository,
    private val settingsRepository: SettingsRepository,
    private val reloadPhotos: suspend (Long) -> Unit,
    private val reloadSignature: suspend (Long) -> Unit,
    private val refreshStatus: suspend (Long) -> Unit,
) {

    fun smartSyncDriveNow() {
        val workOrderId = session.formState.editingId ?: run {
            session.formState = session.formState.copy(message = "Salve a OS antes de sincronizar.")
            return
        }
        session.scope?.launch {
            session.formState = session.formState.copy(message = "Sincronizando Drive...")
            val result = driveSyncRepository.smartSyncWorkOrder(workOrderId)
            reloadPhotos(workOrderId)
            reloadSignature(workOrderId)
            refreshStatus(workOrderId)
            session.formState = session.formState.copy(
                message = result.fold(
                    onSuccess = {
                        val baseMessage = if (it.rebuilt) "Drive reconstruido e sincronizado." else "Drive sincronizado."
                        listOfNotNull(baseMessage, it.warning).joinToString(" ")
                    },
                    onFailure = { "Falha ao sincronizar Drive: ${it.message.orEmpty()}" },
                ),
            )
        }
    }

    fun importDesignFromDriveNow() {
        val workOrderId = session.formState.editingId ?: run {
            session.formState = session.formState.copy(message = "Salve a OS antes de importar arquivos do Drive.")
            return
        }
        session.scope?.launch {
            session.formState = session.formState.copy(message = "Buscando arquivos na pasta Design do Drive...")
            val result = driveSyncRepository.importDesignFiles(workOrderId)
            reloadPhotos(workOrderId)
            refreshStatus(workOrderId)
            session.formState = session.formState.copy(
                message = result.fold(
                    onSuccess = {
                        if (it.foundFiles == 0) {
                            "Pasta Design pronta no Drive. Coloque os arquivos nela e toque em importar novamente."
                        } else {
                            "Design importado: ${it.importedFiles} novo(s), ${it.alreadyImportedFiles} ja existente(s)."
                        }
                    },
                    onFailure = { "Falha ao importar Design do Drive: ${it.message.orEmpty()}" },
                ),
            )
        }
    }

    fun importSelectedDesignFromDriveNow(selectedUris: Set<String>, doNotAlertAgain: Boolean) {
        val workOrderId = session.formState.editingId ?: return
        session.scope?.launch {
            if (doNotAlertAgain) disableDesignImportAlert(workOrderId)
            session.pendingDesignImportCandidates = emptyList()
            if (selectedUris.isEmpty()) {
                session.formState = session.formState.copy(message = "Nenhum arquivo Design selecionado.")
                return@launch
            }
            session.formState = session.formState.copy(message = "Importando arquivos selecionados do Drive...")
            val result = driveSyncRepository.importDesignFiles(workOrderId, selectedUris)
            reloadPhotos(workOrderId)
            refreshStatus(workOrderId)
            session.formState = session.formState.copy(
                message = result.fold(
                    onSuccess = { "Design importado: ${it.importedFiles} novo(s), ${it.alreadyImportedFiles} ja existente(s)." },
                    onFailure = { "Falha ao importar Design do Drive: ${it.message.orEmpty()}" },
                ),
            )
        }
    }

    fun dismissDesignImportPrompt(doNotAlertAgain: Boolean) {
        val workOrderId = session.formState.editingId
        session.scope?.launch {
            if (doNotAlertAgain && workOrderId != null) disableDesignImportAlert(workOrderId)
            session.pendingDesignImportCandidates = emptyList()
        }
    }

    fun buildDriveDebugReport() {
        val workOrderId = session.formState.editingId ?: run {
            session.formState = session.formState.copy(message = "Salve a OS antes de gerar debug do Drive.")
            return
        }
        session.scope?.launch {
            session.formState = session.formState.copy(message = "Gerando debug do Drive...")
            session.driveDebugReport = driveSyncRepository.buildWorkOrderDebugReport(workOrderId)
            session.formState = session.formState.copy(message = "Debug do Drive gerado. Copie e envie para analise.")
        }
    }

    suspend fun checkDesignImportCandidatesOnOpen(workOrderId: Long) {
        if (settingsRepository.getString(designImportAlertDisabledKey(workOrderId)) == "true") return
        driveSyncRepository.listDesignImportCandidates(workOrderId)
            .onSuccess { candidates ->
                session.pendingDesignImportCandidates = candidates
            }
    }

    private suspend fun disableDesignImportAlert(workOrderId: Long) {
        settingsRepository.set(designImportAlertDisabledKey(workOrderId), "true")
    }

    private fun designImportAlertDisabledKey(workOrderId: Long): String =
        "drive_design_alert_disabled_$workOrderId"
}
