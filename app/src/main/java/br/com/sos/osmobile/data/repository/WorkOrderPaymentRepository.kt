package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.WorkOrderPaymentDao
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import kotlinx.coroutines.flow.Flow

class WorkOrderPaymentRepository(
    private val paymentDao: WorkOrderPaymentDao,
    private val auditRepository: AuditRepository,
) {
    suspend fun listByWorkOrder(workOrderId: Long): List<WorkOrderPaymentEntity> =
        paymentDao.listByWorkOrder(workOrderId)

    fun observeAll(): Flow<List<WorkOrderPaymentEntity>> = paymentDao.observeAll()

    suspend fun addPayment(workOrderId: Long, value: Double, method: String, note: String?): Long {
        val payment = WorkOrderPaymentEntity(
            workOrderId = workOrderId,
            valor = value,
            forma = method.trim().ifBlank { "Nao informado" },
            observacao = note?.trim()?.takeIf { it.isNotBlank() },
            paidAt = Clock.nowMillis(),
        )
        val id = paymentDao.insert(payment)
        auditRepository.record("Financeiro", "Pagamento registrado", "ordens_servico", workOrderId, details = "${payment.forma}: ${payment.valor}")
        return id
    }

    suspend fun deletePayment(id: Long) {
        val payment = paymentDao.findById(id) ?: return
        paymentDao.deleteById(id)
        auditRepository.record("Financeiro", "Pagamento removido", "ordens_servico", payment.workOrderId, details = payment.valor.toString())
    }
}
