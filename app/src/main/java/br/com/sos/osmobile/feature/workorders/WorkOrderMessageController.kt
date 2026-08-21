package br.com.sos.osmobile.feature.workorders

import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.message.WorkOrderMessageRenderer
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import kotlinx.coroutines.launch

class WorkOrderMessageController(
    private val session: WorkOrderSessionState,
    private val workOrderRepository: WorkOrderRepository,
    private val paymentRepository: WorkOrderPaymentRepository,
    private val uiStateProvider: () -> WorkOrderUiState,
) {

    fun showMessage(workOrder: WorkOrderSummary) {
        session.scope?.launch {
            val paidTotal = paymentRepository.listByWorkOrder(workOrder.id).sumOf { it.valor }
            val entity = workOrderRepository.findById(workOrder.id)
            val discount = entity?.discountValue ?: 0.0
            val itemTokens = MessageTemplateRenderer.itemTokens(workOrderRepository.listDocumentItems(workOrder.id))
            val settings = uiStateProvider()
            session.messagePhone = workOrder.customerPhone
            session.messageText = MessageTemplateRenderer.render(
                template = settings.workOrderStatusTemplates[workOrder.status] ?: settings.workOrderTemplate,
                tokens = WorkOrderMessageRenderer.tokens(
                    customerName = workOrder.customerName,
                    customerPhone = workOrder.customerPhone,
                    customerCpfCnpj = "",
                    workOrderNumber = workOrder.number,
                    status = workOrder.status,
                    totalValue = workOrder.totalValue,
                    discountValue = discount,
                    minAcceptanceValue = settings.quoteMinAcceptanceValue,
                    deliveryType = entity?.deliveryType.orEmpty(),
                    deliveryStatus = entity?.deliveryStatus.orEmpty(),
                    deliveryAddress = entity?.deliveryAddress.orEmpty(),
                    deliveryFee = entity?.deliveryFee ?: 0.0,
                    trackingCode = entity?.trackingCode.orEmpty(),
                    paidTotal = paidTotal,
                    companyName = settings.companyName,
                    pixName = settings.pixName,
                    pixKey = settings.pixKey,
                ) + itemTokens,
            )
        }
    }
}
