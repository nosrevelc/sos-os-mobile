package br.com.sos.osmobile.feature.appointments

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.sos.osmobile.data.local.entity.AppointmentStatus
import br.com.sos.osmobile.data.local.model.AppointmentSummary
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.ui.components.CustomerSearchSelector
import br.com.sos.osmobile.ui.components.MessageActionButtons
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AppointmentScreen(
    viewModel: AppointmentViewModel,
    onOpenWorkOrder: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result[Manifest.permission.READ_CALENDAR] == true && result[Manifest.permission.WRITE_CALENDAR] == true) {
            viewModel.loadCalendars()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Novo agendamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            CustomerSearchSelector(
                customers = uiState.customers,
                selectedCustomerId = viewModel.form.selectedCustomerId,
                onCustomerSelected = viewModel::selectCustomer,
                emptyText = "Cadastre um cliente antes de agendar.",
            )
        }
        item {
            OutlinedTextField(
                value = viewModel.form.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("Titulo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.form.type,
                onValueChange = viewModel::onTypeChanged,
                label = { Text("Tipo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = viewModel.form.date,
                    onValueChange = viewModel::onDateChanged,
                    label = { Text("Data") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = viewModel.form.time,
                    onValueChange = viewModel::onTimeChanged,
                    label = { Text("Hora") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = viewModel.form.durationMinutes,
                onValueChange = viewModel::onDurationChanged,
                label = { Text("Duracao em minutos") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = viewModel.form.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("Observacoes") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = viewModel::saveAppointment, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("Salvar agendamento")
            }
            viewModel.form.message?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        }
        item {
            Text("Calendario Android/Google", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedButton(
                onClick = {
                    val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                    if (hasRead && hasWrite) viewModel.loadCalendars() else calendarPermissionLauncher.launch(
                        arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Event, contentDescription = null)
                Text("Buscar agendas do aparelho")
            }
            viewModel.calendars.forEach { calendar ->
                AssistChip(onClick = { viewModel.selectCalendar(calendar.id) }, label = { Text(calendar.label) })
            }
            viewModel.calendarMessage?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
        }
        item {
            Text("Proximos agendamentos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (uiState.appointments.isEmpty()) {
            item { Text("Nenhum agendamento cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(uiState.appointments, key = { it.id }) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    viewModel = viewModel,
                    onOpenWorkOrder = onOpenWorkOrder,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppointmentCard(
    appointment: AppointmentSummary,
    viewModel: AppointmentViewModel,
    onOpenWorkOrder: (Long) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${formatDateTime(appointment.startsAt)} - ${appointment.customerName}", fontWeight = FontWeight.SemiBold)
            Text("${appointment.type} | ${appointment.status} | Calendario: ${appointment.calendarSyncStatus}", style = MaterialTheme.typography.bodySmall)
            appointment.workOrderNumber?.let {
                Text(
                    "OS $it",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { appointment.workOrderId?.let(onOpenWorkOrder) },
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppointmentStatus.entries.forEach { status ->
                    AssistChip(onClick = { viewModel.updateStatus(appointment, status) }, label = { Text(status) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.createWorkOrder(appointment, onOpenWorkOrder) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Assignment, contentDescription = null)
                    Text(if (appointment.workOrderId == null) "Criar OS" else "Abrir OS")
                }
            }
            Text("Mensagens", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            MessageBlock("Criado", SettingsRepository.TEMPLATE_APPOINTMENT_CREATED_KEY, appointment, viewModel)
            MessageBlock("Lembrete 2 dias", SettingsRepository.TEMPLATE_APPOINTMENT_REMINDER_2D_KEY, appointment, viewModel)
            MessageBlock("Lembrete 1 dia", SettingsRepository.TEMPLATE_APPOINTMENT_REMINDER_1D_KEY, appointment, viewModel)
            MessageBlock("Lembrete hoje", SettingsRepository.TEMPLATE_APPOINTMENT_REMINDER_TODAY_KEY, appointment, viewModel)
        }
    }
}

@Composable
private fun MessageBlock(label: String, key: String, appointment: AppointmentSummary, viewModel: AppointmentViewModel) {
    val text = viewModel.renderMessage(appointment, key)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        MessageActionButtons(
            phone = appointment.customerPhone,
            email = appointment.customerEmail,
            subject = label,
            text = text,
        )
    }
}

private fun formatDateTime(value: Long): String =
    Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
