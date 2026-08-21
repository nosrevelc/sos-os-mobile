package br.com.sos.osmobile.feature.workorders

import androidx.test.core.app.ApplicationProvider
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.WorkOrderItemInput
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.testing.TestFixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkOrderMessageControllerTest {

    private lateinit var database: AppDatabase
    private lateinit var session: WorkOrderSessionState
    private lateinit var controller: WorkOrderMessageController
    private lateinit var workOrderRepository: WorkOrderRepository
    private lateinit var paymentRepository: WorkOrderPaymentRepository

    @Before
    fun setUp() {
        database = TestFixtures.inMemoryDatabase()
        val audit = TestFixtures.auditRepository(database)
        workOrderRepository = WorkOrderRepository(database.workOrderDao(), audit)
        paymentRepository = WorkOrderPaymentRepository(database.workOrderPaymentDao(), audit)
        val driveSyncRepository = DriveSyncRepository(
            context = ApplicationProvider.getApplicationContext(),
            workOrderDao = database.workOrderDao(),
            photoDao = database.workOrderPhotoDao(),
            signatureDao = database.workOrderSignatureDao(),
            settingsRepository = SettingsRepository(database.settingsDao(), audit),
            auditRepository = audit,
        )
        session = WorkOrderSessionState()
        controller = WorkOrderMessageController(
            session = session,
            workOrderRepository = workOrderRepository,
            paymentRepository = paymentRepository,
            uiStateProvider = { uiState },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private var uiState = WorkOrderUiState()

    private suspend fun createSavedOrderId(): Long {
        val customerId = database.customerDao().insert(TestFixtures.customer())
        val serviceId = database.serviceProductDao().insert(TestFixtures.serviceProduct())
        return workOrderRepository.create(
            customerId = customerId,
            status = WorkOrderStatus.Open.label,
            notes = null,
            items = listOf(WorkOrderItemInput(serviceId, 2.0, 100.0)),
            discountValue = 0.0,
            deliveryType = "Retirada no local",
            deliveryStatus = "Aguardando retirada",
            deliveryAddress = null,
            deliveryFee = 0.0,
            trackingCode = null,
            deliveryNotes = null,
        )
    }

    private suspend fun summaryOf(orderId: Long) =
        workOrderRepository.findSummaryById(orderId).let { requireNotNull(it) }

    @Test
    fun showMessageRendersTokensFromDefaultTemplate() = runTest {
        session.scope = CoroutineScope(UnconfinedTestDispatcher())
        val orderId = createSavedOrderId()
        paymentRepository.addPayment(orderId, 30.0, "Pix", null)

        controller.showMessage(summaryOf(orderId))

        assertEquals("11999990000", session.messagePhone)
        val text = session.messageText.orEmpty()
        assertNotNull(text)
        assertFalse(text.contains("{"))
        assertTrue(text.contains("Cliente Teste"))
        assertTrue(text.contains("Aberta"))
        assertEquals(200.0, workOrderRepository.findById(orderId)?.totalValue ?: -1.0, 0.0)
    }

    @Test
    fun statusSpecificTemplateOverridesDefault() = runTest {
        session.scope = CoroutineScope(UnconfinedTestDispatcher())
        val orderId = createSavedOrderId()
        uiState = uiState.copy(workOrderStatusTemplates = mapOf("Aberta" to "STATUS {status} OS {os}"))

        controller.showMessage(summaryOf(orderId))

        val text = session.messageText.orEmpty()
        assertFalse(text.contains("{"))
        assertTrue(text.startsWith("STATUS Aberta OS "))
    }
}
