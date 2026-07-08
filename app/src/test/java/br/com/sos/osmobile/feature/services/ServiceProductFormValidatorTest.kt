package br.com.sos.osmobile.feature.services

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceProductFormValidatorTest {
    @Test
    fun validFormReturnsNoMessage() {
        val form = ServiceProductFormState(
            code = "SRV-001",
            name = "Troca de tela",
            unitPrice = "150,50",
        )

        val result = ServiceProductFormValidator.validate(form)

        assertNull(result)
    }

    @Test
    fun blankCodeReturnsMessage() {
        val form = ServiceProductFormState(
            code = "",
            name = "Troca de tela",
            unitPrice = "150",
        )

        val result = ServiceProductFormValidator.validate(form)

        assertEquals("Codigo e obrigatorio.", result)
    }

    @Test
    fun invalidPriceReturnsMessage() {
        val form = ServiceProductFormState(
            code = "SRV-001",
            name = "Troca de tela",
            unitPrice = "abc",
        )

        val result = ServiceProductFormValidator.validate(form)

        assertEquals("Valor deve ser um numero valido.", result)
    }

    @Test
    fun negativePriceReturnsMessage() {
        val form = ServiceProductFormState(
            code = "SRV-001",
            name = "Troca de tela",
            unitPrice = "-1",
        )

        val result = ServiceProductFormValidator.validate(form)

        assertEquals("Valor nao pode ser negativo.", result)
    }

    @Test
    fun parsePriceAcceptsDotDecimalSeparator() {
        val result = ServiceProductFormValidator.parsePrice("150.50")

        assertEquals(150.50, result ?: 0.0, 0.0)
    }
}
