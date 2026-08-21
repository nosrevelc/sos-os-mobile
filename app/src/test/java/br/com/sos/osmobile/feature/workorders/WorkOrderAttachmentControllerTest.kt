package br.com.sos.osmobile.feature.workorders

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.WorkOrderChecklistRepository
import br.com.sos.osmobile.data.repository.WorkOrderItemInput
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderPhotoRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.data.repository.WorkOrderSignatureRepository
import br.com.sos.osmobile.data.repository.WorkOrderWarrantyRepository
import br.com.sos.osmobile.testing.TestFixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkOrderAttachmentControllerTest {

    private lateinit var database: AppDatabase
    private lateinit var session: WorkOrderSessionState
    private lateinit var controller: WorkOrderAttachmentController
    private lateinit var workOrderRepository: WorkOrderRepository
    private lateinit var checklistRepository: WorkOrderChecklistRepository
    private lateinit var warrantyRepository: WorkOrderWarrantyRepository
    private lateinit var paymentRepository: WorkOrderPaymentRepository

    @Before
    fun setUp() {
        database = TestFixtures.inMemoryDatabase()
        val audit = TestFixtures.auditRepository(database)
        val context = ApplicationProvider.getApplicationContext<Context>()
        workOrderRepository = WorkOrderRepository(database.workOrderDao(), audit)
        checklistRepository = WorkOrderChecklistRepository(database.workOrderChecklistDao(), audit)
        warrantyRepository = WorkOrderWarrantyRepository(database.workOrderWarrantyDao(), audit)
        paymentRepository = WorkOrderPaymentRepository(database.workOrderPaymentDao(), audit)
        val driveSyncRepository = DriveSyncRepository(
            context = context,
            workOrderDao = database.workOrderDao(),
            photoDao = database.workOrderPhotoDao(),
            signatureDao = database.workOrderSignatureDao(),
            settingsRepository = SettingsRepository(database.settingsDao(), audit),
            auditRepository = audit,
        )
        session = WorkOrderSessionState()
        controller = WorkOrderAttachmentController(
            session = session,
            photoRepository = WorkOrderPhotoRepository(context, database.workOrderPhotoDao(), audit),
            signatureRepository = WorkOrderSignatureRepository(context, database.workOrderSignatureDao(), audit),
            checklistRepository = checklistRepository,
            warrantyRepository = warrantyRepository,
            paymentRepository = paymentRepository,
            driveSyncRepository = driveSyncRepository,
            workOrderRepository = workOrderRepository,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun attachScope() {
        session.scope = CoroutineScope(UnconfinedTestDispatcher())
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

    private fun openEditing(orderId: Long) {
        session.formState = session.formState.copy(editingId = orderId)
    }

    @Test
    fun photoRequiresSavedOrder() = runTest {
        attachScope()

        controller.addPhoto(Uri.parse("content://teste/foto.jpg"))

        assertEquals("Salve a OS antes de adicionar anexos.", session.formState.message)
    }

    @Test
    fun checklistRequiresSavedOrder() = runTest {
        attachScope()

        controller.addChecklistItem("Trocar oleo")

        assertEquals("Salve a OS antes de adicionar checklist.", session.formState.message)
    }

    @Test
    fun checklistRejectsBlankDescription() = runTest {
        attachScope()
        openEditing(1L)

        controller.addChecklistItem("   ")

        assertEquals("Informe o item do checklist.", session.formState.message)
    }

    @Test
    fun checklistAddToggleAndDeleteRoundTrip() = runTest {
        attachScope()
        openEditing(createSavedOrderId())

        controller.addChecklistItem("  Trocar oleo  ")

        assertEquals(1, session.checklist.size)
        val item = session.checklist.first()
        assertEquals("Trocar oleo", item.descricao)
        assertEquals(false, item.concluido)

        controller.setChecklistChecked(item.id, true)

        assertTrue(session.checklist.first().concluido)

        controller.deleteChecklistItem(item.id)

        assertTrue(session.checklist.isEmpty())
        assertNull(checklistRepository.listByWorkOrder(session.formState.editingId ?: 0).firstOrNull())
    }

    @Test
    fun warrantyParsesDaysOrFallsBackToZero() = runTest {
        attachScope()
        val orderId = createSavedOrderId()
        openEditing(orderId)

        controller.saveWarranty("90", "Garantia de fabrica")

        assertEquals(90, session.warranty?.warrantyDays)
        assertEquals("Garantia de fabrica", session.warranty?.termos)

        controller.saveWarranty("abc", "")

        assertEquals(0, session.warranty?.warrantyDays)
    }

    @Test
    fun paymentRejectsInvalidValueAndRegistersValidOne() = runTest {
        attachScope()
        val orderId = createSavedOrderId()
        openEditing(orderId)

        controller.addPayment("abc", "Pix", "")
        assertEquals("Informe um valor de pagamento valido.", session.formState.message)

        controller.addPayment("0", "Dinheiro", "")
        assertEquals("Informe um valor de pagamento valido.", session.formState.message)

        controller.addPayment("100,50", "Pix", "Entrada")

        assertEquals(1, session.payments.size)
        assertEquals(100.5, session.payments.first().valor, 0.0)
        assertEquals("Pagamento registrado.", session.formState.message)

        controller.deletePayment(session.payments.first().id)

        assertTrue(session.payments.isEmpty())
    }
}
