package br.com.sos.osmobile.core.format

import java.text.DateFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object Formatters {

    private val ptBr = Locale("pt", "BR")

    fun currency(value: Double): String =
        NumberFormat.getCurrencyInstance(ptBr).format(value)

    fun quantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format(ptBr, "%.2f", value)

    fun fileSize(bytes: Long): String =
        when {
            bytes >= 1_048_576L -> String.format(ptBr, "%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024L -> String.format(ptBr, "%.1f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }

    fun dateTimeShort(timestamp: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

    fun dateTime(timestamp: Long): String =
        Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
}
