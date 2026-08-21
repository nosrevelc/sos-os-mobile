package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.data.model.QuoteStatus
import br.com.sos.osmobile.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class QuoteConversionRepositoryTest {

    private lateinit var database: androidx.room.RoomDatabase
    private lateinit var repository: QuoteConversionRepository

    @Before
    fun setUp() {
        val db = TestFixtures.inMemoryDatabase()
        database = db
        repository = QuoteConversionRepository(db, TestFixtures.auditRepository(db))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun convertsApprovedQuoteIntoWorkOrder() = runTest {
        val db = database as br.com.sos.osmobile.core.database.AppDatabase
        val (customerId, serviceId) = TestFixtures.seedCustomerAndService(db)
        val quoteId = db.quoteDao().insertWithItems(
            quote = TestFixtures.quote(
                numero = "2601010001",
                customerId = customerId,
                status = QuoteStatus.Approved.label,
                totalValue = 130.0,
                discountValue = 10.0,
                observacoes = "Entregar na portaria",
            ),
            items = listOf(
                TestFixtures.quoteItem(0, serviceId, 2.0, 50.0),
                TestFixtures.quoteItem(0, serviceId, 1.0, 30.0),
            ),
        )

        val result = repository.convertApprovedQuoteToWorkOrder(quoteId)

        assertTrue(result is QuoteConversionResult.Converted)
        val workOrderId = (result as QuoteConversionResult.Converted).workOrderId
        val workOrder = db.workOrderDao().findById(workOrderId)
        assertNotNull(workOrder)
        assertEquals(customerId, workOrder?.customerId)
        assertEquals("Aberta", workOrder?.status)
        assertEquals(130.0, workOrder?.totalValue ?: 0.0, 0.0)
        assertEquals(10.0, workOrder?.discountValue ?: 0.0, 0.0)
        assertEquals("Entregar na portaria", workOrder?.observacoes)
        assertNull(workOrder?.concludedAt)
        assertTrue(workOrder?.numero?.matches(Regex("\\d{6}\\d{4}")) == true)

        val items = db.workOrderDao().findItemsByWorkOrder(workOrderId)
        assertEquals(2, items.size)
        assertEquals(serviceId, items[0].serviceProductId)
        assertEquals(2.0, items[0].quantidade, 0.0)
        assertEquals(100.0, items[0].subtotal, 0.0)

        val convertedQuote = db.quoteDao().findById(quoteId)
        assertEquals(QuoteStatus.Converted.label, convertedQuote?.status)
    }

    @Test
    fun rejectsQuoteThatIsNotApproved() = runTest {
        val db = database as br.com.sos.osmobile.core.database.AppDatabase
        val (customerId, _) = TestFixtures.seedCustomerAndService(db)
        val quoteId = db.quoteDao().insert(
            TestFixtures.quote(
                numero = "2601010002",
                customerId = customerId,
                status = QuoteStatus.Pending.label,
                totalValue = 100.0,
            ),
        )

        val result = repository.convertApprovedQuoteToWorkOrder(quoteId)

        assertEquals(QuoteConversionResult.QuoteNotApproved, result)
        assertEquals(0, db.workOrderDao().listAll().size)
    }

    @Test
    fun rejectsApprovedQuoteWithoutItems() = runTest {
        val db = database as br.com.sos.osmobile.core.database.AppDatabase
        val (customerId, _) = TestFixtures.seedCustomerAndService(db)
        val quoteId = db.quoteDao().insert(
            TestFixtures.quote(
                numero = "2601010003",
                customerId = customerId,
                status = QuoteStatus.Approved.label,
                totalValue = 100.0,
            ),
        )

        val result = repository.convertApprovedQuoteToWorkOrder(quoteId)

        assertEquals(QuoteConversionResult.QuoteWithoutItems, result)
        assertEquals(0, db.workOrderDao().listAll().size)
    }

    @Test
    fun returnsNotFoundForMissingQuote() = runTest {
        val result = repository.convertApprovedQuoteToWorkOrder(9999L)

        assertEquals(QuoteConversionResult.QuoteNotFound, result)
    }

    @Test
    fun recordsAuditEntriesOnBothRecords() = runTest {
        val db = database as br.com.sos.osmobile.core.database.AppDatabase
        val (customerId, serviceId) = TestFixtures.seedCustomerAndService(db)
        val quoteId = db.quoteDao().insertWithItems(
            quote = TestFixtures.quote(
                numero = "2601010004",
                customerId = customerId,
                status = QuoteStatus.Approved.label,
                totalValue = 100.0,
            ),
            items = listOf(TestFixtures.quoteItem(0, serviceId, 1.0, 100.0)),
        )
        val result = repository.convertApprovedQuoteToWorkOrder(quoteId)
        val workOrderId = (result as QuoteConversionResult.Converted).workOrderId

        val quoteAudit = TestFixtures.auditRepository(db).listForRecord("orcamentos", quoteId)
        val workOrderAudit = TestFixtures.auditRepository(db).listForRecord("ordens_servico", workOrderId)

        assertTrue(quoteAudit.any { it.acao == "Orcamento convertido em OS" })
        assertTrue(workOrderAudit.any { it.acao == "OS criada por conversao de orcamento" })
        assertTrue(quoteAudit.first { it.acao == "Orcamento convertido em OS" }.detalhes!!.contains("2601010004"))
    }
}
