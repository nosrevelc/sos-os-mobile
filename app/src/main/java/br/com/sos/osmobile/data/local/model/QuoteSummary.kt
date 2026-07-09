package br.com.sos.osmobile.data.local.model

data class QuoteSummary(
    val id: Long,
    val number: String,
    val customerName: String,
    val customerPhone: String,
    val status: String,
    val totalValue: Double,
    val itemCount: Int,
    val createdAt: Long,
)
