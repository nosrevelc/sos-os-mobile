package br.com.sos.osmobile.data.local.model

data class AppointmentSummary(
    val id: Long,
    val customerId: Long,
    val customerName: String,
    val customerPhone: String,
    val customerEmail: String?,
    val workOrderId: Long?,
    val workOrderNumber: String?,
    val title: String,
    val type: String,
    val startsAt: Long,
    val endsAt: Long,
    val status: String,
    val notes: String?,
    val calendarSyncStatus: String,
)
