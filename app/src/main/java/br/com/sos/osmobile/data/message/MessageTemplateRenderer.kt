package br.com.sos.osmobile.data.message

object MessageTemplateRenderer {
    fun render(template: String, tokens: Map<String, String>): String =
        tokens.entries.fold(template) { text, (key, value) ->
            text.replace("{$key}", value)
        }

    val quoteDefaultTemplate = "Ola {nome}, seu orcamento {orcamento} esta {status} no valor de {valor}."

    val workOrderDefaultTemplate = "Ola {nome}, sua OS {os} esta {status}. Total: {valor}. Pago: {valor_pago}. Saldo: {saldo}."

    val workOrderOpenTemplate = "Ola {nome}, sua OS {os} foi aberta. Total: {valor}. Pago: {valor_pago}. Saldo: {saldo}."
    val workOrderInProgressTemplate = "Ola {nome}, sua OS {os} esta em andamento."
    val workOrderCompletedTemplate = "Ola {nome}, sua OS {os} foi concluida. Total: {valor}. Pago: {valor_pago}. Saldo: {saldo}."
    val workOrderCanceledTemplate = "Ola {nome}, sua OS {os} foi cancelada."
    val reviewRequestTemplate = "Ola {nome}, voce poderia avaliar nosso atendimento? Sua opiniao e muito importante para nos."
    val pickupReminderTemplate = "Ola {nome}, sua OS {os} esta pronta ha {dias} dia(s). Por favor, venha retirar."
}
