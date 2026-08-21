package br.com.sos.osmobile.testing

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import br.com.sos.osmobile.data.local.entity.QuoteItemEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.WorkOrderItemInput
import kotlinx.coroutines.runBlocking

object TestFixtures {

    fun inMemoryDatabase(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()

    fun customer(
        nome: String = "Cliente Teste",
        telefone: String = "11999990000",
        now: Long = 1_000L,
    ): CustomerEntity = CustomerEntity(
        nome = nome,
        telefone = telefone,
        createdAt = now,
        updatedAt = now,
    )

    fun serviceProduct(
        codigo: String = "SP-0001",
        nome: String = "Servico Teste",
        unitPrice: Double = 100.0,
        now: Long = 1_000L,
    ): ServiceProductEntity = ServiceProductEntity(
        codigo = codigo,
        nome = nome,
        unitPrice = unitPrice,
        createdAt = now,
        updatedAt = now,
    )

    fun quote(
        numero: String,
        customerId: Long,
        status: String,
        totalValue: Double,
        discountValue: Double = 0.0,
        observacoes: String? = null,
        now: Long = 2_000L,
    ): QuoteEntity = QuoteEntity(
        numero = numero,
        customerId = customerId,
        createdAt = now,
        status = status,
        observacoes = observacoes,
        totalValue = totalValue,
        discountValue = discountValue,
        updatedAt = now,
    )

    fun quoteItem(
        quoteId: Long,
        serviceProductId: Long,
        quantidade: Double,
        practicedUnitPrice: Double,
    ): QuoteItemEntity = QuoteItemEntity(
        quoteId = quoteId,
        serviceProductId = serviceProductId,
        quantidade = quantidade,
        practicedUnitPrice = practicedUnitPrice,
        subtotal = quantidade * practicedUnitPrice,
    )

    suspend fun seedCustomerAndService(database: AppDatabase): Pair<Long, Long> = runBlocking {
        val customerId = database.customerDao().insert(customer())
        val serviceId = database.serviceProductDao().insert(serviceProduct())
        customerId to serviceId
    }

    fun auditRepository(database: AppDatabase): AuditRepository =
        AuditRepository(database.auditLogDao())

    fun workOrderInputs(vararg quantitiesAndPrices: Pair<Double, Double>): List<WorkOrderItemInput> =
        quantitiesAndPrices.map { WorkOrderItemInput(1L, it.first, it.second) }
}
