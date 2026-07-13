package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity

@Dao
interface WorkOrderSignatureDao {
    @Query("SELECT * FROM assinaturas_os WHERE id_os = :workOrderId LIMIT 1")
    suspend fun findByWorkOrder(workOrderId: Long): WorkOrderSignatureEntity?

    @Query("SELECT * FROM assinaturas_os ORDER BY id_os")
    suspend fun listAll(): List<WorkOrderSignatureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(signature: WorkOrderSignatureEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(signatures: List<WorkOrderSignatureEntity>)

    @Query("DELETE FROM assinaturas_os WHERE id_os = :workOrderId")
    suspend fun deleteByWorkOrder(workOrderId: Long)

    @Query("DELETE FROM assinaturas_os")
    suspend fun deleteAll()
}
