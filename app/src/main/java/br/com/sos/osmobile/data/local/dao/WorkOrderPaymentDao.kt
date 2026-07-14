package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderPaymentDao {
    @Query("SELECT * FROM pagamentos_os WHERE id_os = :workOrderId ORDER BY data_pagamento DESC")
    suspend fun listByWorkOrder(workOrderId: Long): List<WorkOrderPaymentEntity>

    @Query("SELECT * FROM pagamentos_os ORDER BY id_os, data_pagamento")
    suspend fun listAll(): List<WorkOrderPaymentEntity>

    @Query("SELECT * FROM pagamentos_os ORDER BY data_pagamento DESC")
    fun observeAll(): Flow<List<WorkOrderPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: WorkOrderPaymentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(payments: List<WorkOrderPaymentEntity>)

    @Query("SELECT * FROM pagamentos_os WHERE id_pagamento_os = :id")
    suspend fun findById(id: Long): WorkOrderPaymentEntity?

    @Query("DELETE FROM pagamentos_os WHERE id_pagamento_os = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pagamentos_os")
    suspend fun deleteAll()
}
