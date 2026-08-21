package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.format.Formatters
import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.document.ServiceDocumentGenerator
import br.com.sos.osmobile.data.local.dao.QuoteDao
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import br.com.sos.osmobile.data.local.entity.QuoteItemEntity
import br.com.sos.osmobile.data.local.model.DocumentItem
import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.model.QuoteStatus
import br.com.sos.osmobile.data.print.ThermalPrintContent
import kotlinx.coroutines.flow.Flow
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class QuoteItemInput(
    val serviceProductId: Long,
    val quantity: Double,
    val practicedUnitPrice: Double,
)

class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val auditRepository: AuditRepository,
) {
    fun observeAll(): Flow<List<QuoteEntity>> = quoteDao.observeAll()

    fun observeSummaries(): Flow<List<QuoteSummary>> = quoteDao.observeSummaries()

    suspend fun findSummaryById(id: Long): QuoteSummary? = quoteDao.findSummaryById(id)

    suspend fun findById(id: Long): QuoteEntity? = quoteDao.findById(id)

    suspend fun listSummariesByCustomer(customerId: Long): List<QuoteSummary> =
        quoteDao.listSummariesByCustomer(customerId)

    suspend fun listDocumentItems(id: Long): List<DocumentItem> = quoteDao.findDocumentItems(id)

    suspend fun listItems(id: Long): List<QuoteItemEntity> = quoteDao.findItemsByQuoteId(id)

    suspend fun create(
        customerId: Long,
        status: String,
        notes: String?,
        items: List<QuoteItemInput>,
        discountValue: Double,
        minimumDepositValue: Double,
    ): Long {
        val now = Clock.nowMillis()
        val number = generateQuoteNumber(now)
        val quoteItems = items.map {
            QuoteItemEntity(
                quoteId = 0,
                serviceProductId = it.serviceProductId,
                quantidade = it.quantity,
                practicedUnitPrice = it.practicedUnitPrice,
                subtotal = it.quantity * it.practicedUnitPrice,
            )
        }
        val subtotal = quoteItems.sumOf { it.subtotal }
        val discount = discountValue.coerceIn(0.0, subtotal)
        val total = (subtotal - discount).coerceAtLeast(0.0)
        val minimumDeposit = minimumDepositValue.coerceIn(0.0, total)
        val id = quoteDao.insertWithItems(
            quote = QuoteEntity(
                numero = number,
                customerId = customerId,
                createdAt = now,
                status = status,
                observacoes = notes?.trim()?.takeIf { it.isNotBlank() },
                totalValue = total,
                discountValue = discount,
                minimumDepositValue = minimumDeposit,
                updatedAt = now,
            ),
            items = quoteItems,
        )
        auditRepository.record("Orcamentos", "Orcamento criado", "orcamentos", id, details = number)
        return id
    }

    suspend fun updateStatus(id: Long, status: QuoteStatus) {
        val current = quoteDao.findById(id) ?: return
        quoteDao.update(current.copy(status = status.label, updatedAt = Clock.nowMillis()))
        auditRepository.record("Orcamentos", "Status do orcamento alterado", "orcamentos", id, details = status.label)
    }

    suspend fun updateContent(
        id: Long,
        customerId: Long,
        status: QuoteStatus,
        notes: String?,
        items: List<QuoteItemInput>,
        discountValue: Double,
        minimumDepositValue: Double,
    ): Boolean {
        val current = quoteDao.findById(id) ?: return false
        if (current.status == QuoteStatus.Converted.label) {
            return false
        }
        val quoteItems = items.map {
            QuoteItemEntity(
                quoteId = id,
                serviceProductId = it.serviceProductId,
                quantidade = it.quantity,
                practicedUnitPrice = it.practicedUnitPrice,
                subtotal = it.quantity * it.practicedUnitPrice,
            )
        }
        val subtotal = quoteItems.sumOf { it.subtotal }
        val discount = discountValue.coerceIn(0.0, subtotal)
        val total = (subtotal - discount).coerceAtLeast(0.0)
        val minimumDeposit = minimumDepositValue.coerceIn(0.0, total)
        quoteDao.updateWithItems(
            quote = current.copy(
                customerId = customerId,
                status = status.label,
                observacoes = notes?.trim()?.takeIf { it.isNotBlank() },
                totalValue = total,
                discountValue = discount,
                minimumDepositValue = minimumDeposit,
                updatedAt = Clock.nowMillis(),
            ),
            items = quoteItems,
        )
        auditRepository.record("Orcamentos", "Orcamento editado", "orcamentos", id, details = current.numero)
        return true
    }

    suspend fun generateDocumentText(id: Long): String? {
        val quote = quoteDao.findById(id) ?: return null
        val summary = quoteDao.findSummaryById(id) ?: return null
        val items = quoteDao.findDocumentItems(id)
        return ServiceDocumentGenerator.generate(
            title = "ORCAMENTO",
            number = quote.numero,
            customerName = summary.customerName,
            status = quote.status,
            totalValue = quote.totalValue,
            discountValue = quote.discountValue,
            minimumDepositValue = quote.minimumDepositValue,
            notes = quote.observacoes,
            items = items,
        )
    }

    suspend fun generateThermalPrintContent(
        id: Long,
        headerTemplate: String,
        footerTemplate: String,
        companyName: String,
    ): ThermalPrintContent? {
        val quote = quoteDao.findById(id) ?: return null
        val summary = quoteDao.findSummaryById(id) ?: return null
        val tokens = mapOf(
            "empresa" to companyName,
            "data" to Formatters.dateTime(quote.createdAt),
            "os" to "",
            "orcamento" to quote.numero,
            "nome" to summary.customerName,
            "telefone" to summary.customerPhone,
            "valor" to Formatters.currency(quote.totalValue),
            "sinal_minimo" to Formatters.currency(quote.minimumDepositValue),
            "linha_sinal_minimo" to if (quote.minimumDepositValue > 0.0) "Sinal minimo: ${Formatters.currency(quote.minimumDepositValue)}" else "",
            "status" to quote.status,
        )
        return ThermalPrintContent(
            header = MessageTemplateRenderer.render(headerTemplate, tokens).trim(),
            body = ServiceDocumentGenerator.generate(
                title = "ORCAMENTO",
                number = quote.numero,
                customerName = summary.customerName,
                status = quote.status,
                totalValue = quote.totalValue,
                discountValue = quote.discountValue,
                minimumDepositValue = quote.minimumDepositValue,
                notes = quote.observacoes,
                items = quoteDao.findDocumentItems(id),
            ),
            footer = MessageTemplateRenderer.render(footerTemplate, tokens).trim(),
        )
    }

    private suspend fun generateQuoteNumber(nowMillis: Long): String {
        val zone = ZoneId.systemDefault()
        val date = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val yearStart = date.withDayOfYear(1).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val yearEnd = date.withDayOfYear(date.toLocalDate().lengthOfYear())
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli() - 1
        val sequence = quoteDao.countCreatedBetween(yearStart, yearEnd) + 1
        val datePart = date.format(DateTimeFormatter.ofPattern("yyMMdd"))
        return "OR$datePart${sequence.toString().padStart(4, '0')}"
    }


}
