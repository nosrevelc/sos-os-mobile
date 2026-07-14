package br.com.sos.osmobile.feature.services

import br.com.sos.osmobile.data.local.entity.ServiceProductType

object ServiceProductFormValidator {
    fun validate(form: ServiceProductFormState): String? {
        if (form.name.isBlank()) {
            return "Nome e obrigatorio."
        }
        if (form.type !in ServiceProductType.all) {
            return "Tipo invalido."
        }
        val price = parsePrice(form.unitPrice)
        if (price == null) {
            return "Valor deve ser um numero valido."
        }
        if (price < 0.0) {
            return "Valor nao pode ser negativo."
        }
        if (parseQuantity(form.minimumStock) == null) {
            return "Estoque minimo invalido."
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

    fun parseQuantity(value: String): Double? =
        value.trim()
            .replace(",", ".")
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
}
