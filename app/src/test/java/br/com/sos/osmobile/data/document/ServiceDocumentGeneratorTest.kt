package br.com.sos.osmobile.data.document

import br.com.sos.osmobile.data.local.model.DocumentItem
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceDocumentGeneratorTest {
    @Test
    fun generateIncludesMainDocumentFields() {
        val text = ServiceDocumentGenerator.generate(
            title = "ORDEM DE SERVICO",
            number = "2607090001",
            customerName = "Cliente Teste",
            status = "Aberta",
            totalValue = 100.0,
            notes = "Sem observacoes",
            items = listOf(
                DocumentItem(
                    name = "Servico",
                    quantity = 2.0,
                    unitPrice = 50.0,
                    subtotal = 100.0,
                ),
            ),
        )

        assertTrue(text.contains("ORDEM DE SERVICO"))
        assertTrue(text.contains("2607090001"))
        assertTrue(text.contains("Cliente Teste"))
        assertTrue(text.contains("Servico"))
        assertTrue(text.contains("Total:"))
    }
}
