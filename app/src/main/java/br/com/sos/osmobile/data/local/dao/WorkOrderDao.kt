package br.com.sos.osmobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity
import br.com.sos.osmobile.data.local.model.DocumentItem
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkOrderDao {
    @Query("SELECT * FROM ordens_servico ORDER BY data_abertura DESC")
    fun observeAll(): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM ordens_servico ORDER BY data_abertura DESC")
    suspend fun listAll(): List<WorkOrderEntity>

    @Query("SELECT * FROM itens_os ORDER BY id_os, id_item_os")
    suspend fun listAllItems(): List<WorkOrderItemEntity>

    @Query("SELECT * FROM ordens_servico WHERE status = :status ORDER BY data_abertura DESC")
    fun observeByStatus(status: String): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM ordens_servico WHERE id_os = :id")
    suspend fun findById(id: Long): WorkOrderEntity?

    @Query(
        """
        SELECT
            sp.nome AS name,
            i.quantidade AS quantity,
            i.preco_unitario_praticado AS unitPrice,
            i.subtotal AS subtotal
        FROM itens_os i
        INNER JOIN servicos_produtos sp ON sp.id_servico_produto = i.id_servico_produto
        WHERE i.id_os = :workOrderId
        ORDER BY i.id_item_os
        """,
    )
    suspend fun findDocumentItems(workOrderId: Long): List<DocumentItem>

    @Query("SELECT * FROM itens_os WHERE id_os = :workOrderId ORDER BY id_item_os")
    suspend fun findItemsByWorkOrder(workOrderId: Long): List<WorkOrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(workOrder: WorkOrderEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackup(workOrders: List<WorkOrderEntity>)

    @Update
    suspend fun update(workOrder: WorkOrderEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<WorkOrderItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBackupItems(items: List<WorkOrderItemEntity>)

    @Query("DELETE FROM itens_os WHERE id_os = :workOrderId")
    suspend fun deleteItemsByWorkOrder(workOrderId: Long)

    @Query("DELETE FROM itens_os")
    suspend fun deleteAllItems()

    @Query("DELETE FROM ordens_servico")
    suspend fun deleteAll()

    @Query(
        """
        SELECT
            os.id_os AS id,
            os.numero AS number,
            c.nome AS customerName,
            c.telefone AS customerPhone,
            os.status AS status,
            os.valor_total AS totalValue,
            COUNT(i.id_item_os) AS itemCount,
            os.data_abertura AS openedAt
        FROM ordens_servico os
        INNER JOIN clientes c ON c.id_cliente = os.id_cliente
        LEFT JOIN itens_os i ON i.id_os = os.id_os
        GROUP BY os.id_os
        ORDER BY os.data_abertura DESC
        """,
    )
    fun observeSummaries(): Flow<List<WorkOrderSummary>>

    @Query(
        """
        SELECT
            os.id_os AS id,
            os.numero AS number,
            c.nome AS customerName,
            c.telefone AS customerPhone,
            os.status AS status,
            os.valor_total AS totalValue,
            COUNT(i.id_item_os) AS itemCount,
            os.data_abertura AS openedAt
        FROM ordens_servico os
        INNER JOIN clientes c ON c.id_cliente = os.id_cliente
        LEFT JOIN itens_os i ON i.id_os = os.id_os
        WHERE os.id_os = :id
        GROUP BY os.id_os
        """,
    )
    suspend fun findSummaryById(id: Long): WorkOrderSummary?

    @Query(
        """
        SELECT
            os.id_os AS id,
            os.numero AS number,
            c.nome AS customerName,
            c.telefone AS customerPhone,
            os.status AS status,
            os.valor_total AS totalValue,
            COUNT(i.id_item_os) AS itemCount,
            os.data_abertura AS openedAt
        FROM ordens_servico os
        INNER JOIN clientes c ON c.id_cliente = os.id_cliente
        LEFT JOIN itens_os i ON i.id_os = os.id_os
        WHERE os.id_cliente = :customerId
        GROUP BY os.id_os
        ORDER BY os.data_abertura DESC
        """,
    )
    suspend fun listSummariesByCustomer(customerId: Long): List<WorkOrderSummary>

    @Query("SELECT COUNT(*) FROM ordens_servico WHERE data_abertura BETWEEN :startMillis AND :endMillis")
    suspend fun countOpenedBetween(startMillis: Long, endMillis: Long): Int

    @Transaction
    suspend fun insertWithItems(workOrder: WorkOrderEntity, items: List<WorkOrderItemEntity>): Long {
        val workOrderId = insert(workOrder)
        insertItems(items.map { it.copy(workOrderId = workOrderId) })
        return workOrderId
    }

    @Transaction
    suspend fun updateWithItems(workOrder: WorkOrderEntity, items: List<WorkOrderItemEntity>) {
        update(workOrder)
        deleteItemsByWorkOrder(workOrder.id)
        insertItems(items.map { it.copy(workOrderId = workOrder.id) })
    }
}
