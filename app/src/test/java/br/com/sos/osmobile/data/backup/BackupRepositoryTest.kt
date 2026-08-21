package br.com.sos.osmobile.data.backup

import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import br.com.sos.osmobile.data.model.QuoteStatus
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    private lateinit var sourceDatabase: br.com.sos.osmobile.core.database.AppDatabase
    private lateinit var targetDatabase: br.com.sos.osmobile.core.database.AppDatabase
    private lateinit var sourceRepository: BackupRepository
    private lateinit var targetRepository: BackupRepository

    @Before
    fun setUp() {
        sourceDatabase = TestFixtures.inMemoryDatabase()
        targetDatabase = TestFixtures.inMemoryDatabase()
        sourceRepository = backupRepository(sourceDatabase)
        targetRepository = backupRepository(targetDatabase)
    }

    @After
    fun tearDown() {
        sourceDatabase.close()
        targetDatabase.close()
    }

    private fun backupRepository(database: br.com.sos.osmobile.core.database.AppDatabase): BackupRepository =
        BackupRepository(
            database = database,
            context = androidx.test.core.app.ApplicationProvider.getApplicationContext(),
            settingsRepository = SettingsRepository(
                settingsDao = database.settingsDao(),
                auditRepository = TestFixtures.auditRepository(database),
            ),
        )

    private suspend fun seedSourceData(): Triple<Long, Long, Long> {
        val customerId = sourceDatabase.customerDao().insert(
            TestFixtures.customer(nome = "Maria Souza", telefone = "11988887777"),
        )
        val serviceId = sourceDatabase.serviceProductDao().insert(
            TestFixtures.serviceProduct(codigo = "SP-0042", nome = "Levantamento", unitPrice = 250.0),
        )
        val quoteId = sourceDatabase.quoteDao().insertWithItems(
            quote = TestFixtures.quote(
                numero = "2601010009",
                customerId = customerId,
                status = QuoteStatus.Approved.label,
                totalValue = 500.0,
                discountValue = 25.0,
                observacoes = "Cliente preferencial",
            ),
            items = listOf(TestFixtures.quoteItem(0, serviceId, 2.0, 250.0)),
        )
        val workOrderId = sourceDatabase.workOrderDao().insertWithItems(
            workOrder = br.com.sos.osmobile.data.local.entity.WorkOrderEntity(
                numero = "2601010001",
                customerId = customerId,
                openedAt = 5_000L,
                status = WorkOrderStatus.InProgress.label,
                observacoes = "Urgente",
                totalValue = 500.0,
                discountValue = 0.0,
                updatedAt = 5_000L,
            ),
            items = listOf(TestFixtures.quoteItem(0, serviceId, 2.0, 250.0).let {
                br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity(
                    workOrderId = 0,
                    serviceProductId = it.serviceProductId,
                    quantidade = it.quantidade,
                    practicedUnitPrice = it.practicedUnitPrice,
                    subtotal = it.subtotal,
                )
            }),
        )
        sourceDatabase.settingsDao().upsert(
            AppSettingEntity(chave = "empresa_nome", valor = "SOS Teste", updatedAt = 1L),
        )
        return Triple(customerId, quoteId, workOrderId)
    }

    @Test
    fun exportThenImportPreservesOperationalData() = runTest {
        val (customerId, quoteId, workOrderId) = seedSourceData()

        val json = sourceRepository.exportJson()
        val result = targetRepository.importJson(json)

        assertEquals(BackupImportResult(customers = 1, services = 1, quotes = 1, workOrders = 1), result)

        val customer = targetDatabase.customerDao().findById(customerId)
        assertNotNull(customer)
        assertEquals("Maria Souza", customer?.nome)

        val service = targetDatabase.serviceProductDao().listAll().first()
        assertEquals("SP-0042", service.codigo)
        assertEquals(250.0, service.unitPrice, 0.0)

        val quote = targetDatabase.quoteDao().findById(quoteId)
        assertEquals("2601010009", quote?.numero)
        assertEquals(QuoteStatus.Approved.label, quote?.status)
        assertEquals(500.0, quote?.totalValue ?: 0.0, 0.0)
        assertEquals(25.0, quote?.discountValue ?: 0.0, 0.0)
        assertEquals("Cliente preferencial", quote?.observacoes)

        val quoteItems = targetDatabase.quoteDao().findItemsByQuoteId(quoteId)
        assertEquals(1, quoteItems.size)
        assertEquals(500.0, quoteItems[0].subtotal, 0.0)

        val workOrder = targetDatabase.workOrderDao().findById(workOrderId)
        assertEquals("2601010001", workOrder?.numero)
        assertEquals(WorkOrderStatus.InProgress.label, workOrder?.status)
        assertEquals("Urgente", workOrder?.observacoes)

        val workOrderItems = targetDatabase.workOrderDao().findItemsByWorkOrder(workOrderId)
        assertEquals(1, workOrderItems.size)
        assertEquals(500.0, workOrderItems[0].subtotal, 0.0)
    }

    @Test
    fun exportIncludesPortableSettingsOnly() = runTest {
        seedSourceData()
        sourceDatabase.settingsDao().upsert(
            AppSettingEntity(chave = SettingsRepository.DRIVE_ROOT_URI_KEY, valor = "content://secreto", updatedAt = 1L),
        )
        sourceDatabase.settingsDao().upsert(
            AppSettingEntity(chave = "contact_raw_id_customer_7", valor = "42", updatedAt = 1L),
        )

        val json = sourceRepository.exportJson()

        assertTrue(!json.contains("content://secreto"))
        assertTrue(!json.contains("contact_raw_id_customer_7"))
        assertTrue(json.contains("empresa_nome"))
    }

    @Test
    fun importIntoPopulatedDatabaseUpsertsById() = runTest {
        seedSourceData()
        val json = sourceRepository.exportJson()
        targetRepository.importJson(json)

        val result = targetRepository.importJson(json)

        assertEquals(BackupImportResult(customers = 1, services = 1, quotes = 1, workOrders = 1), result)
        assertEquals(1, targetDatabase.customerDao().listAll().size)
        assertEquals(1, targetDatabase.workOrderDao().listAll().size)
    }
}
