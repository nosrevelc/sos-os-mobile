package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pagamentos_os",
    foreignKeys = [
        ForeignKey(
            entity = WorkOrderEntity::class,
            parentColumns = ["id_os"],
            childColumns = ["id_os"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["id_os"])],
)
data class WorkOrderPaymentEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_pagamento_os")
    val id: Long = 0,
    @ColumnInfo(name = "id_os")
    val workOrderId: Long,
    val valor: Double,
    val forma: String,
    val observacao: String? = null,
    @ColumnInfo(name = "data_pagamento")
    val paidAt: Long,
)
