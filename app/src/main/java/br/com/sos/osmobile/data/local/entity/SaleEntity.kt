package br.com.sos.osmobile.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vendas",
    foreignKeys = [
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id_cliente"],
            childColumns = ["id_cliente"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["numero"], unique = true), Index(value = ["id_cliente"])],
)
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id_venda")
    val id: Long = 0,
    val numero: String,
    @ColumnInfo(name = "id_cliente")
    val customerId: Long,
    @ColumnInfo(name = "valor_total")
    val totalValue: Double,
    @ColumnInfo(name = "valor_pago")
    val paidValue: Double,
    @ColumnInfo(name = "forma_pagamento")
    val paymentMethod: String,
    @ColumnInfo(name = "status_fiscal")
    val fiscalStatus: String = FiscalStatus.NOT_ISSUED,
    @ColumnInfo(name = "data_criacao")
    val createdAt: Long,
    @ColumnInfo(name = "data_atualizacao")
    val updatedAt: Long,
)
