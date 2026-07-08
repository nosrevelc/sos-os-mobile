package br.com.sos.osmobile.ui

enum class AppRoute(
    val route: String,
    val label: String,
) {
    Customers("customers", "Clientes"),
    Services("services", "Servicos"),
    Quotes("quotes", "Orcamentos"),
    WorkOrders("work_orders", "OS"),
    Settings("settings", "Configuracoes"),
    Audit("audit", "Auditoria"),
}
