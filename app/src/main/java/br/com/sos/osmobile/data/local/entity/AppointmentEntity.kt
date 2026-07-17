package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "agendamentos",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id_cliente"],
            childColumns = ["id_cliente"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["id_cliente"]),
        Index(value = ["id_os"]),
        Index(value = ["data_inicio"]),
        Index(value = ["status"]),
    ],
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_agendamento")
    val id: Long = 0,
    @ColumnInfo(name = "id_cliente")
    val customerId: Long,
    @ColumnInfo(name = "id_os")
    val workOrderId: Long? = null,
    val titulo: String,
    val tipo: String,
    @ColumnInfo(name = "data_inicio")
    val startsAt: Long,
    @ColumnInfo(name = "data_fim")
    val endsAt: Long,
    val status: String = AppointmentStatus.SCHEDULED,
    val observacoes: String? = null,
    @ColumnInfo(name = "calendar_event_id")
    val calendarEventId: Long? = null,
    @ColumnInfo(name = "calendar_sync_status")
    val calendarSyncStatus: String = CalendarSyncStatus.PENDING,
    @ColumnInfo(name = "calendar_sync_error")
    val calendarSyncError: String? = null,
    @ColumnInfo(name = "data_criacao")
    val createdAt: Long,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)

object AppointmentStatus {
    const val SCHEDULED = "Agendado"
    const val CONFIRMED = "Confirmado"
    const val ATTENDED = "Compareceu"
    const val NO_SHOW = "Nao compareceu"
    const val RESCHEDULED = "Remarcado"
    const val CANCELED = "Cancelado"
    const val COMPLETED = "Concluido"

    val entries = listOf(SCHEDULED, CONFIRMED, ATTENDED, NO_SHOW, RESCHEDULED, CANCELED, COMPLETED)
}

object CalendarSyncStatus {
    const val PENDING = "Pendente"
    const val SYNCED = "Sincronizado"
    const val ERROR = "Erro"
    const val NOT_CONFIGURED = "Sem configuracao"
}
