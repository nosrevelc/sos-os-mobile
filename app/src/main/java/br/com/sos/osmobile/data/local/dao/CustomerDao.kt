package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {
    @Query("SELECT * FROM clientes WHERE ativo = 1 ORDER BY nome")
    fun observeActive(): Flow<List<CustomerEntity>>

    @Query(
        """
        SELECT * FROM clientes
        WHERE (:includeInactive = 1 OR ativo = 1)
          AND (
            nome LIKE '%' || :query || '%'
            OR telefone LIKE '%' || :query || '%'
            OR cpf_cnpj LIKE '%' || :query || '%'
            OR email LIKE '%' || :query || '%'
            OR endereco LIKE '%' || :query || '%'
          )
        ORDER BY nome
        """,
    )
    fun search(query: String, includeInactive: Boolean): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM clientes WHERE id_cliente = :id")
    suspend fun findById(id: Long): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(customer: CustomerEntity): Long

    @Update
    suspend fun update(customer: CustomerEntity)

    @Query("UPDATE clientes SET ativo = 0, data_atualizacao = :updatedAt WHERE id_cliente = :id")
    suspend fun archive(id: Long, updatedAt: Long)
}
