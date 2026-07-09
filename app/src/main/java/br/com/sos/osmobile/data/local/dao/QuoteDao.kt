package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import br.com.sos.osmobile.data.local.entity.QuoteItemEntity
import br.com.sos.osmobile.data.local.model.QuoteSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM orcamentos ORDER BY data_criacao DESC")
    fun observeAll(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM orcamentos WHERE status = :status ORDER BY data_criacao DESC")
    fun observeByStatus(status: String): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM orcamentos WHERE id_orcamento = :id")
    suspend fun findById(id: Long): QuoteEntity?

    @Query("SELECT * FROM itens_orcamento WHERE id_orcamento = :quoteId")
    suspend fun findItemsByQuoteId(quoteId: Long): List<QuoteItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(quote: QuoteEntity): Long

    @Update
    suspend fun update(quote: QuoteEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<QuoteItemEntity>)

    @Query(
        """
        SELECT
            o.id_orcamento AS id,
            o.numero AS number,
            c.nome AS customerName,
            o.status AS status,
            o.valor_total AS totalValue,
            COUNT(i.id_item_orcamento) AS itemCount,
            o.data_criacao AS createdAt
        FROM orcamentos o
        INNER JOIN clientes c ON c.id_cliente = o.id_cliente
        LEFT JOIN itens_orcamento i ON i.id_orcamento = o.id_orcamento
        GROUP BY o.id_orcamento
        ORDER BY o.data_criacao DESC
        """,
    )
    fun observeSummaries(): Flow<List<QuoteSummary>>

    @Query("SELECT COUNT(*) FROM orcamentos WHERE data_criacao BETWEEN :startMillis AND :endMillis")
    suspend fun countCreatedBetween(startMillis: Long, endMillis: Long): Int

    @Transaction
    suspend fun insertWithItems(quote: QuoteEntity, items: List<QuoteItemEntity>): Long {
        val quoteId = insert(quote)
        insertItems(items.map { it.copy(quoteId = quoteId) })
        return quoteId
    }
}
