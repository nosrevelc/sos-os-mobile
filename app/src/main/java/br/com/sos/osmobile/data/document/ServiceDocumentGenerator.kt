package br.com.sos.osmobile.data.document

import br.com.sos.osmobile.data.local.model.DocumentItem
import java.text.NumberFormat
import java.util.Locale

object ServiceDocumentGenerator {
    fun generate(
        title: String,
        number: String,
        customerName: String,
        status: String,
        totalValue: Double,
        discountValue: Double = 0.0,
        minimumDepositValue: Double = 0.0,
        notes: String?,
        items: List<DocumentItem>,
    ): String = buildString {
        appendLine(title)
        appendLine("Numero: $number")
        appendLine("Cliente: $customerName")
        appendLine("Status: $status")
        appendLine()
        appendLine("Itens")
        items.forEach { item ->
            appendLine("${item.name}")
            appendLine("${item.quantity} x ${money(item.unitPrice)} = ${money(item.subtotal)}")
        }
        appendLine()
        if (discountValue > 0.0) {
            appendLine("Subtotal: ${money(totalValue + discountValue)}")
            appendLine("Desconto: ${money(discountValue)}")
        }
        if (minimumDepositValue > 0.0) {
            appendLine("Sinal minimo: ${money(minimumDepositValue)}")
        }
        appendLine("Total: ${money(totalValue)}")
        notes?.takeIf { it.isNotBlank() }?.let {
            appendLine()
            appendLine("Observacoes: $it")
        }
    }

    private fun money(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}
