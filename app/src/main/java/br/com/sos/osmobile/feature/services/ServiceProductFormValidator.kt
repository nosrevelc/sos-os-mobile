package br.com.sos.osmobile.feature.services

object ServiceProductFormValidator {
    fun validate(form: ServiceProductFormState): String? {
        if (form.name.isBlank()) {
            return "Nome e obrigatorio."
        }
        val price = parsePrice(form.unitPrice)
        if (price == null) {
            return "Valor deve ser um numero valido."
        }
        if (price < 0.0) {
            return "Valor nao pode ser negativo."
        }
        return null
    }

    fun parsePrice(value: String): Double? =
        value.trim()
            .replace("R$", "")
            .replace("\u00A0", "")
            .replace(" ", "")
            .let { trimmed ->
            if (trimmed.contains(",") && trimmed.contains(".")) {
                trimmed.replace(".", "").replace(",", ".")
            } else {
                trimmed.replace(",", ".")
            }
        }
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
}
