package br.com.sos.osmobile.data.model

enum class WorkOrderStatus(val label: String) {
    Open("Aberta"),
    InProgress("Em andamento"),
    Completed("Concluida"),
    Canceled("Cancelada");
}
