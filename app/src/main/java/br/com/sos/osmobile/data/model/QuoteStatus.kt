package br.com.sos.osmobile.data.model

enum class QuoteStatus(val label: String) {
    Pending("Pendente"),
    Approved("Aprovado"),
    Rejected("Rejeitado"),
    Converted("Convertido");
}
