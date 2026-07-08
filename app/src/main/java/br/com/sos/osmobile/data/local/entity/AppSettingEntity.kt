package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuracoes")
data class AppSettingEntity(
    @PrimaryKey
    val chave: String,
    val valor: String,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)
