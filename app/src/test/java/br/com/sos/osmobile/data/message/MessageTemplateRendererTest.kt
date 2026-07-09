package br.com.sos.osmobile.data.message

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
}
