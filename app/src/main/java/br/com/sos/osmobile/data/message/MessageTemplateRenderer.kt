package br.com.sos.osmobile.data.message

object MessageTemplateRenderer {
    fun render(template: String, tokens: Map<String, String>): String =
        tokens.entries.fold(template) { text, (key, value) ->
            text.replace("{$key}", value)
        }

    val quoteDefaultTemplate = "Ola {nome}, seu orcamento {orcamento} esta {status} no valor de {valor}."

    val workOrderDefaultTemplate = "Ola {nome}, sua OS {os} esta {status} no valor de {valor}."
}
