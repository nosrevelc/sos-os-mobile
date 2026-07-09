package br.com.sos.osmobile.data.repository

import androidx.room.withTransaction
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity
import br.com.sos.osmobile.data.model.QuoteStatus
import br.com.sos.osmobile.data.model.WorkOrderStatus
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed class QuoteConversionResult {
    data class Converted(val workOrderId: Long) : QuoteConversionResult()
    data object QuoteNotFound : QuoteConversionResult()
    data object QuoteNotApproved : QuoteConversionResult()
    data object QuoteWithoutItems : QuoteConversionResult()
}

class QuoteConversionRepository(
    private val database: AppDatabase,
    private val auditRepository: AuditRepository,
) {
    suspend fun convertApprovedQuoteToWorkOrder(quoteId: Long): QuoteConversionResult {
        var auditDetails: String? = null
        val result = database.withTransaction {
            val quoteDao = database.quoteDao()
            val workOrderDao = database.workOrderDao()
            val quote = quoteDao.findById(quoteId) ?: return@withTransaction QuoteConversionResult.QuoteNotFound

            if (quote.status != QuoteStatus.Approved.label) {
                return@withTransaction QuoteConversionResult.QuoteNotApproved
            }

            val quoteItems = quoteDao.findItemsByQuoteId(quoteId)
            if (quoteItems.isEmpty()) {
                return@withTransaction QuoteConversionResult.QuoteWithoutItems
            }

            val now = Clock.nowMillis()
            val workOrderNumber = generateWorkOrderNumber(now)
            val workOrderItems = quoteItems.map {
                WorkOrderItemEntity(
                    workOrderId = 0,
                    serviceProductId = it.serviceProductId,
                    quantidade = it.quantidade,
                    practicedUnitPrice = it.practicedUnitPrice,
                    subtotal = it.subtotal,
                )
            }
            val workOrderId = workOrderDao.insertWithItems(
                workOrder = WorkOrderEntity(
                    numero = workOrderNumber,
                    customerId = quote.customerId,
                    openedAt = now,
                    status = WorkOrderStatus.Open.label,
                    observacoes = quote.observacoes,
                    totalValue = quote.totalValue,
                    updatedAt = now,
                ),
                items = workOrderItems,
            )

            quoteDao.update(
                quote.copy(
                    status = QuoteStatus.Converted.label,
                    updatedAt = now,
                ),
            )

            auditDetails = "${quote.numero} -> $workOrderNumber"
            QuoteConversionResult.Converted(workOrderId)
        }

        if (result is QuoteConversionResult.Converted) {
            auditRepository.record(
                module = "Orcamentos",
                action = "Orcamento convertido em OS",
                table = "orcamentos",
                recordId = quoteId,
                details = auditDetails,
            )
            auditRepository.record(
                module = "Ordens de Servico",
                action = "OS criada por conversao de orcamento",
                table = "ordens_servico",
                recordId = result.workOrderId,
                details = auditDetails,
            )
        }

        return result
    }

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
        val sequence = database.workOrderDao().countOpenedBetween(yearStart, yearEnd) + 1
        val datePart = date.format(DateTimeFormatter.ofPattern("yyMMdd"))
        return "$datePart${sequence.toString().padStart(4, '0')}"
    }
}
