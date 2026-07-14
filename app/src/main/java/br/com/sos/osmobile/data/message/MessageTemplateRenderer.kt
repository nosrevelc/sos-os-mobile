package br.com.sos.osmobile.data.message

import br.com.sos.osmobile.data.local.model.DocumentItem
import java.text.NumberFormat
import java.util.Locale

object MessageTemplateRenderer {
    fun render(template: String, tokens: Map<String, String>): String =
        tokens.entries.fold(template) { text, (key, value) ->
            text.replace("{$key}", value)
        }

    fun itemTokens(items: List<DocumentItem>): Map<String, String> {
        val formatted = items.joinToString("\n") { item ->
            "- ${item.name}: ${quantity(item.quantity)} x ${money(item.unitPrice)} = ${money(item.subtotal)}"
        }
        val totalItems = items.sumOf { it.subtotal }
        return mapOf(
            "itens" to formatted,
            "servicos" to formatted,
            "produtos" to formatted,
            "qtd_itens" to quantity(items.sumOf { it.quantity }),
            "total_itens" to money(totalItems),
        )
    }

    val quoteDefaultTemplate = "Ola {nome}, seu orcamento {orcamento} esta {status} no valor de {valor}."

    val workOrderDefaultTemplate = "Ola {nome}, sua OS {os} esta {status}. Total: {valor}. Pago: {valor_pago}. Saldo: {saldo}."

    val workOrderOpenTemplate = "Ola {nome}, sua OS {os} foi aberta. Total: {valor}. Pago: {valor_pago}. Saldo: {saldo}."
    val workOrderInProgressTemplate = "Ola {nome}, sua OS {os} esta em andamento."
    val workOrderCompletedTemplate = "Ola {nome}, sua OS {os} foi concluida. Total: {valor}. Pago: {valor_pago}. Saldo: {saldo}."
    val workOrderCanceledTemplate = "Ola {nome}, sua OS {os} foi cancelada."
    val reviewRequestTemplate = "Ola {nome}, voce poderia avaliar nosso atendimento? Sua opiniao e muito importante para nos."
    val pickupReminderTemplate = "Ola {nome}, sua OS {os} esta pronta ha {dias} dia(s). Por favor, venha retirar."

    private fun money(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

    private fun quantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(Locale("pt", "BR"), value)
}
