package br.com.sos.osmobile.ui.input

import java.text.NumberFormat
import java.util.Locale

object InputMasks {
    fun digits(value: String, maxLength: Int = Int.MAX_VALUE): String =
        value.filter(Char::isDigit).take(maxLength)

    fun phone(value: String): String {
        val digits = brazilianPhoneDigits(value)
        return when {
            digits.length <= 2 -> digits
            digits.length <= 6 -> "(${digits.take(2)}) ${digits.drop(2)}"
            digits.length <= 10 -> "(${digits.take(2)}) ${digits.substring(2, 6)}-${digits.drop(6)}"
            else -> "(${digits.take(2)}) ${digits.substring(2, 7)}-${digits.drop(7)}"
        }
    }

    private fun brazilianPhoneDigits(value: String): String {
        val rawDigits = digits(value)
        val withoutInternationalPrefix = when {
            rawDigits.startsWith("0055") && rawDigits.length > 12 -> rawDigits.drop(4)
            rawDigits.startsWith("55") && rawDigits.length > 11 -> rawDigits.drop(2)
            else -> rawDigits
        }
        return withoutInternationalPrefix.take(11)
    }

    fun cpfCnpj(value: String): String {
        val digits = digits(value, 14)
        return if (digits.length <= 11) {
            cpf(digits)
        } else {
            cnpj(digits)
        }
    }

    fun decimal(value: String, integerDigits: Int = 7, decimalDigits: Int = 2): String {
        val cleaned = value.replace(".", ",").filter { it.isDigit() || it == ',' }
        val parts = cleaned.split(",", limit = 2)
        val integer = parts.getOrNull(0).orEmpty().filter(Char::isDigit).take(integerDigits)
        val decimal = parts.getOrNull(1).orEmpty().filter(Char::isDigit).take(decimalDigits)
        return if (cleaned.contains(",")) "$integer,$decimal" else integer
    }

    fun currency(value: String): String {
        val digits = digits(value, 11)
        if (digits.isBlank()) return ""
        val cents = digits.toLongOrNull() ?: 0L
        return currencyFromDouble(cents / 100.0)
    }

    fun currencyFromDouble(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

    fun dateIso(value: String): String {
        val digits = digits(value, 8)
        return when {
            digits.length <= 4 -> digits
            digits.length <= 6 -> "${digits.take(4)}-${digits.drop(4)}"
            else -> "${digits.take(4)}-${digits.substring(4, 6)}-${digits.drop(6)}"
        }
    }

    private fun cpf(digits: String): String =
        when {
            digits.length <= 3 -> digits
            digits.length <= 6 -> "${digits.take(3)}.${digits.drop(3)}"
            digits.length <= 9 -> "${digits.take(3)}.${digits.substring(3, 6)}.${digits.drop(6)}"
            else -> "${digits.take(3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.drop(9)}"
        }

    private fun cnpj(digits: String): String =
        when {
            digits.length <= 2 -> digits
            digits.length <= 5 -> "${digits.take(2)}.${digits.drop(2)}"
            digits.length <= 8 -> "${digits.take(2)}.${digits.substring(2, 5)}.${digits.drop(5)}"
            digits.length <= 12 -> "${digits.take(2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.drop(8)}"
            else -> "${digits.take(2)}.${digits.substring(2, 5)}.${digits.substring(5, 8)}/${digits.substring(8, 12)}-${digits.drop(12)}"
        }
}
