package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM configuracoes ORDER BY chave")
    fun observeAll(): Flow<List<AppSettingEntity>>

    @Query("SELECT * FROM configuracoes ORDER BY chave")
    suspend fun listAll(): List<AppSettingEntity>

    @Query("SELECT * FROM configuracoes WHERE chave = :key")
    suspend fun findByKey(key: String): AppSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: AppSettingEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(settings: List<AppSettingEntity>)

    @Query("DELETE FROM configuracoes WHERE chave LIKE 'contact_raw_id_customer_%'")
    suspend fun deleteContactLinks()
}
