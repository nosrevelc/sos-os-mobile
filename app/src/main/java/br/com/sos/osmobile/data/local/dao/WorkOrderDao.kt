package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderDao {
    @Query("SELECT * FROM ordens_servico ORDER BY data_abertura DESC")
    fun observeAll(): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM ordens_servico WHERE status = :status ORDER BY data_abertura DESC")
    fun observeByStatus(status: String): Flow<List<WorkOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(workOrder: WorkOrderEntity): Long

    @Update
    suspend fun update(workOrder: WorkOrderEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<WorkOrderItemEntity>)
}
