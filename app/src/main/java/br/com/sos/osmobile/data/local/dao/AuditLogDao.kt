package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.AuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM historico_sistema ORDER BY data_hora DESC")
    fun observeRecent(): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * FROM historico_sistema
        WHERE tabela_afetada = :tableName AND id_registro_afetado = :recordId
        ORDER BY data_hora DESC
        """,
    )
    fun observeForRecord(tableName: String, recordId: Long): Flow<List<AuditLogEntity>>

    @Query(
        """
        SELECT * FROM historico_sistema
        WHERE tabela_afetada = :tableName AND id_registro_afetado = :recordId
        ORDER BY data_hora DESC
        """,
    )
    suspend fun listForRecord(tableName: String, recordId: Long): List<AuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(log: AuditLogEntity): Long

    @Query("DELETE FROM historico_sistema")
    suspend fun deleteAll()
}
