package br.com.sos.osmobile.feature.workorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalTextBlock
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import kotlinx.coroutines.launch

class WorkOrderPrintController(
    private val session: WorkOrderSessionState,
    private val workOrderRepository: WorkOrderRepository,
    private val uiStateProvider: () -> WorkOrderUiState,
) {

    fun showDocument(workOrderId: Long) {
        showDocumentThen(workOrderId)
    }

    fun showDocumentThen(workOrderId: Long, onLoaded: ((String) -> Unit)? = null) {
        session.scope?.launch {
            val text = workOrderRepository.generateDocumentText(workOrderId) ?: "Documento nao encontrado."
            session.documentText = text
            onLoaded?.invoke(text)
        }
    }

    fun showThermalDocumentThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) {
        session.scope?.launch {
            val settings = uiStateProvider()
            val content = workOrderRepository.generateThermalPrintContent(
                id = workOrderId,
                headerTemplate = settings.printWorkOrderHeader,
                footerTemplate = settings.printWorkOrderFooter,
                companyName = settings.companyName,
            ) ?: ThermalPrintContent(body = "Documento nao encontrado.")
            session.documentText = content.asText()
            onLoaded(content)
        }
    }

    fun showShelfLabelThen(workOrderId: Long, onLoaded: (List<ThermalTextBlock>) -> Unit) {
        session.scope?.launch {
            val blocks = workOrderRepository.generateShelfLabelBlocks(workOrderId)
                ?: listOf(ThermalTextBlock(text = "Etiqueta nao encontrada."))
            session.documentText = blocks.joinToString(separator = "\n") { it.text }
            onLoaded(blocks)
        }
    }

    fun showReceiptThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) {
        session.scope?.launch {
            val settings = uiStateProvider()
            val content = workOrderRepository.generateReceiptPrintContent(
                id = workOrderId,
                headerTemplate = settings.printWorkOrderHeader,
                footerTemplate = settings.printWorkOrderFooter,
                companyName = settings.companyName,
            ) ?: ThermalPrintContent(body = "Recibo nao encontrado.")
            session.documentText = content.asText()
            onLoaded(content)
        }
    }

    fun showWarrantyThen(workOrderId: Long, onLoaded: (ThermalPrintContent) -> Unit) {
        session.scope?.launch {
            val settings = uiStateProvider()
            val content = workOrderRepository.generateWarrantyPrintContent(
                id = workOrderId,
                headerTemplate = settings.printWorkOrderHeader,
                footerTemplate = settings.printWorkOrderFooter,
                companyName = settings.companyName,
                warrantyDays = session.warranty?.warrantyDays,
                warrantyTerms = session.warranty?.termos,
            ) ?: ThermalPrintContent(body = "Garantia nao encontrada.")
            session.documentText = content.asText()
            onLoaded(content)
        }
    }
}
