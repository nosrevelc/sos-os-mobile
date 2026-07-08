package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itens_os",
    foreignKeys = [
        ForeignKey(
            entity = WorkOrderEntity::class,
            parentColumns = ["id_os"],
            childColumns = ["id_os"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ServiceProductEntity::class,
            parentColumns = ["id_servico_produto"],
            childColumns = ["id_servico_produto"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["id_os"]),
        Index(value = ["id_servico_produto"]),
    ],
)
data class WorkOrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_item_os")
    val id: Long = 0,
    @ColumnInfo(name = "id_os")
    val workOrderId: Long,
    @ColumnInfo(name = "id_servico_produto")
    val serviceProductId: Long,
    val quantidade: Double,
    @ColumnInfo(name = "preco_unitario_praticado")
    val practicedUnitPrice: Double,
    val subtotal: Double,
)
