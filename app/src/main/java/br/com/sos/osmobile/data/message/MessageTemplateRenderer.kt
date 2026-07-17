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
    val paymentPendingTemplate = "Ola {nome}, sua OS {os} possui saldo pendente de {saldo}. Total: {valor}. Pago: {valor_pago}."
    val paymentConfirmedTemplate = "Ola {nome}, confirmamos o pagamento da OS {os}. Status: {status_pagamento}. Obrigado."
    val paymentProofRequestTemplate = "Ola {nome}, por favor envie o comprovante de pagamento da OS {os}. Saldo: {saldo}."
    val orderSentTemplate = "Ola {nome}, seu pedido da OS {os} foi enviado. Rastreio: {codigo_rastreio}."
    val outForDeliveryTemplate = "Ola {nome}, seu pedido da OS {os} saiu para entrega. Endereco: {endereco_entrega}."
    val deliveredTemplate = "Ola {nome}, sua OS {os} consta como entregue. Obrigado pela preferencia."
    val notDeliveredTemplate = "Ola {nome}, nao conseguimos concluir a entrega da OS {os}. Entraremos em contato para combinar nova tentativa."
    val thankYouTemplate = "Ola {nome}, agradecemos pela preferencia. Foi um prazer atender voce."
    val announcementTemplate = "Ola {nome}, temos um comunicado: {empresa} informa que estamos a disposicao."
    val welcomeTemplate = "Ola {nome}, seja bem-vindo(a) a {empresa}. Estamos felizes em atender voce."
    val quoteExpiredTemplate = "Ola {nome}, seu orcamento {orcamento} expirou. Fale conosco se desejar atualizar os valores."
    val quoteReminderTemplate = "Ola {nome}, passando para lembrar do orcamento {orcamento} no valor de {valor}."
    val appointmentCreatedTemplate = "Ola {nome}, seu agendamento foi marcado para {agendamento_data} as {agendamento_hora}. Tipo: {agendamento_tipo}."
    val appointmentReminder2DaysTemplate = "Ola {nome}, lembrando do seu agendamento em 2 dias: {agendamento_data} as {agendamento_hora}."
    val appointmentReminder1DayTemplate = "Ola {nome}, lembrando do seu agendamento amanha, {agendamento_data} as {agendamento_hora}."
    val appointmentReminderTodayTemplate = "Ola {nome}, lembrando do seu agendamento hoje as {agendamento_hora}."

    private fun money(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

    private fun quantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(Locale("pt", "BR"), value)
}
