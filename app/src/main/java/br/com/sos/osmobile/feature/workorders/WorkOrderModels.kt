package br.com.sos.osmobile.feature.workorders

import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.model.DeliveryStatus
import br.com.sos.osmobile.data.model.DeliveryType
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.print.ThermalPrintStyle

data class WorkOrderDraftItem(
    val serviceProductId: Long,
    val name: String,
    val type: String = ServiceProductType.SERVICE,
    val quantity: Double,
    val unitPrice: Double,
) {
    val subtotal: Double = quantity * unitPrice
}

data class WorkOrderFormState(
    val editingId: Long? = null,
    val editingNumber: String? = null,
    val selectedCustomerId: Long? = null,
    val selectedServiceProductId: Long? = null,
    val status: WorkOrderStatus = WorkOrderStatus.Open,
    val quantity: String = "1",
    val unitPrice: String = "",
    val discount: String = "",
    val deliveryType: String = DeliveryType.PICKUP,
    val deliveryStatus: String = DeliveryStatus.WAITING_PICKUP,
    val deliveryAddress: String = "",
    val deliveryFee: String = "",
    val trackingCode: String = "",
    val deliveryNotes: String = "",
    val notes: String = "",
    val items: List<WorkOrderDraftItem> = emptyList(),
    val originalItems: List<WorkOrderDraftItem> = emptyList(),
    val driveSyncStatus: String = "",
    val driveSyncError: String = "",
    val message: String? = null,
)

data class WorkOrderUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val services: List<ServiceProductEntity> = emptyList(),
    val stockByServiceProductId: Map<Long, Double> = emptyMap(),
    val workOrders: List<WorkOrderSummary> = emptyList(),
    val companyName: String = "",
    val pixName: String = "",
    val pixKey: String = "",
    val quoteMinAcceptanceValue: String = "",
    val workOrderTemplate: String = MessageTemplateRenderer.workOrderDefaultTemplate,
    val workOrderStatusTemplates: Map<String, String> = emptyMap(),
    val reviewRequestTemplate: String = MessageTemplateRenderer.reviewRequestTemplate,
    val pickupReminderTemplate: String = MessageTemplateRenderer.pickupReminderTemplate,
    val paymentPendingTemplate: String = MessageTemplateRenderer.paymentPendingTemplate,
    val paymentConfirmedTemplate: String = MessageTemplateRenderer.paymentConfirmedTemplate,
    val paymentProofRequestTemplate: String = MessageTemplateRenderer.paymentProofRequestTemplate,
    val orderSentTemplate: String = MessageTemplateRenderer.orderSentTemplate,
    val outForDeliveryTemplate: String = MessageTemplateRenderer.outForDeliveryTemplate,
    val deliveredTemplate: String = MessageTemplateRenderer.deliveredTemplate,
    val notDeliveredTemplate: String = MessageTemplateRenderer.notDeliveredTemplate,
    val thankYouTemplate: String = MessageTemplateRenderer.thankYouTemplate,
    val photosEnabled: Boolean = false,
    val signatureEnabled: Boolean = false,
    val checklistEnabled: Boolean = false,
    val warrantyEnabled: Boolean = false,
    val financeEnabled: Boolean = false,
    val printBluetoothAddress: String = "",
    val printWorkOrderAuto: Boolean = false,
    val printWorkOrderCopies: Int = 0,
    val printWorkOrderHeader: String = "{empresa}\nOS {os}\n{data}",
    val printWorkOrderFooter: String = "Obrigado pela preferencia",
    val printWorkOrderStyle: ThermalPrintStyle = ThermalPrintStyle(),
)
