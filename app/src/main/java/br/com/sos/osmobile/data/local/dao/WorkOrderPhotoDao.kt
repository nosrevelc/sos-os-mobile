package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.DriveSyncStatus
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity

@Dao
interface WorkOrderPhotoDao {
    @Query("SELECT * FROM fotos_os WHERE id_os = :workOrderId ORDER BY data_criacao DESC")
    suspend fun listByWorkOrder(workOrderId: Long): List<WorkOrderPhotoEntity>

    @Query("SELECT * FROM fotos_os WHERE id_os = :workOrderId ORDER BY data_criacao ASC")
    suspend fun listByWorkOrderAsc(workOrderId: Long): List<WorkOrderPhotoEntity>

    @Query("SELECT * FROM fotos_os ORDER BY id_os, data_criacao")
    suspend fun listAll(): List<WorkOrderPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: WorkOrderPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(photos: List<WorkOrderPhotoEntity>)

    @Query("SELECT * FROM fotos_os WHERE id_foto_os = :id")
    suspend fun findById(id: Long): WorkOrderPhotoEntity?

    @Query("SELECT * FROM fotos_os WHERE drive_sync_status != :syncedStatus ORDER BY data_criacao ASC")
    suspend fun listPendingDriveSync(syncedStatus: String = DriveSyncStatus.SYNCED): List<WorkOrderPhotoEntity>

    @Query("SELECT * FROM fotos_os WHERE id_os = :workOrderId AND drive_sync_status != :syncedStatus ORDER BY data_criacao ASC")
    suspend fun listPendingDriveSyncByWorkOrder(workOrderId: Long, syncedStatus: String = DriveSyncStatus.SYNCED): List<WorkOrderPhotoEntity>

    @Query(
        """
        UPDATE fotos_os
        SET drive_file_uri = :fileUri,
            drive_sync_status = :status,
            drive_sync_error = :error
        WHERE id_foto_os = :id
        """,
    )
    suspend fun updateDriveSync(id: Long, fileUri: String?, status: String, error: String?)

    @Query(
        """
        UPDATE fotos_os
        SET drive_file_uri = NULL,
            drive_sync_status = :status,
            drive_sync_error = NULL
        WHERE id_os = :workOrderId
        """,
    )
    suspend fun resetDriveSyncByWorkOrder(workOrderId: Long, status: String = DriveSyncStatus.PENDING)

    @Query("DELETE FROM fotos_os WHERE id_foto_os = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fotos_os")
    suspend fun deleteAll()
}
