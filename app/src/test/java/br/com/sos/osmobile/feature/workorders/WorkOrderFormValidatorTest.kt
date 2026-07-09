package br.com.sos.osmobile.feature.workorders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkOrderFormValidatorTest {
    @Test
    fun formWithoutCustomerReturnsMessage() {
        val form = WorkOrderFormState(
            selectedCustomerId = null,
            items = listOf(
                WorkOrderDraftItem(
                    serviceProductId = 1,
                    name = "Servico",
                    quantity = 1.0,
                    unitPrice = 10.0,
                ),
            ),
        )

        val result = WorkOrderFormValidator.validate(form)

        assertEquals("Selecione um cliente.", result)
    }

    @Test
    fun formWithoutItemsReturnsMessage() {
        val form = WorkOrderFormState(selectedCustomerId = 1)

        val result = WorkOrderFormValidator.validate(form)

        assertEquals("Adicione pelo menos um item.", result)
    }

    @Test
    fun validFormReturnsNoMessage() {
        val form = WorkOrderFormState(
            selectedCustomerId = 1,
            items = listOf(
                WorkOrderDraftItem(
                    serviceProductId = 1,
                    name = "Servico",
                    quantity = 2.0,
                    unitPrice = 10.0,
                ),
            ),
        )

        val result = WorkOrderFormValidator.validate(form)

        assertNull(result)
    }

    @Test
    fun invalidQuantityReturnsMessage() {
        val result = WorkOrderFormValidator.validateItem(quantity = "0", unitPrice = "10")

        assertEquals("Quantidade deve ser maior que zero.", result)
    }

    @Test
    fun invalidPriceReturnsMessage() {
        val result = WorkOrderFormValidator.validateItem(quantity = "1", unitPrice = "abc")

        assertEquals("Valor deve ser valido e nao negativo.", result)
    }

    @Test
    fun parseDecimalAcceptsBrazilianFormat() {
        val result = WorkOrderFormValidator.parseDecimal("1.234,56")

        assertEquals(1234.56, result ?: 0.0, 0.0)
    }
}
