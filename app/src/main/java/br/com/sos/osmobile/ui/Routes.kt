package br.com.sos.osmobile.ui

enum class AppRoute(
    val route: String,
    val label: String,
) {
    Dashboard("dashboard", "Painel"),
    Customers("customers", "Clientes"),
    Services("services", "Servicos"),
    Quotes("quotes", "Orcamentos"),
    WorkOrders("work_orders", "Nova OS"),
    WorkOrderList("work_order_list", "Lista de OS"),
    Backup("backup", "Backup"),
    Settings("settings", "Configuracoes"),
    Audit("audit", "Auditoria"),
}
