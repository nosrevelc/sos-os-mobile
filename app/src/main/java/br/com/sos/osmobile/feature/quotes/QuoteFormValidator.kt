package br.com.sos.osmobile.feature.quotes

object QuoteFormValidator {
    fun validate(form: QuoteFormState): String? {
        if (form.selectedCustomerId == null) {
            return "Selecione um cliente."
        }
        if (form.items.isEmpty()) {
            return "Adicione pelo menos um item."
        }
        return null
    }

    fun validateItem(quantity: String, unitPrice: String): String? {
        val parsedQuantity = parseDecimal(quantity)
        val parsedUnitPrice = parseDecimal(unitPrice)
        if (parsedQuantity == null || parsedQuantity <= 0.0) {
            return "Quantidade deve ser maior que zero."
        }
        if (parsedUnitPrice == null || parsedUnitPrice < 0.0) {
            return "Valor deve ser valido e nao negativo."
        }
        return null
    }

    fun parseDecimal(value: String): Double? =
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
