package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import br.com.sos.osmobile.data.local.entity.SaleEntity
import br.com.sos.osmobile.data.local.entity.SaleItemEntity
import br.com.sos.osmobile.data.local.model.SaleSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query(
        """
        SELECT v.id_venda AS id, v.numero AS number, c.nome AS customerName,
            v.valor_total AS totalValue, v.valor_pago AS paidValue,
            v.forma_pagamento AS paymentMethod, v.status_fiscal AS fiscalStatus,
            v.data_criacao AS createdAt
        FROM vendas v
        INNER JOIN clientes c ON c.id_cliente = v.id_cliente
        ORDER BY v.data_criacao DESC
        """,
    )
    fun observeSummaries(): Flow<List<SaleSummary>>

    @Query("SELECT * FROM vendas ORDER BY data_criacao DESC")
    suspend fun listAll(): List<SaleEntity>

    @Query("SELECT * FROM itens_venda ORDER BY id_venda, id_item_venda")
    suspend fun listAllItems(): List<SaleItemEntity>

    @Query("SELECT COUNT(*) FROM vendas WHERE data_criacao BETWEEN :startMillis AND :endMillis")
    suspend fun countCreatedBetween(startMillis: Long, endMillis: Long): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(sale: SaleEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<SaleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(sales: List<SaleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackupItems(items: List<SaleItemEntity>)

    @Query("DELETE FROM itens_venda")
    suspend fun deleteAllItems()

    @Query("DELETE FROM vendas")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertWithItems(sale: SaleEntity, items: List<SaleItemEntity>): Long {
        val saleId = insert(sale)
        insertItems(items.map { it.copy(saleId = saleId) })
        return saleId
    }
}
