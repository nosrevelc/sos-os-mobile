package br.com.sos.osmobile.data.message

import java.text.Normalizer
import java.util.Locale

object PixPayloadGenerator {
    fun generate(key: String, name: String, amount: Double): String {
        if (key.isBlank() || name.isBlank() || amount <= 0.0) return ""
        return buildPayload(key, name, amount)
    }

    fun generateOpenAmount(key: String, name: String): String {
        if (key.isBlank() || name.isBlank()) return ""
        return buildPayload(key, name, null)
    }

    private fun buildPayload(key: String, name: String, amount: Double?): String {
        val pixKey = normalizeKey(key)
        val fields = mutableListOf(
            field("00", "01"),
            field("26", field("00", "br.gov.bcb.pix") + field("01", pixKey)),
            field("52", "0000"),
            field("53", "986"),
        )
        amount?.let { fields += field("54", String.format(Locale.US, "%.2f", it)) }
        fields += field("58", "BR")
        fields += field("59", clean(name).take(25))
        fields += field("60", "CAMBUI")
        fields += field("62", field("05", "***"))
        val base = fields.joinToString("")
        val withCrcId = base + "6304"
        return withCrcId + crc16(withCrcId)
    }

    private fun field(id: String, value: String): String =
        id + value.length.toString().padStart(2, '0') + value

    private fun clean(value: String): String =
        Normalizer.normalize(value.uppercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^A-Z0-9 ]"), "")
            .ifBlank { "PIX" }

    private fun normalizeKey(value: String): String {
        val trimmed = value.trim()
        if (trimmed.startsWith("+") || trimmed.contains("@")) return trimmed
        val digits = trimmed.filter { it.isDigit() }
        return if (digits.length == 10 || digits.length == 11) "+55$digits" else trimmed
    }

    private fun crc16(value: String): String {
        var crc = 0xFFFF
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    (crc shl 1) xor 0x1021
                } else {
                    crc shl 1
                } and 0xFFFF
            }
        }
        return crc.toString(16).uppercase(Locale.ROOT).padStart(4, '0')
    }
}
