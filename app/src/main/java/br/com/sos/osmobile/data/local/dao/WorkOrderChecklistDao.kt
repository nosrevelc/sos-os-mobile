package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.sos.osmobile.data.local.entity.WorkOrderChecklistItemEntity

@Dao
interface WorkOrderChecklistDao {
    @Query("SELECT * FROM checklist_os WHERE id_os = :workOrderId ORDER BY id_checklist_os")
    suspend fun listByWorkOrder(workOrderId: Long): List<WorkOrderChecklistItemEntity>

    @Query("SELECT * FROM checklist_os ORDER BY id_os, id_checklist_os")
    suspend fun listAll(): List<WorkOrderChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: WorkOrderChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(items: List<WorkOrderChecklistItemEntity>)

    @Update
    suspend fun update(item: WorkOrderChecklistItemEntity)

    @Query("SELECT * FROM checklist_os WHERE id_checklist_os = :id")
    suspend fun findById(id: Long): WorkOrderChecklistItemEntity?

    @Query("DELETE FROM checklist_os WHERE id_checklist_os = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM checklist_os")
    suspend fun deleteAll()
}
