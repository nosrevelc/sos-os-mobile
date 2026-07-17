package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.AppointmentDao
import br.com.sos.osmobile.data.local.entity.AppointmentEntity
import br.com.sos.osmobile.data.local.entity.AppointmentStatus
import br.com.sos.osmobile.data.local.model.AppointmentSummary
import br.com.sos.osmobile.data.model.DeliveryStatus
import br.com.sos.osmobile.data.model.DeliveryType
import br.com.sos.osmobile.data.model.WorkOrderStatus
import kotlinx.coroutines.flow.Flow

class AppointmentRepository(
    private val appointmentDao: AppointmentDao,
    private val workOrderRepository: WorkOrderRepository,
    private val calendarRepository: CalendarRepository,
    private val auditRepository: AuditRepository,
) {
    fun observeSummaries(): Flow<List<AppointmentSummary>> = appointmentDao.observeSummaries()

    suspend fun findById(id: Long): AppointmentEntity? = appointmentDao.findById(id)

    suspend fun create(
        customerId: Long,
        title: String,
        type: String,
        startsAt: Long,
        endsAt: Long,
        notes: String?,
        customerName: String,
        customerPhone: String,
    ): Long {
        val now = Clock.nowMillis()
        val id = appointmentDao.insert(
            AppointmentEntity(
                customerId = customerId,
                titulo = title.trim(),
                tipo = type,
                startsAt = startsAt,
                endsAt = endsAt,
                observacoes = notes?.trim()?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now,
            ),
        )
        auditRepository.record("Agenda", "Agendamento criado", "agendamentos", id, details = title)
        syncCalendar(id, customerName, customerPhone)
        return id
    }

    suspend fun updateStatus(id: Long, status: String) {
        val current = appointmentDao.findById(id) ?: return
        val now = Clock.nowMillis()
        appointmentDao.update(current.copy(status = status, updatedAt = now))
        auditRepository.record("Agenda", "Status alterado", "agendamentos", id, details = "${current.status} -> $status")
    }

    suspend fun createWorkOrderFromAppointment(appointment: AppointmentSummary): Long {
        appointment.workOrderId?.let { return it }
        val id = workOrderRepository.create(
            customerId = appointment.customerId,
            status = WorkOrderStatus.Open.label,
            notes = "Criada a partir do agendamento ${appointment.title}",
            items = emptyList(),
            discountValue = 0.0,
            deliveryType = DeliveryType.PICKUP,
            deliveryStatus = DeliveryStatus.WAITING_PICKUP,
            deliveryAddress = null,
            deliveryFee = 0.0,
            trackingCode = null,
            deliveryNotes = null,
        )
        appointmentDao.setWorkOrder(appointment.id, id, Clock.nowMillis())
        updateStatus(appointment.id, AppointmentStatus.ATTENDED)
        auditRepository.record("Agenda", "OS criada pelo agendamento", "agendamentos", appointment.id, details = "OS $id")
        return id
    }

    suspend fun syncCalendar(id: Long, customerName: String, customerPhone: String) {
        val current = appointmentDao.findById(id) ?: return
        val result = calendarRepository.syncAppointment(current, customerName, customerPhone)
        appointmentDao.updateCalendarSync(id, result.eventId, result.status, result.error, Clock.nowMillis())
    }
}
