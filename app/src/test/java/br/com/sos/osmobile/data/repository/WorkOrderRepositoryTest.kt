package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.data.model.WorkOrderStatus
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
class WorkOrderRepositoryTest {

    private lateinit var database: br.com.sos.osmobile.core.database.AppDatabase
    private lateinit var repository: WorkOrderRepository
    private var sampleServiceId: Long = 0L

    @Before
    fun setUp() {
        database = TestFixtures.inMemoryDatabase()
        repository = WorkOrderRepository(
            workOrderDao = database.workOrderDao(),
            auditRepository = TestFixtures.auditRepository(database),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createSampleWorkOrder(
        discount: Double = 0.0,
        status: String = "Aberta",
    ): Long {
        val (customerId, serviceId) = TestFixtures.seedCustomerAndService(database)
        sampleServiceId = serviceId
        return repository.create(
            customerId = customerId,
            status = status,
            notes = "  Observacao da OS  ",
            items = listOf(
                WorkOrderItemInput(serviceId, 2.0, 50.0),
                WorkOrderItemInput(serviceId, 1.0, 30.0),
            ),
            discountValue = discount,
            deliveryType = "Retirada no local",
            deliveryStatus = "Aguardando retirada",
            deliveryAddress = null,
            deliveryFee = 0.0,
            trackingCode = null,
            deliveryNotes = null,
        )
    }

    @Test
    fun createComputesTotalsAndTrimsNotes() = runTest {
        val id = createSampleWorkOrder(discount = 10.0)

        val workOrder = repository.findById(id)

        assertNotNull(workOrder)
        assertEquals(120.0, workOrder?.totalValue ?: 0.0, 0.0)
        assertEquals(10.0, workOrder?.discountValue ?: 0.0, 0.0)
        assertEquals("Observacao da OS", workOrder?.observacoes)
        assertTrue(workOrder?.numero?.matches(Regex("\\d{6}\\d{4}")) == true)
        assertNull(workOrder?.concludedAt)
    }

    @Test
    fun createClampsDiscountAboveSubtotal() = runTest {
        val id = createSampleWorkOrder(discount = 500.0)

        val workOrder = repository.findById(id)

        assertEquals(130.0, workOrder?.discountValue ?: 0.0, 0.0)
        assertEquals(0.0, workOrder?.totalValue ?: -1.0, 0.0)
    }

    @Test
    fun createWithCompletedStatusSetsConclusionDate() = runTest {
        val id = createSampleWorkOrder(status = WorkOrderStatus.Completed.label)

        val workOrder = repository.findById(id)

        assertNotNull(workOrder?.concludedAt)
    }

    @Test
    fun updateStatusRecordsOriginAndDestination() = runTest {
        val id = createSampleWorkOrder()

        repository.updateStatus(id, WorkOrderStatus.Completed)

        val workOrder = repository.findById(id)
        assertEquals("Concluida", workOrder?.status)
        assertNotNull(workOrder?.concludedAt)

        val audit = TestFixtures.auditRepository(database).listForRecord("ordens_servico", id)
        assertTrue(audit.any { it.detalhes == "Aberta -> Concluida" })
    }

    @Test
    fun updateStatusKeepsPreviousConclusionDateWhenReopened() = runTest {
        val id = createSampleWorkOrder()
        repository.updateStatus(id, WorkOrderStatus.Completed)
        val concludedAt = repository.findById(id)?.concludedAt

        repository.updateStatus(id, WorkOrderStatus.Open)

        val workOrder = repository.findById(id)
        assertEquals(concludedAt, workOrder?.concludedAt)
    }

    @Test
    fun updateStatusIgnoresMissingWorkOrder() = runTest {
        repository.updateStatus(9999L, WorkOrderStatus.Completed)

        assertEquals(0, database.workOrderDao().listAll().size)
    }

    @Test
    fun updateContentRecalculatesTotals() = runTest {
        val id = createSampleWorkOrder(discount = 10.0)

        val updated = repository.updateContent(
            id = id,
            customerId = repository.findById(id)!!.customerId,
            status = WorkOrderStatus.Open,
            notes = null,
            items = listOf(WorkOrderItemInput(sampleServiceId, 3.0, 50.0)),
            discountValue = 5.0,
            deliveryType = "Retirada no local",
            deliveryStatus = "Aguardando retirada",
            deliveryAddress = null,
            deliveryFee = 0.0,
            trackingCode = null,
            deliveryNotes = null,
        )

        assertTrue(updated)
        val workOrder = repository.findById(id)
        assertEquals(145.0, workOrder?.totalValue ?: 0.0, 0.0)
        assertEquals(5.0, workOrder?.discountValue ?: 0.0, 0.0)
        assertEquals(1, repository.listItems(id).size)
    }

    @Test
    fun updateContentReturnsFalseForMissingWorkOrder() = runTest {
        val updated = repository.updateContent(
            id = 9999L,
            customerId = 1L,
            status = WorkOrderStatus.Open,
            notes = null,
            items = emptyList(),
            discountValue = 0.0,
            deliveryType = "Retirada no local",
            deliveryStatus = "Aguardando retirada",
            deliveryAddress = null,
            deliveryFee = 0.0,
            trackingCode = null,
            deliveryNotes = null,
        )

        assertEquals(false, updated)
    }
}
