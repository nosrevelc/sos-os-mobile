package br.com.sos.osmobile.data.local.model

data class SaleSummary(
    val id: Long,
    val number: String,
    val customerName: String,
    val totalValue: Double,
    val paidValue: Double,
    val paymentMethod: String,
    val fiscalStatus: String,
    val createdAt: Long,
)
