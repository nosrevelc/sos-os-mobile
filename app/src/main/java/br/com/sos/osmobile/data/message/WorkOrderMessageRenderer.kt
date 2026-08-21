package br.com.sos.osmobile.data.message

import br.com.sos.osmobile.core.format.Formatters

object WorkOrderMessageRenderer {

    fun paymentStatus(totalValue: Double, paidTotal: Double): String =
        when {
            totalValue <= 0.0 || paidTotal <= 0.0 -> "Pendente"
            paidTotal + 0.009 >= totalValue -> "Pago"
            else -> "Parcial"
        }

    fun tokens(
        customerName: String,
        customerPhone: String,
        customerCpfCnpj: String,
        workOrderNumber: String,
        status: String,
        totalValue: Double,
        discountValue: Double,
        minAcceptanceValue: String,
        deliveryType: String = "",
        deliveryStatus: String = "",
        deliveryAddress: String = "",
        deliveryFee: Double = 0.0,
        trackingCode: String = "",
        paidTotal: Double,
        companyName: String,
        pixName: String,
        pixKey: String,
        items: List<MessageTemplateRenderer.ItemData> = emptyList(),
        nowMillis: Long = System.currentTimeMillis(),
    ): Map<String, String> {
        val balance = (totalValue - paidTotal).coerceAtLeast(0.0)
        val subtotalValue = totalValue + discountValue
        return mapOf(
            "nome" to customerName,
            "telefone" to customerPhone,
            "cpf" to customerCpfCnpj,
            "os" to workOrderNumber,
            "orcamento" to "",
            "status" to status,
            "valor" to Formatters.currency(totalValue),
            "subtotal" to Formatters.currency(subtotalValue),
            "desconto" to Formatters.currency(discountValue),
            "linha_desconto" to if (discountValue > 0.0) "Desconto: ${Formatters.currency(discountValue)}" else "",
            "valor_minimo_aceite" to minAcceptanceValue,
            "tipo_entrega" to deliveryType,
            "status_entrega" to deliveryStatus,
            "endereco_entrega" to deliveryAddress,
            "taxa_entrega" to Formatters.currency(deliveryFee),
            "codigo_rastreio" to trackingCode,
            "valor_pago" to Formatters.currency(paidTotal),
            "saldo" to Formatters.currency(balance),
            "status_pagamento" to paymentStatus(totalValue, paidTotal),
            "empresa" to companyName,
            "data" to Formatters.dateTimeShort(nowMillis),
            "PIX" to PixPayloadGenerator.generate(pixKey, pixName, balance.takeIf { it > 0.0 } ?: totalValue),
            "PIX_SEM_VALOR" to PixPayloadGenerator.generateOpenAmount(pixKey, pixName),
            "PIX_QR" to "",
        ) + MessageTemplateRenderer.itemTokensOf(items)
    }

    fun render(
        template: String,
        customerName: String,
        customerPhone: String,
        customerCpfCnpj: String,
        workOrderNumber: String,
        status: String,
        totalValue: Double,
        discountValue: Double,
        minAcceptanceValue: String,
        deliveryType: String = "",
        deliveryStatus: String = "",
        deliveryAddress: String = "",
        deliveryFee: Double = 0.0,
        trackingCode: String = "",
        paidTotal: Double,
        companyName: String,
        pixName: String,
        pixKey: String,
        items: List<MessageTemplateRenderer.ItemData> = emptyList(),
        nowMillis: Long = System.currentTimeMillis(),
    ): String = MessageTemplateRenderer.render(
        template = template,
        tokens = tokens(
            customerName = customerName,
            customerPhone = customerPhone,
            customerCpfCnpj = customerCpfCnpj,
            workOrderNumber = workOrderNumber,
            status = status,
            totalValue = totalValue,
            discountValue = discountValue,
            minAcceptanceValue = minAcceptanceValue,
            deliveryType = deliveryType,
            deliveryStatus = deliveryStatus,
            deliveryAddress = deliveryAddress,
            deliveryFee = deliveryFee,
            trackingCode = trackingCode,
            paidTotal = paidTotal,
            companyName = companyName,
            pixName = pixName,
            pixKey = pixKey,
            items = items,
            nowMillis = nowMillis,
        ),
    )
}
