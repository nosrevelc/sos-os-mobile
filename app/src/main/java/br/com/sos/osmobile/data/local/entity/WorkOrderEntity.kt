package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import br.com.sos.osmobile.data.model.DeliveryStatus
import br.com.sos.osmobile.data.model.DeliveryType

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
    @ColumnInfo(name = "tipo_entrega")
    val deliveryType: String = DeliveryType.PICKUP,
    @ColumnInfo(name = "status_entrega")
    val deliveryStatus: String = DeliveryStatus.WAITING_PICKUP,
    @ColumnInfo(name = "endereco_entrega")
    val deliveryAddress: String? = null,
    @ColumnInfo(name = "taxa_entrega")
    val deliveryFee: Double = 0.0,
    @ColumnInfo(name = "codigo_rastreio")
    val trackingCode: String? = null,
    @ColumnInfo(name = "observacoes_entrega")
    val deliveryNotes: String? = null,
    @ColumnInfo(name = "data_conclusao")
    val concludedAt: Long? = null,
    @ColumnInfo(name = "status_fiscal")
    val fiscalStatus: String = FiscalStatus.NOT_ISSUED,
    @ColumnInfo(name = "chave_fiscal")
    val fiscalKey: String? = null,
    @ColumnInfo(name = "protocolo_fiscal")
    val fiscalProtocol: String? = null,
    @ColumnInfo(name = "drive_folder_uri")
    val driveFolderUri: String? = null,
    @ColumnInfo(name = "drive_sync_status")
    val driveSyncStatus: String = DriveSyncStatus.PENDING,
    @ColumnInfo(name = "drive_sync_error")
    val driveSyncError: String? = null,
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

object DriveSyncStatus {
    const val PENDING = "Pendente"
    const val SYNCED = "Sincronizado"
    const val ERROR = "Erro"
    const val NOT_CONFIGURED = "Sem configuracao"
}
