package br.com.sos.osmobile.data.local.model

data class WorkOrderServiceUsage(
    val serviceId: Long,
    val serviceName: String,
    val quantity: Double,
    val totalValue: Double,
    val openedAt: Long,
)
