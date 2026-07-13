package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.document.ServiceDocumentGenerator
import br.com.sos.osmobile.data.local.dao.WorkOrderDao
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity
import br.com.sos.osmobile.data.local.model.DocumentItem
import br.com.sos.osmobile.data.local.model.WorkOrderServiceUsage
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalTextBlock
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import java.util.Locale

data class WorkOrderItemInput(
    val serviceProductId: Long,
    val quantity: Double,
    val practicedUnitPrice: Double,
)

class WorkOrderRepository(
    private val workOrderDao: WorkOrderDao,
    private val auditRepository: AuditRepository,
) {
    fun observeAll(): Flow<List<WorkOrderEntity>> = workOrderDao.observeAll()

    fun observeSummaries(): Flow<List<WorkOrderSummary>> = workOrderDao.observeSummaries()

    fun observeServiceUsage(): Flow<List<WorkOrderServiceUsage>> = workOrderDao.observeServiceUsage()

    suspend fun findSummaryById(id: Long): WorkOrderSummary? = workOrderDao.findSummaryById(id)

    suspend fun findById(id: Long): WorkOrderEntity? = workOrderDao.findById(id)

    suspend fun listSummariesByCustomer(customerId: Long): List<WorkOrderSummary> =
        workOrderDao.listSummariesByCustomer(customerId)

    suspend fun listDocumentItems(id: Long): List<DocumentItem> = workOrderDao.findDocumentItems(id)

    suspend fun listItems(id: Long): List<WorkOrderItemEntity> = workOrderDao.findItemsByWorkOrder(id)

    suspend fun create(
        customerId: Long,
        status: String,
        notes: String?,
        items: List<WorkOrderItemInput>,
    ): Long {
        val now = Clock.nowMillis()
        val number = generateWorkOrderNumber(now)
        val workOrderItems = items.map {
            WorkOrderItemEntity(
                workOrderId = 0,
                serviceProductId = it.serviceProductId,
                quantidade = it.quantity,
                practicedUnitPrice = it.practicedUnitPrice,
                subtotal = it.quantity * it.practicedUnitPrice,
            )
        }
        val id = workOrderDao.insertWithItems(
            workOrder = WorkOrderEntity(
                numero = number,
                customerId = customerId,
                openedAt = now,
                status = status,
                observacoes = notes?.trim()?.takeIf { it.isNotBlank() },
                totalValue = workOrderItems.sumOf { it.subtotal },
                concludedAt = if (status == "Concluida") now else null,
                updatedAt = now,
            ),
            items = workOrderItems,
        )
        auditRepository.record("Ordens de Servico", "OS criada", "ordens_servico", id, details = number)
        return id
    }

    suspend fun updateStatus(id: Long, status: WorkOrderStatus) {
        val current = workOrderDao.findById(id) ?: return
        val now = Clock.nowMillis()
        workOrderDao.update(
            current.copy(
                status = status.label,
                concludedAt = if (status == WorkOrderStatus.Completed) now else current.concludedAt,
                updatedAt = now,
            ),
        )
        auditRepository.record(
            "Ordens de Servico",
            "Status da OS alterado",
            "ordens_servico",
            id,
            details = "${current.status} -> ${status.label}",
        )
    }

    suspend fun updateContent(
        id: Long,
        customerId: Long,
        status: WorkOrderStatus,
        notes: String?,
        items: List<WorkOrderItemInput>,
    ): Boolean {
        val current = workOrderDao.findById(id) ?: return false
        val now = Clock.nowMillis()
        val workOrderItems = items.map {
            WorkOrderItemEntity(
                workOrderId = id,
                serviceProductId = it.serviceProductId,
                quantidade = it.quantity,
                practicedUnitPrice = it.practicedUnitPrice,
                subtotal = it.quantity * it.practicedUnitPrice,
            )
        }
        workOrderDao.updateWithItems(
            workOrder = current.copy(
                customerId = customerId,
                status = status.label,
                observacoes = notes?.trim()?.takeIf { it.isNotBlank() },
                totalValue = workOrderItems.sumOf { it.subtotal },
                concludedAt = if (status == WorkOrderStatus.Completed) now else current.concludedAt,
                updatedAt = now,
            ),
            items = workOrderItems,
        )
        val details = if (current.status != status.label) {
            "${current.numero}; status ${current.status} -> ${status.label}"
        } else {
            current.numero
        }
        auditRepository.record("Ordens de Servico", "OS editada", "ordens_servico", id, details = details)
        return true
    }

    suspend fun generateDocumentText(id: Long): String? {
        val workOrder = workOrderDao.findById(id) ?: return null
        val summary = workOrderDao.findSummaryById(id) ?: return null
        return ServiceDocumentGenerator.generate(
            title = "ORDEM DE SERVICO",
            number = workOrder.numero,
            customerName = summary.customerName,
            status = workOrder.status,
            totalValue = workOrder.totalValue,
            notes = workOrder.observacoes,
            items = workOrderDao.findDocumentItems(id),
        )
    }

    suspend fun generateThermalPrintContent(
        id: Long,
        headerTemplate: String,
        footerTemplate: String,
        companyName: String,
    ): ThermalPrintContent? {
        val workOrder = workOrderDao.findById(id) ?: return null
        val summary = workOrderDao.findSummaryById(id) ?: return null
        val tokens = mapOf(
            "empresa" to companyName,
            "data" to formatDate(workOrder.openedAt),
            "os" to workOrder.numero,
            "nome" to summary.customerName,
            "telefone" to summary.customerPhone,
            "valor" to money(workOrder.totalValue),
            "status" to workOrder.status,
        )
        return ThermalPrintContent(
            header = MessageTemplateRenderer.render(headerTemplate, tokens).trim(),
            body = ServiceDocumentGenerator.generate(
                title = "ORDEM DE SERVICO",
                number = workOrder.numero,
                customerName = summary.customerName,
                status = workOrder.status,
                totalValue = workOrder.totalValue,
                notes = workOrder.observacoes,
                items = workOrderDao.findDocumentItems(id),
            ),
            footer = MessageTemplateRenderer.render(footerTemplate, tokens).trim(),
        )
    }

    suspend fun generateShelfLabelBlocks(id: Long): List<ThermalTextBlock>? {
        val workOrder = workOrderDao.findById(id) ?: return null
        val summary = workOrderDao.findSummaryById(id) ?: return null
        return listOf(
            ThermalTextBlock(
                text = "OS",
                alignment = "center",
                bold = true,
                size = "large",
            ),
            ThermalTextBlock(
                text = workOrder.numero,
                alignment = "center",
                bold = true,
                size = "large",
            ),
            ThermalTextBlock(
                text = "----------------",
                alignment = "center",
            ),
            ThermalTextBlock(
                text = summary.customerName,
                alignment = "center",
                bold = true,
                size = "large",
            ),
            ThermalTextBlock(
                text = "Tel: ${summary.customerPhone}",
                alignment = "center",
                bold = true,
            ),
            ThermalTextBlock(
                text = "Valor: ${money(workOrder.totalValue)}",
                alignment = "center",
                bold = true,
            ),
            ThermalTextBlock(
                text = formatDate(workOrder.openedAt),
                alignment = "center",
                font = "B",
            ),
        )
    }

    suspend fun generateReceiptPrintContent(
        id: Long,
        headerTemplate: String,
        footerTemplate: String,
        companyName: String,
    ): ThermalPrintContent? {
        val workOrder = workOrderDao.findById(id) ?: return null
        val summary = workOrderDao.findSummaryById(id) ?: return null
        val tokens = workOrderTokens(workOrder, summary, companyName)
        return ThermalPrintContent(
            header = MessageTemplateRenderer.render(headerTemplate, tokens).trim(),
            body = buildString {
                appendLine("RECIBO")
                appendLine("OS: ${workOrder.numero}")
                appendLine("Cliente: ${summary.customerName}")
                appendLine("Telefone: ${summary.customerPhone}")
                appendLine("Data: ${formatDate(Clock.nowMillis())}")
                appendLine()
                appendLine("Recebemos o valor de")
                appendLine(money(workOrder.totalValue))
                appendLine("referente aos servicos da OS.")
                workOrder.observacoes?.takeIf { it.isNotBlank() }?.let {
                    appendLine()
                    appendLine("Obs: $it")
                }
            },
            footer = MessageTemplateRenderer.render(footerTemplate, tokens).trim(),
        )
    }

    suspend fun generateWarrantyPrintContent(
        id: Long,
        headerTemplate: String,
        footerTemplate: String,
        companyName: String,
        warrantyDays: Int?,
        warrantyTerms: String?,
    ): ThermalPrintContent? {
        val workOrder = workOrderDao.findById(id) ?: return null
        val summary = workOrderDao.findSummaryById(id) ?: return null
        val tokens = workOrderTokens(workOrder, summary, companyName)
        return ThermalPrintContent(
            header = MessageTemplateRenderer.render(headerTemplate, tokens).trim(),
            body = buildString {
                appendLine("GARANTIA")
                appendLine("OS: ${workOrder.numero}")
                appendLine("Cliente: ${summary.customerName}")
                appendLine("Telefone: ${summary.customerPhone}")
                appendLine("Data: ${formatDate(Clock.nowMillis())}")
                warrantyDays?.takeIf { it > 0 }?.let {
                    appendLine("Prazo: $it dias")
                }
                appendLine()
                appendLine(warrantyTerms?.takeIf { it.isNotBlank() } ?: "Garantia vinculada aos servicos descritos nesta OS.")
                appendLine()
                appendLine("Valor: ${money(workOrder.totalValue)}")
            },
            footer = MessageTemplateRenderer.render(footerTemplate, tokens).trim(),
        )
    }

    private fun workOrderTokens(
        workOrder: WorkOrderEntity,
        summary: WorkOrderSummary,
        companyName: String,
    ): Map<String, String> =
        mapOf(
            "empresa" to companyName,
            "data" to formatDate(workOrder.openedAt),
            "os" to workOrder.numero,
            "nome" to summary.customerName,
            "telefone" to summary.customerPhone,
            "valor" to money(workOrder.totalValue),
            "status" to workOrder.status,
        )

    private suspend fun generateWorkOrderNumber(nowMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val yearStart = date.withDayOfYear(1).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val yearEnd = date.withDayOfYear(date.toLocalDate().lengthOfYear())
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli() - 1
        val sequence = workOrderDao.countOpenedBetween(yearStart, yearEnd) + 1
        val datePart = date.format(DateTimeFormatter.ofPattern("yyMMdd"))
        return "$datePart${sequence.toString().padStart(4, '0')}"
    }

    private fun formatDate(value: Long): String {
        val date = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault())
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
    }

    private fun money(value: Double): String =
        NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
}
