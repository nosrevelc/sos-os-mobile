package br.com.sos.osmobile.feature.appointments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.local.entity.AppointmentStatus
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.model.AppointmentSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.repository.AppointmentRepository
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.COMPANY_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_APPOINTMENT_CREATED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_APPOINTMENT_REMINDER_1D_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_APPOINTMENT_REMINDER_2D_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_APPOINTMENT_REMINDER_TODAY_KEY
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class AppointmentFormState(
    val selectedCustomerId: Long? = null,
    val title: String = "",
    val type: String = "Atendimento",
    val date: String = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
    val time: String = "09:00",
    val durationMinutes: String = "60",
    val notes: String = "",
    val message: String? = null,
)

data class AppointmentUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val appointments: List<AppointmentSummary> = emptyList(),
    val settings: Map<String, String> = emptyMap(),
)

class AppointmentViewModel(
    private val appointmentRepository: AppointmentRepository,
    private val customerRepository: CustomerRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    val uiState = combine(
        customerRepository.observeActive(),
        appointmentRepository.observeSummaries(),
        settingsRepository.observeAll(),
    ) { customers, appointments, settings ->
        AppointmentUiState(customers, appointments, settings.associate { it.chave to it.valor })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppointmentUiState())

    var form by mutableStateOf(AppointmentFormState())
        private set

    fun selectCustomer(id: Long) {
        form = form.copy(selectedCustomerId = id, message = null)
    }

    fun onTitleChanged(value: String) { form = form.copy(title = value, message = null) }
    fun onTypeChanged(value: String) { form = form.copy(type = value, message = null) }
    fun onDateChanged(value: String) { form = form.copy(date = value.take(10), message = null) }
    fun onTimeChanged(value: String) { form = form.copy(time = value.take(5), message = null) }
    fun onDurationChanged(value: String) { form = form.copy(durationMinutes = value.filter(Char::isDigit).take(4), message = null) }
    fun onNotesChanged(value: String) { form = form.copy(notes = value, message = null) }

    fun saveAppointment() {
        val customer = uiState.value.customers.firstOrNull { it.id == form.selectedCustomerId }
        if (customer == null) {
            form = form.copy(message = "Selecione um cliente.")
            return
        }
        val startsAt = parseDateTime(form.date, form.time)
        if (startsAt == null) {
            form = form.copy(message = "Informe data/hora validas.")
            return
        }
        val duration = form.durationMinutes.toLongOrNull()?.coerceAtLeast(15) ?: 60
        viewModelScope.launch {
            appointmentRepository.create(
                customerId = customer.id,
                title = form.title.ifBlank { form.type },
                type = form.type,
                startsAt = startsAt,
                endsAt = startsAt + duration * 60_000,
                notes = form.notes,
                customerName = customer.nome,
                customerPhone = customer.telefone,
            )
            form = AppointmentFormState(message = "Agendamento criado.")
        }
    }

    fun updateStatus(appointment: AppointmentSummary, status: String) {
        viewModelScope.launch { appointmentRepository.updateStatus(appointment.id, status) }
    }

    fun createWorkOrder(appointment: AppointmentSummary, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = appointmentRepository.createWorkOrderFromAppointment(appointment)
            onCreated(id)
        }
    }

    fun renderMessage(appointment: AppointmentSummary, key: String): String {
        val settings = uiState.value.settings
        val template = settings[key] ?: defaultTemplate(key)
        return MessageTemplateRenderer.render(template, appointmentTokens(appointment, settings[COMPANY_NAME_KEY].orEmpty()))
    }

    private fun defaultTemplate(key: String): String =
        when (key) {
            TEMPLATE_APPOINTMENT_CREATED_KEY -> MessageTemplateRenderer.appointmentCreatedTemplate
            TEMPLATE_APPOINTMENT_REMINDER_2D_KEY -> MessageTemplateRenderer.appointmentReminder2DaysTemplate
            TEMPLATE_APPOINTMENT_REMINDER_1D_KEY -> MessageTemplateRenderer.appointmentReminder1DayTemplate
            TEMPLATE_APPOINTMENT_REMINDER_TODAY_KEY -> MessageTemplateRenderer.appointmentReminderTodayTemplate
            else -> MessageTemplateRenderer.appointmentCreatedTemplate
        }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun parseDateTime(date: String, time: String): Long? =
            runCatching {
                LocalDate.parse(date, dateFormatter)
                    .atTime(LocalTime.parse(time, timeFormatter))
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }.getOrNull()

        fun appointmentTokens(appointment: AppointmentSummary, companyName: String): Map<String, String> {
            val start = java.time.Instant.ofEpochMilli(appointment.startsAt).atZone(ZoneId.systemDefault())
            return mapOf(
                "nome" to appointment.customerName,
                "telefone" to appointment.customerPhone,
                "empresa" to companyName,
                "os" to appointment.workOrderNumber.orEmpty(),
                "agendamento" to appointment.title,
                "agendamento_tipo" to appointment.type,
                "agendamento_status" to appointment.status,
                "agendamento_data" to start.format(dateFormatter),
                "agendamento_hora" to start.format(timeFormatter),
            )
        }

        fun factory(
            appointmentRepository: AppointmentRepository,
            customerRepository: CustomerRepository,
            settingsRepository: SettingsRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppointmentViewModel(appointmentRepository, customerRepository, settingsRepository) as T
            }
    }
}
