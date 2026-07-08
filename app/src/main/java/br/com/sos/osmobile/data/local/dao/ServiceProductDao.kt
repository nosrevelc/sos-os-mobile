package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServiceProductDao {
    @Query("SELECT * FROM servicos_produtos WHERE ativo = 1 ORDER BY nome")
    fun observeActive(): Flow<List<ServiceProductEntity>>

    @Query(
        """
        SELECT * FROM servicos_produtos
        WHERE ativo = 1
          AND (nome LIKE '%' || :query || '%' OR descricao LIKE '%' || :query || '%' OR codigo LIKE '%' || :query || '%')
        ORDER BY nome
        """,
    )
    fun search(query: String): Flow<List<ServiceProductEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ServiceProductEntity): Long

    @Update
    suspend fun update(item: ServiceProductEntity)

    @Query("UPDATE servicos_produtos SET ativo = 0, data_atualizacao = :updatedAt WHERE id_servico_produto = :id")
    suspend fun archive(id: Long, updatedAt: Long)
}
