package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "clientes",
    indices = [
        Index(value = ["nome"]),
        Index(value = ["telefone"]),
        Index(value = ["cpf_cnpj"], unique = true),
    ],
)
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_cliente")
    val id: Long = 0,
    val nome: String,
    @ColumnInfo(name = "cpf_cnpj")
    val cpfCnpj: String? = null,
    val telefone: String,
    val email: String? = null,
    val endereco: String? = null,
    val observacoes: String? = null,
    val ativo: Boolean = true,
    @ColumnInfo(name = "data_cadastro")
    val createdAt: Long,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)
