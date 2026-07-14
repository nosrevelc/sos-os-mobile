package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimentacoes_estoque",
    foreignKeys = [
        ForeignKey(
            entity = ServiceProductEntity::class,
            parentColumns = ["id_servico_produto"],
            childColumns = ["id_servico_produto"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["id_servico_produto"]), Index(value = ["data_criacao"])],
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_movimentacao_estoque")
    val id: Long = 0,
    @ColumnInfo(name = "id_servico_produto")
    val serviceProductId: Long,
    val tipo: String,
    val quantidade: Double,
    val motivo: String? = null,
    @ColumnInfo(name = "id_os")
    val workOrderId: Long? = null,
    @ColumnInfo(name = "data_criacao")
    val createdAt: Long,
)

object StockMovementType {
    const val IN = "Entrada"
    const val OUT = "Saida"
    const val ADJUST = "Ajuste"

    val all = listOf(IN, OUT, ADJUST)
}
