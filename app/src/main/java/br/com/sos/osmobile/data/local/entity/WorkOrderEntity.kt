package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ordens_servico",
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
data class WorkOrderEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_os")
    val id: Long = 0,
    val numero: String,
    @ColumnInfo(name = "id_cliente")
    val customerId: Long,
    @ColumnInfo(name = "data_abertura")
    val openedAt: Long,
    @ColumnInfo(name = "data_previsao_conclusao")
    val expectedConclusionAt: Long? = null,
    val status: String,
    val observacoes: String? = null,
    @ColumnInfo(name = "valor_total")
    val totalValue: Double = 0.0,
    @ColumnInfo(name = "valor_desconto")
    val discountValue: Double = 0.0,
    @ColumnInfo(name = "data_conclusao")
    val concludedAt: Long? = null,
    @ColumnInfo(name = "status_fiscal")
    val fiscalStatus: String = FiscalStatus.NOT_ISSUED,
    @ColumnInfo(name = "chave_fiscal")
    val fiscalKey: String? = null,
    @ColumnInfo(name = "protocolo_fiscal")
    val fiscalProtocol: String? = null,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)

object FiscalStatus {
    const val NOT_ISSUED = "Nao emitida"
    const val PENDING = "Em emissao"
    const val AUTHORIZED = "Autorizada"
    const val REJECTED = "Rejeitada"
    const val CANCELED = "Cancelada"
}
