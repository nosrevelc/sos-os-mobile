package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "assinaturas_os",
    foreignKeys = [
        ForeignKey(
            entity = WorkOrderEntity::class,
            parentColumns = ["id_os"],
            childColumns = ["id_os"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["id_os"], unique = true),
    ],
)
data class WorkOrderSignatureEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_assinatura_os")
    val id: Long = 0,
    @ColumnInfo(name = "id_os")
    val workOrderId: Long,
    @ColumnInfo(name = "nome_arquivo")
    val fileName: String,
    @ColumnInfo(name = "caminho_relativo")
    val relativePath: String,
    @ColumnInfo(name = "nome_assinante")
    val signerName: String,
    @ColumnInfo(name = "data_criacao")
    val createdAt: Long,
)
