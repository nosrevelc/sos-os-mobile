package br.com.sos.osmobile.feature.customers

import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomerFormValidatorTest {
    @Test
    fun validFormWithOptionalCpfReturnsNoMessage() {
        val form = CustomerFormState(
            name = "Cliente Teste",
            phone = "11999999999",
            cpfCnpj = "",
        )

        val result = CustomerFormValidator.validate(form, CpfCnpjPolicy.Optional)

        assertNull(result)
    }

    @Test
    fun requiredCpfWithoutValueReturnsMessage() {
        val form = CustomerFormState(
            name = "Cliente Teste",
            phone = "11999999999",
            cpfCnpj = "",
        )

        val result = CustomerFormValidator.validate(form, CpfCnpjPolicy.Required)

        assertEquals("CPF/CNPJ e obrigatorio.", result)
    }

    @Test
    fun blankPhoneReturnsMessage() {
        val form = CustomerFormState(
            name = "Cliente Teste",
            phone = "",
            cpfCnpj = "123",
        )

        val result = CustomerFormValidator.validate(form, CpfCnpjPolicy.Required)

        assertEquals("Telefone e obrigatorio.", result)
    }
}
