package br.com.sos.osmobile.data.backup

import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.repository.SettingsRepository
import org.json.JSONObject

// Helpers de CSV compartilhados entre exportacao e importacao.
internal fun esc(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

internal fun str(value: String?): String =
    value?.let { "\"${esc(it)}\"" } ?: "null"

internal fun parseCsv(csv: String): List<Map<String, String>> {
    val records = mutableListOf<List<String>>()
    val currentRecord = mutableListOf<String>()
    val currentField = StringBuilder()
    var quoted = false
    var index = 0

    while (index < csv.length) {
        val char = csv[index]
        when {
            char == '"' && quoted && index + 1 < csv.length && csv[index + 1] == '"' -> {
                currentField.append('"')
                index++
            }
            char == '"' -> quoted = !quoted
            char == ',' && !quoted -> {
                currentRecord += currentField.toString()
                currentField.clear()
            }
            (char == '\n' || char == '\r') && !quoted -> {
                if (char == '\r' && index + 1 < csv.length && csv[index + 1] == '\n') index++
                currentRecord += currentField.toString()
                currentField.clear()
                if (currentRecord.any { it.isNotBlank() }) records += currentRecord.toList()
                currentRecord.clear()
            }
            else -> currentField.append(char)
        }
        index++
    }
    currentRecord += currentField.toString()
    if (currentRecord.any { it.isNotBlank() }) records += currentRecord.toList()

    require(records.size >= 2) { "CSV precisa de cabecalho e ao menos uma linha." }
    val headers = records.first().map { it.trim().removePrefix("\uFEFF").lowercase() }
    return records.drop(1).map { record ->
        headers.mapIndexed { column, header -> header to record.getOrElse(column) { "" }.trim() }.toMap()
    }
}

internal fun Map<String, String>.value(vararg keys: String): String =
    keys.firstNotNullOfOrNull { key -> this[key.lowercase()] }?.trim().orEmpty()

internal fun String.blankToNull(): String? = takeIf { it.isNotBlank() }

internal fun String.parseMoney(): Double {
    val clean = replace(Regex("[^0-9,.-]"), "")
    if (clean.isBlank()) return 0.0
    val normalized = if (clean.contains(",")) clean.replace(".", "").replace(",", ".") else clean
    return normalized.toDoubleOrNull() ?: 0.0
}

internal fun String.decodeCsvEscapedLines(): String =
    replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n")

internal fun normalizedServiceType(value: String): String =
    when (value.trim().lowercase()) {
        "produto", "product" -> ServiceProductType.PRODUCT
        "insumo", "supply" -> ServiceProductType.SUPPLY
        else -> ServiceProductType.SERVICE
    }

internal fun messageTemplateKeys(): Map<String, String> = mapOf(
    "orcamento" to SettingsRepository.TEMPLATE_QUOTE_KEY,
    "os_geral" to SettingsRepository.TEMPLATE_WORK_ORDER_KEY,
    "os_aberta" to SettingsRepository.TEMPLATE_WORK_ORDER_OPEN_KEY,
    "os_em_andamento" to SettingsRepository.TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY,
    "os_concluida" to SettingsRepository.TEMPLATE_WORK_ORDER_COMPLETED_KEY,
    "os_cancelada" to SettingsRepository.TEMPLATE_WORK_ORDER_CANCELED_KEY,
    "solicitacao_avaliacao" to SettingsRepository.TEMPLATE_REVIEW_REQUEST_KEY,
    "aviso_retirada" to SettingsRepository.TEMPLATE_PICKUP_REMINDER_KEY,
    "pagamento_pendente" to SettingsRepository.TEMPLATE_PAYMENT_PENDING_KEY,
    "pagamento_confirmado" to SettingsRepository.TEMPLATE_PAYMENT_CONFIRMED_KEY,
    "solicitar_comprovante" to SettingsRepository.TEMPLATE_PAYMENT_PROOF_REQUEST_KEY,
    "pedido_enviado" to SettingsRepository.TEMPLATE_ORDER_SENT_KEY,
    "saiu_entrega" to SettingsRepository.TEMPLATE_OUT_FOR_DELIVERY_KEY,
    "entregue" to SettingsRepository.TEMPLATE_DELIVERED_KEY,
    "nao_entregue" to SettingsRepository.TEMPLATE_NOT_DELIVERED_KEY,
    "agradecimento" to SettingsRepository.TEMPLATE_THANK_YOU_KEY,
    "comunicado" to SettingsRepository.TEMPLATE_ANNOUNCEMENT_KEY,
    "boas_vindas" to SettingsRepository.TEMPLATE_WELCOME_KEY,
    "orcamento_expirado" to SettingsRepository.TEMPLATE_QUOTE_EXPIRED_KEY,
    "lembrete_orcamento" to SettingsRepository.TEMPLATE_QUOTE_REMINDER_KEY,
)
