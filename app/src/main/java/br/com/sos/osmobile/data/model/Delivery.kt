package br.com.sos.osmobile.data.model

object DeliveryType {
    const val PICKUP = "Retirada no local"
    const val OWN_DELIVERY = "Entrega propria"
    const val CARRIER = "Correios/transportadora"
    const val COURIER = "Motoboy"

    val all = listOf(PICKUP, OWN_DELIVERY, CARRIER, COURIER)
}

object DeliveryStatus {
    const val WAITING_PICKUP = "Aguardando retirada"
    const val OUT_FOR_DELIVERY = "Saiu para entrega"
    const val SENT = "Pedido enviado"
    const val DELIVERED = "Entregue"
    const val NOT_DELIVERED = "Nao entregue"

    val all = listOf(WAITING_PICKUP, OUT_FOR_DELIVERY, SENT, DELIVERED, NOT_DELIVERED)
}
