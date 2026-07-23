package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.DriveSyncStatus
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity

@Dao
interface WorkOrderSignatureDao {
    @Query("SELECT * FROM assinaturas_os WHERE id_os = :workOrderId LIMIT 1")
    suspend fun findByWorkOrder(workOrderId: Long): WorkOrderSignatureEntity?

    @Query("SELECT * FROM assinaturas_os ORDER BY id_os")
    suspend fun listAll(): List<WorkOrderSignatureEntity>

    @Query("SELECT * FROM assinaturas_os WHERE drive_sync_status != :syncedStatus ORDER BY data_criacao ASC")
    suspend fun listPendingDriveSync(syncedStatus: String = DriveSyncStatus.SYNCED): List<WorkOrderSignatureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(signature: WorkOrderSignatureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(signatures: List<WorkOrderSignatureEntity>)

    @Query(
        """
        UPDATE assinaturas_os
        SET drive_file_uri = :fileUri,
            drive_sync_status = :status,
            drive_sync_error = :error
        WHERE id_assinatura_os = :id
        """,
    )
    suspend fun updateDriveSync(id: Long, fileUri: String?, status: String, error: String?)

    @Query(
        """
        UPDATE assinaturas_os
        SET drive_file_uri = NULL,
            drive_sync_status = :status,
            drive_sync_error = NULL
        WHERE id_os = :workOrderId
        """,
    )
    suspend fun resetDriveSyncByWorkOrder(workOrderId: Long, status: String = DriveSyncStatus.PENDING)

    @Query("DELETE FROM assinaturas_os WHERE id_os = :workOrderId")
    suspend fun deleteByWorkOrder(workOrderId: Long)

    @Query("DELETE FROM assinaturas_os")
    suspend fun deleteAll()
}
