package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "garantias_os",
    foreignKeys = [
        ForeignKey(
            entity = WorkOrderEntity::class,
            parentColumns = ["id_os"],
            childColumns = ["id_os"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["id_os"], unique = true)],
)
data class WorkOrderWarrantyEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_garantia_os")
    val id: Long = 0,
    @ColumnInfo(name = "id_os")
    val workOrderId: Long,
    @ColumnInfo(name = "prazo_dias")
    val warrantyDays: Int,
    val termos: String,
    @ColumnInfo(name = "data_criacao")
    val createdAt: Long,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)
