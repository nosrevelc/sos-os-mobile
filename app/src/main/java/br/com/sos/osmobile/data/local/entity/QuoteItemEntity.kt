package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "itens_orcamento",
    foreignKeys = [
        ForeignKey(
            entity = QuoteEntity::class,
            parentColumns = ["id_orcamento"],
            childColumns = ["id_orcamento"],
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
        Index(value = ["id_orcamento"]),
        Index(value = ["id_servico_produto"]),
    ],
)
data class QuoteItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_item_orcamento")
    val id: Long = 0,
    @ColumnInfo(name = "id_orcamento")
    val quoteId: Long,
    @ColumnInfo(name = "id_servico_produto")
    val serviceProductId: Long,
    val quantidade: Double,
    @ColumnInfo(name = "preco_unitario_praticado")
    val practicedUnitPrice: Double,
    val subtotal: Double,
)
