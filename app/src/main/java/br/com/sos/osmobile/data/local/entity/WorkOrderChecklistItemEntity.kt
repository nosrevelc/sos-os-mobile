package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_os",
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
data class WorkOrderChecklistItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_checklist_os")
    val id: Long = 0,
    @ColumnInfo(name = "id_os")
    val workOrderId: Long,
    val descricao: String,
    val concluido: Boolean = false,
    @ColumnInfo(name = "data_criacao")
    val createdAt: Long,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)
