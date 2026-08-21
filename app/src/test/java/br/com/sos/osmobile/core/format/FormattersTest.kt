package br.com.sos.osmobile.core.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormattersTest {

    private fun String.normalizeNbsp(): String = replace('\u00A0', ' ')

    @Test
    fun currencyFormatsPtBr() {
        assertEquals("R$ 1.234,56", Formatters.currency(1234.56).normalizeNbsp())
        assertEquals("R$ 0,50", Formatters.currency(0.5).normalizeNbsp())
    }

    @Test
    fun quantityOmitsDecimalsWhenWhole() {
        assertEquals("3", Formatters.quantity(3.0))
        assertEquals("2,50", Formatters.quantity(2.5))
        assertEquals("0,10", Formatters.quantity(0.1))
    }

    @Test
    fun fileSizeUsesHumanReadableUnits() {
        assertEquals("500 B", Formatters.fileSize(500L))
        assertEquals("2,0 KB", Formatters.fileSize(2048L))
        assertEquals("3,0 MB", Formatters.fileSize(3L * 1024 * 1024))
    }

    @Test
    fun dateTimeFormatsDayMonthYearHourMinute() {
        val formatted = Formatters.dateTime(1_700_000_000_000L)

        assertTrue(formatted.matches(Regex("\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}")))
    }

    @Test
    fun dateTimeShortProducesShortReadableText() {
        val formatted = Formatters.dateTimeShort(1_700_000_000_000L)

        assertTrue(formatted.matches(Regex("\\d{2}/\\d{2}/\\d{2,4} \\d{1,2}:\\d{2}")))
    }
}
