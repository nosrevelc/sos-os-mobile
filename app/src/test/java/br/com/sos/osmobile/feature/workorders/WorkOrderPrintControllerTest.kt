package br.com.sos.osmobile.feature.workorders

import androidx.test.core.app.ApplicationProvider
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.repository.WorkOrderItemInput
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.testing.TestFixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkOrderPrintControllerTest {

    private lateinit var database: AppDatabase
    private lateinit var session: WorkOrderSessionState
    private lateinit var controller: WorkOrderPrintController
    private lateinit var workOrderRepository: WorkOrderRepository

    @Before
    fun setUp() {
        database = TestFixtures.inMemoryDatabase()
        workOrderRepository = WorkOrderRepository(
            workOrderDao = database.workOrderDao(),
            auditRepository = TestFixtures.auditRepository(database),
        )
        session = WorkOrderSessionState()
        controller = WorkOrderPrintController(
            session = session,
            workOrderRepository = workOrderRepository,
            uiStateProvider = { WorkOrderUiState() },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun createSavedOrderId(): Long {
        val customerId = database.customerDao().insert(TestFixtures.customer())
        val serviceId = database.serviceProductDao().insert(TestFixtures.serviceProduct())
        return workOrderRepository.create(
            customerId = customerId,
            status = WorkOrderStatus.Open.label,
            notes = null,
            items = listOf(WorkOrderItemInput(serviceId, 1.0, 100.0)),
            discountValue = 0.0,
            deliveryType = "Retirada no local",
            deliveryStatus = "Aguardando retirada",
            deliveryAddress = null,
            deliveryFee = 0.0,
            trackingCode = null,
            deliveryNotes = null,
        )
    }

    @Test
    fun documentFallbackForMissingOrder() = runTest {
        session.scope = CoroutineScope(UnconfinedTestDispatcher())
        var callbackText: String? = null

        controller.showDocumentThen(9_999L) { callbackText = it }

        assertEquals("Documento nao encontrado.", callbackText)
        assertEquals("Documento nao encontrado.", session.documentText)
    }

    @Test
    fun documentTextMatchesRepositoryOutput() = runTest {
        session.scope = CoroutineScope(UnconfinedTestDispatcher())
        val orderId = createSavedOrderId()

        controller.showDocumentThen(orderId)

        assertEquals(workOrderRepository.generateDocumentText(orderId), session.documentText)
        assertNotNull(session.documentText)
    }

    @Test
    fun thermalFallbackShowsNotFoundBody() = runTest {
        session.scope = CoroutineScope(UnconfinedTestDispatcher())
        var content: ThermalPrintContent? = null

        controller.showThermalDocumentThen(9_999L) { content = it }

        assertEquals("Documento nao encontrado.", content?.body)
        assertNotNull(session.documentText)
    }
}
