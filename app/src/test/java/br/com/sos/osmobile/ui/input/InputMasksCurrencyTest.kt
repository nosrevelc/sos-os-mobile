package br.com.sos.osmobile.ui.input

import org.junit.Assert.assertEquals
import org.junit.Test

class InputMasksCurrencyTest {
    private val brl = "\u00A0"

    @Test
    fun emptyInputReturnsEmpty() {
        assertEquals("", InputMasks.currency(""))
        assertEquals("", InputMasks.currency("   "))
    }

    @Test
    fun pureDigitsAreTreatedAsCents() {
        assertEquals("R$${brl}0,08", InputMasks.currency("8"))
        assertEquals("R$${brl}0,88", InputMasks.currency("88"))
        assertEquals("R$${brl}8,80", InputMasks.currency("880"))
        assertEquals("R$${brl}88,00", InputMasks.currency("8800"))
        assertEquals("R$${brl}880,00", InputMasks.currency("88000"))
        assertEquals("R$${brl}1.234,56", InputMasks.currency("123456"))
    }

    @Test
    fun alreadyFormattedValueIsPreserved() {
        assertEquals("R$${brl}88,00", InputMasks.currency("R$${brl}88,00"))
        assertEquals("R$${brl}880,00", InputMasks.currency("R$${brl}880,00"))
        assertEquals("R$${brl}1.234,56", InputMasks.currency("R$${brl}1.234,56"))
        assertEquals("R$${brl}0,08", InputMasks.currency("R$${brl}0,08"))
    }

    @Test
    fun editingFormattedValuePreservesAmount() {
        val initial = "R$${brl}880,00"
        val result = InputMasks.currency(initial)
        assertEquals("R$${brl}880,00", result)
    }

    @Test
    fun editingWithThousandsSeparator() {
        assertEquals("R$${brl}1.234,56", InputMasks.currency("R$${brl}1.234,56"))
        assertEquals("R$${brl}12.345,67", InputMasks.currency("R$${brl}12.345,67"))
    }

    @Test
    fun decimalInputWithComma() {
        assertEquals("R$${brl}88,00", InputMasks.currency("88,00"))
    }

    @Test
    fun formattedInputWithCommaOnly() {
        assertEquals("R$${brl}88,00", InputMasks.currency("R$${brl}88,00"))
    }

    @Test
    fun malformedInputFallsBackToDigits() {
        assertEquals("R$${brl}88,00", InputMasks.currency("R$ invalid 88,00"))
    }

    @Test
    fun largeValuesAreHandled() {
        assertEquals("R$${brl}99.999.999,99", InputMasks.currency("9999999999"))
    }

    @Test
    fun singleCent() {
        assertEquals("R$${brl}0,01", InputMasks.currency("1"))
    }

    @Test
    fun zeroValue() {
        assertEquals("R$${brl}0,00", InputMasks.currency("0"))
        assertEquals("R$${brl}0,00", InputMasks.currency("00"))
    }
}