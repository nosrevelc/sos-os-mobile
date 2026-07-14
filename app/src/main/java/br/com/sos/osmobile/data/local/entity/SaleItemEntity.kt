package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itens_venda",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id_venda"],
            childColumns = ["id_venda"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ServiceProductEntity::class,
            parentColumns = ["id_servico_produto"],
            childColumns = ["id_servico_produto"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["id_venda"]), Index(value = ["id_servico_produto"])],
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_item_venda")
    val id: Long = 0,
    @ColumnInfo(name = "id_venda")
    val saleId: Long,
    @ColumnInfo(name = "id_servico_produto")
    val serviceProductId: Long,
    val quantidade: Double,
    @ColumnInfo(name = "preco_unitario")
    val unitPrice: Double,
    val subtotal: Double,
)
