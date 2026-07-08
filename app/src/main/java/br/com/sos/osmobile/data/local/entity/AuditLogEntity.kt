package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "historico_sistema",
    indices = [
        Index(value = ["modulo"]),
        Index(value = ["tabela_afetada", "id_registro_afetado"]),
        Index(value = ["data_hora"]),
    ],
)
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_historico")
    val id: Long = 0,
    @ColumnInfo(name = "data_hora")
    val timestamp: Long,
    val usuario: String? = null,
    val modulo: String,
    val acao: String,
    @ColumnInfo(name = "id_registro_afetado")
    val affectedRecordId: Long? = null,
    @ColumnInfo(name = "tabela_afetada")
    val affectedTable: String? = null,
    val detalhes: String? = null,
)
