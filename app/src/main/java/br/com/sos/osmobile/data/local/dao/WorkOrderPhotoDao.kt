package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity

@Dao
interface WorkOrderPhotoDao {
    @Query("SELECT * FROM fotos_os WHERE id_os = :workOrderId ORDER BY data_criacao DESC")
    suspend fun listByWorkOrder(workOrderId: Long): List<WorkOrderPhotoEntity>

    @Query("SELECT * FROM fotos_os ORDER BY id_os, data_criacao")
    suspend fun listAll(): List<WorkOrderPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: WorkOrderPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(photos: List<WorkOrderPhotoEntity>)

    @Query("SELECT * FROM fotos_os WHERE id_foto_os = :id")
    suspend fun findById(id: Long): WorkOrderPhotoEntity?

    @Query("DELETE FROM fotos_os WHERE id_foto_os = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM fotos_os")
    suspend fun deleteAll()
}
