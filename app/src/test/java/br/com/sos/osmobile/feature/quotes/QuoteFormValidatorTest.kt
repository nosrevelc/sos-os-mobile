package br.com.sos.osmobile.feature.quotes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteFormValidatorTest {
    @Test
    fun formWithoutCustomerReturnsMessage() {
        val form = QuoteFormState(
            selectedCustomerId = null,
            items = listOf(
                QuoteDraftItem(
                    serviceProductId = 1,
                    name = "Servico",
                    quantity = 1.0,
                    unitPrice = 10.0,
                ),
            ),
        )

        val result = QuoteFormValidator.validate(form)

        assertEquals("Selecione um cliente.", result)
    }

    @Test
    fun formWithoutItemsReturnsMessage() {
        val form = QuoteFormState(selectedCustomerId = 1)

        val result = QuoteFormValidator.validate(form)

        assertEquals("Adicione pelo menos um item.", result)
    }

    @Test
    fun validFormReturnsNoMessage() {
        val form = QuoteFormState(
            selectedCustomerId = 1,
            items = listOf(
                QuoteDraftItem(
                    serviceProductId = 1,
                    name = "Servico",
                    quantity = 2.0,
                    unitPrice = 10.0,
                ),
            ),
        )

        val result = QuoteFormValidator.validate(form)

        assertNull(result)
    }

    @Test
    fun invalidQuantityReturnsMessage() {
        val result = QuoteFormValidator.validateItem(quantity = "0", unitPrice = "10")

        assertEquals("Quantidade deve ser maior que zero.", result)
    }

    @Test
    fun invalidPriceReturnsMessage() {
        val result = QuoteFormValidator.validateItem(quantity = "1", unitPrice = "abc")

        assertEquals("Valor deve ser valido e nao negativo.", result)
    }

    @Test
    fun parseDecimalAcceptsBrazilianFormat() {
        val result = QuoteFormValidator.parseDecimal("1.234,56")

        assertEquals(1234.56, result ?: 0.0, 0.0)
    }
}
