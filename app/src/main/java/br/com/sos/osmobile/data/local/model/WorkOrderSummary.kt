package br.com.sos.osmobile.data.local.model

data class WorkOrderSummary(
    val id: Long,
    val number: String,
    val customerName: String,
    val customerPhone: String,
    val status: String,
    val totalValue: Double,
    val itemCount: Int,
    val openedAt: Long,
    val concludedAt: Long? = null,
)
