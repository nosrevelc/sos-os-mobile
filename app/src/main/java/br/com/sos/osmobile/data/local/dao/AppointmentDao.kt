package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.sos.osmobile.data.local.entity.AppointmentEntity
import br.com.sos.osmobile.data.local.entity.CalendarSyncStatus
import br.com.sos.osmobile.data.local.model.AppointmentSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {
    @Query(
        """
        SELECT
            a.id_agendamento AS id,
            a.id_cliente AS customerId,
            c.nome AS customerName,
            c.telefone AS customerPhone,
            c.email AS customerEmail,
            a.id_os AS workOrderId,
            os.numero AS workOrderNumber,
            a.titulo AS title,
            a.tipo AS type,
            a.data_inicio AS startsAt,
            a.data_fim AS endsAt,
            a.status AS status,
            a.observacoes AS notes,
            a.calendar_sync_status AS calendarSyncStatus
        FROM agendamentos a
        INNER JOIN clientes c ON c.id_cliente = a.id_cliente
        LEFT JOIN ordens_servico os ON os.id_os = a.id_os
        ORDER BY a.data_inicio ASC
        """,
    )
    fun observeSummaries(): Flow<List<AppointmentSummary>>

    @Query("SELECT * FROM agendamentos WHERE id_agendamento = :id")
    suspend fun findById(id: Long): AppointmentEntity?

    @Query("SELECT * FROM agendamentos WHERE calendar_sync_status != :syncedStatus ORDER BY data_inicio ASC")
    suspend fun listPendingCalendarSync(syncedStatus: String = CalendarSyncStatus.SYNCED): List<AppointmentEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(appointment: AppointmentEntity): Long

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Query(
        """
        UPDATE agendamentos
        SET id_os = :workOrderId,
            data_atualizacao = :updatedAt
        WHERE id_agendamento = :id
        """,
    )
    suspend fun setWorkOrder(id: Long, workOrderId: Long, updatedAt: Long)

    @Query(
        """
        UPDATE agendamentos
        SET calendar_event_id = :eventId,
            calendar_sync_status = :status,
            calendar_sync_error = :error,
            data_atualizacao = :updatedAt
        WHERE id_agendamento = :id
        """,
    )
    suspend fun updateCalendarSync(id: Long, eventId: Long?, status: String, error: String?, updatedAt: Long)

    @Query("DELETE FROM agendamentos WHERE id_agendamento = :id")
    suspend fun deleteById(id: Long)
}
