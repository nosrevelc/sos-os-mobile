package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.WorkOrderWarrantyEntity

@Dao
interface WorkOrderWarrantyDao {
    @Query("SELECT * FROM garantias_os WHERE id_os = :workOrderId LIMIT 1")
    suspend fun findByWorkOrder(workOrderId: Long): WorkOrderWarrantyEntity?

    @Query("SELECT * FROM garantias_os ORDER BY id_os")
    suspend fun listAll(): List<WorkOrderWarrantyEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(warranty: WorkOrderWarrantyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(warranties: List<WorkOrderWarrantyEntity>)

    @Query("DELETE FROM garantias_os WHERE id_os = :workOrderId")
    suspend fun deleteByWorkOrder(workOrderId: Long)

    @Query("DELETE FROM garantias_os")
    suspend fun deleteAll()
}
