package br.com.sos.osmobile.ui

enum class AppRoute(
    val route: String,
    val label: String,
) {
    Dashboard("dashboard", "Painel"),
    Customers("customers", "Clientes"),
    Services("services", "Servicos"),
    Quotes("quotes", "Novo Orcamento"),
    QuoteList("quote_list", "Lista de Orcamentos"),
    WorkOrders("work_orders", "Nova OS"),
    WorkOrderList("work_order_list", "Lista de OS"),
    WorkOrderPickup("work_order_pickup", "OS para Retirada"),
    QuickMessages("quick_messages", "Mensagens Rapidas"),
    Sales("sales", "Vendas"),
    Finance("finance", "Financeiro"),
    Reports("reports", "Relatorios"),
    Backup("backup", "Backup"),
    Settings("settings", "Configuracoes"),
    Audit("audit", "Auditoria"),
}
