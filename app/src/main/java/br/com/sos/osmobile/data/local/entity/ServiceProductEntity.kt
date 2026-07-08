package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "servicos_produtos",
    indices = [
        Index(value = ["codigo"], unique = true),
        Index(value = ["nome"]),
        Index(value = ["categoria"]),
    ],
)
data class ServiceProductEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_servico_produto")
    val id: Long = 0,
    val codigo: String,
    val nome: String,
    val categoria: String? = null,
    val descricao: String? = null,
    @ColumnInfo(name = "preco_unitario")
    val unitPrice: Double,
    val ativo: Boolean = true,
    @ColumnInfo(name = "data_cadastro")
    val createdAt: Long,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)
