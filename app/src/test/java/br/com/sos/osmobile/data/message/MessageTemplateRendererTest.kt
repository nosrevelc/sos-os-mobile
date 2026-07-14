package br.com.sos.osmobile.data.message

import br.com.sos.osmobile.data.local.model.DocumentItem
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTemplateRendererTest {
    @Test
    fun renderReplacesKnownTokens() {
        val result = MessageTemplateRenderer.render(
            template = "Ola {nome}, OS {os}: {status}",
            tokens = mapOf(
                "nome" to "Cliente",
                "os" to "2607090001",
                "status" to "Aberta",
            ),
        )

        assertEquals("Ola Cliente, OS 2607090001: Aberta", result)
    }

    @Test
    fun itemTokensRenderItemsAndTotals() {
        val tokens = MessageTemplateRenderer.itemTokens(
            listOf(DocumentItem("Barra", 2.0, 10.0, 20.0)),
        )

        assertEquals("- Barra: 2 x R$ 10,00 = R$ 20,00", tokens["itens"])
        assertEquals("2", tokens["qtd_itens"])
        assertEquals("R$ 20,00", tokens["total_itens"])
    }
}
