package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orcamentos",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id_cliente"],
            childColumns = ["id_cliente"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["numero"], unique = true),
        Index(value = ["id_cliente"]),
        Index(value = ["status"]),
    ],
)
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_orcamento")
    val id: Long = 0,
    val numero: String,
    @ColumnInfo(name = "id_cliente")
    val customerId: Long,
    @ColumnInfo(name = "data_criacao")
    val createdAt: Long,
    @ColumnInfo(name = "data_validade")
    val validUntil: Long? = null,
    val status: String,
    val observacoes: String? = null,
    @ColumnInfo(name = "valor_total")
    val totalValue: Double = 0.0,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)
