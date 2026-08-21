package br.com.sos.osmobile.data.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkOrderMessageRendererTest {

    @Test
    fun paymentStatusIsPendingWithoutPayments() {
        assertEquals("Pendente", WorkOrderMessageRenderer.paymentStatus(totalValue = 100.0, paidTotal = 0.0))
        assertEquals("Pendente", WorkOrderMessageRenderer.paymentStatus(totalValue = 0.0, paidTotal = 50.0))
    }

    @Test
    fun paymentStatusIsPaidWhenBalanceCoversTotal() {
        assertEquals("Pago", WorkOrderMessageRenderer.paymentStatus(totalValue = 100.0, paidTotal = 100.0))
        assertEquals("Pago", WorkOrderMessageRenderer.paymentStatus(totalValue = 100.0, paidTotal = 99.995))
    }

    @Test
    fun paymentStatusIsPartialForMiddleGround() {
        assertEquals("Parcial", WorkOrderMessageRenderer.paymentStatus(totalValue = 100.0, paidTotal = 40.0))
    }

    @Test
    fun renderFillsTemplateTokens() {
        val text = WorkOrderMessageRenderer.render(
            template = "Ola {nome}, OS {os} {status}. Total {valor}. Saldo {saldo}. Status pagamento: {status_pagamento}.",
            customerName = "Maria",
            customerPhone = "11999990000",
            customerCpfCnpj = "",
            workOrderNumber = "2601010001",
            status = "Concluida",
            totalValue = 100.0,
            discountValue = 0.0,
            minAcceptanceValue = "",
            paidTotal = 40.0,
            companyName = "SOS",
            pixName = "SOS",
            pixKey = "key",
            nowMillis = 0L,
        )

        assertEquals(
            "Ola Maria, OS 2601010001 Concluida. Total R$ 100,00. Saldo R$ 60,00. Status pagamento: Parcial.",
            text.replace('\u00A0', ' '),
        )
    }

    @Test
    fun tokensIncludeItemLines() {
        val tokens = WorkOrderMessageRenderer.tokens(
            customerName = "Maria",
            customerPhone = "11999990000",
            customerCpfCnpj = "",
            workOrderNumber = "1",
            status = "Aberta",
            totalValue = 150.0,
            discountValue = 0.0,
            minAcceptanceValue = "",
            paidTotal = 0.0,
            companyName = "SOS",
            pixName = "SOS",
            pixKey = "key",
            items = listOf(
                MessageTemplateRenderer.ItemData(name = "Instalacao", quantity = 2.0, unitPrice = 50.0),
                MessageTemplateRenderer.ItemData(name = "Cabos", quantity = 1.0, unitPrice = 50.0),
            ),
            nowMillis = 0L,
        )

        val itens = tokens["itens"]!!.replace('\u00A0', ' ')
        assertTrue(itens.contains("- Instalacao: 2 x R$ 50,00 = R$ 100,00"))
        assertEquals("3", tokens["qtd_itens"])
        assertEquals("R$ 150,00", tokens["total_itens"]!!.replace('\u00A0', ' '))
    }
}
