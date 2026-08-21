package br.com.sos.osmobile.data.backup

import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.repository.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvSupportTest {

    @Test
    fun escEscapesBackslashQuotesAndNewlines() {
        assertEquals("a\\\\b", esc("a\\b"))
        assertEquals("a\\\"b", esc("a\"b"))
        assertEquals("a\\nb", esc("a\nb"))
    }

    @Test
    fun strWrapsInQuotesAndHandlesNull() {
        assertEquals("\"joao\"", str("joao"))
        assertEquals("null", str(null))
        assertEquals("\"a\\nb\"", str("a\nb"))
    }

    @Test
    fun parseCsvReadsSimpleRecords() {
        val rows = parseCsv("nome,telefone\nJoao,11999990000\nMaria,11888880000\n")

        assertEquals(2, rows.size)
        assertEquals("Joao", rows[0].value("nome"))
        assertEquals("11999990000", rows[0].value("telefone"))
        assertEquals("Maria", rows[1].value("nome"))
    }

    @Test
    fun parseCsvSupportsQuotedFieldsWithCommaAndEscapedQuotes() {
        val rows = parseCsv("nome,observacao\n\"Silva, Joao\",\"diz \\\"oi\\\"\"\n")

        assertEquals("Silva, Joao", rows[0].value("nome"))
        assertEquals("diz \"oi\"", rows[0].value("observacao"))
    }

    @Test
    fun parseCsvHandlesCrLfLineEndings() {
        val rows = parseCsv("nome\r\nJoao\r\n")

        assertEquals("Joao", rows[0].value("nome"))
    }

    @Test
    fun parseCsvRemovesBomLowercasesAndTrimsHeaders() {
        val rows = parseCsv("\uFEFF NOME , TELEFONE \n Joao , 1199 \n")

        assertEquals("Joao", rows[0].value("nome"))
        assertEquals("1199", rows[0].value("TELEFONE"))
    }

    @Test
    fun parseCsvRequiresHeaderAndDataRow() {
        var thrown = false
        try {
            parseCsv("nome,telefone\n")
        } catch (error: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun valueReturnsFirstMatchingKeyAndTrims() {
        val row = mapOf("cpf/cnpj" to " 123 ", "cpf" to "456")

        assertEquals("123", row.value("cpf_cnpj", "cpf/cnpj", "cpf"))
        assertEquals("456", row.value("cpf"))
        assertEquals("", row.value("inexistente"))
    }

    @Test
    fun blankToNullConvertsBlankToNull() {
        assertNull("  ".blankToNull())
        assertEquals("x", " x ".blankToNull())
    }

    @Test
    fun parseMoneyHandlesBrazilianAndSimpleFormats() {
        assertEquals(1234.56, "R$ 1.234,56".parseMoney(), 0.0001)
        assertEquals(12.5, "12,5".parseMoney(), 0.0001)
        assertEquals(12.5, "12.5".parseMoney(), 0.0001)
        assertEquals(0.0, "".parseMoney(), 0.0001)
        assertEquals(0.0, "abc".parseMoney(), 0.0001)
    }

    @Test
    fun decodeCsvEscapedLinesNormalizesBreaks() {
        assertEquals("a\nb\nc\nd", "a\\r\\nb\\nc\rd".decodeCsvEscapedLines())
    }

    @Test
    fun normalizedServiceTypeMapsAliases() {
        assertEquals(ServiceProductType.PRODUCT, normalizedServiceType("Produto"))
        assertEquals(ServiceProductType.PRODUCT, normalizedServiceType("PRODUCT"))
        assertEquals(ServiceProductType.SUPPLY, normalizedServiceType(" insumo "))
        assertEquals(ServiceProductType.SERVICE, normalizedServiceType("servico"))
        assertEquals(ServiceProductType.SERVICE, normalizedServiceType(""))
    }

    @Test
    fun messageTemplateKeysCoversMainTemplates() {
        val keys = messageTemplateKeys()

        assertFalse(keys.isEmpty())
        assertEquals(SettingsRepository.TEMPLATE_QUOTE_KEY, keys["orcamento"])
        assertEquals(SettingsRepository.TEMPLATE_WORK_ORDER_KEY, keys["os_geral"])
        assertEquals(SettingsRepository.TEMPLATE_PAYMENT_CONFIRMED_KEY, keys["pagamento_confirmado"])
    }
}
