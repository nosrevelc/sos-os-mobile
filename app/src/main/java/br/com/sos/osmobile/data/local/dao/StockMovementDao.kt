package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.sos.osmobile.data.local.entity.StockMovementEntity
import br.com.sos.osmobile.data.local.model.ServiceProductStockSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface StockMovementDao {
    @Query("SELECT * FROM movimentacoes_estoque ORDER BY data_criacao DESC")
    suspend fun listAll(): List<StockMovementEntity>

    @Query("SELECT * FROM movimentacoes_estoque WHERE id_servico_produto = :serviceProductId ORDER BY data_criacao DESC")
    fun observeByServiceProduct(serviceProductId: Long): Flow<List<StockMovementEntity>>

    @Query(
        """
        SELECT id_servico_produto AS id,
            COALESCE(SUM(
                CASE
                    WHEN tipo = 'Entrada' THEN quantidade
                    WHEN tipo = 'Saida' THEN -quantidade
                    ELSE quantidade
                END
            ), 0.0) AS saldo
        FROM movimentacoes_estoque
        GROUP BY id_servico_produto
        """,
    )
    fun observeSummaries(): Flow<List<ServiceProductStockSummary>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(movement: StockMovementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(movements: List<StockMovementEntity>)

    @Query("DELETE FROM movimentacoes_estoque")
    suspend fun deleteAll()
}
