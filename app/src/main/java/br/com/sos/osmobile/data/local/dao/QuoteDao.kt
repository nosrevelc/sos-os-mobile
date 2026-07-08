package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import br.com.sos.osmobile.data.local.entity.QuoteItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM orcamentos ORDER BY data_criacao DESC")
    fun observeAll(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM orcamentos WHERE status = :status ORDER BY data_criacao DESC")
    fun observeByStatus(status: String): Flow<List<QuoteEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(quote: QuoteEntity): Long

    @Update
    suspend fun update(quote: QuoteEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<QuoteItemEntity>)
}
