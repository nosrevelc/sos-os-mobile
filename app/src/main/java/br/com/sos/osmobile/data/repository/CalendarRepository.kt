package br.com.sos.osmobile.data.repository

import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import br.com.sos.osmobile.data.local.entity.AppointmentEntity
import br.com.sos.osmobile.data.local.entity.CalendarSyncStatus
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CALENDAR_ID_KEY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.TimeZone

data class CalendarAccount(
    val id: Long,
    val name: String,
    val accountName: String,
) {
    val label: String = if (accountName.isBlank()) name else "$name - $accountName"
}

class CalendarRepository(
    context: Context,
    private val settingsRepository: SettingsRepository,
) {
    private val contentResolver = context.applicationContext.contentResolver

    suspend fun listCalendars(): List<CalendarAccount> = withContext(Dispatchers.IO) {
        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
            ),
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CalendarAccount(
                            id = cursor.getLong(idIndex),
                            name = cursor.getString(nameIndex).orEmpty(),
                            accountName = cursor.getString(accountIndex).orEmpty(),
                        ),
                    )
                }
            }
        }.orEmpty()
    }

    suspend fun syncAppointment(appointment: AppointmentEntity, customerName: String, customerPhone: String): CalendarSyncUpdate =
        withContext(Dispatchers.IO) {
            val calendarId = settingsRepository.getString(CALENDAR_ID_KEY)?.toLongOrNull()
                ?: return@withContext CalendarSyncUpdate(null, CalendarSyncStatus.NOT_CONFIGURED, "Selecione uma agenda.")
            runCatching {
                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.DTSTART, appointment.startsAt)
                    put(CalendarContract.Events.DTEND, appointment.endsAt)
                    put(CalendarContract.Events.TITLE, "${appointment.tipo}: $customerName")
                    put(CalendarContract.Events.DESCRIPTION, buildString {
                        appendLine(appointment.titulo)
                        appendLine("Cliente: $customerName")
                        appendLine("Telefone: $customerPhone")
                        appointment.observacoes?.takeIf { it.isNotBlank() }?.let { appendLine("Obs: $it") }
                    })
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
                val eventId = appointment.calendarEventId
                if (eventId != null) {
                    contentResolver.update(
                        CalendarContract.Events.CONTENT_URI,
                        values,
                        "${CalendarContract.Events._ID} = ?",
                        arrayOf(eventId.toString()),
                    )
                    eventId
                } else {
                    contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                        ?.lastPathSegment
                        ?.toLongOrNull()
                        ?: error("Nao foi possivel criar evento.")
                }
            }.fold(
                onSuccess = { CalendarSyncUpdate(it, CalendarSyncStatus.SYNCED, null) },
                onFailure = { CalendarSyncUpdate(appointment.calendarEventId, CalendarSyncStatus.ERROR, it.message ?: "Falha no calendario.") },
            )
        }
}

data class CalendarSyncUpdate(
    val eventId: Long?,
    val status: String,
    val error: String?,
)
