package br.com.sos.osmobile.feature.workorders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.entity.StockMovementType
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.StockRepository
import br.com.sos.osmobile.data.repository.WorkOrderItemInput
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.testing.TestFixtures
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class WorkOrderFormControllerTest {

    private lateinit var database: AppDatabase
    private lateinit var session: WorkOrderSessionState
    private lateinit var controller: WorkOrderFormController
    private lateinit var workOrderRepository: WorkOrderRepository
    private lateinit var paymentRepository: WorkOrderPaymentRepository
    private var uiState = WorkOrderUiState()
    private var codigoSeq = 9_001

    @Before
    fun setUp() {
        database = TestFixtures.inMemoryDatabase()
        val audit = TestFixtures.auditRepository(database)
        workOrderRepository = WorkOrderRepository(database.workOrderDao(), audit)
        val stockRepository = StockRepository(database.stockMovementDao(), audit)
        paymentRepository = WorkOrderPaymentRepository(database.workOrderPaymentDao(), audit)
        val driveSyncRepository = DriveSyncRepository(
            context = ApplicationProvider.getApplicationContext<Context>(),
            workOrderDao = database.workOrderDao(),
            photoDao = database.workOrderPhotoDao(),
            signatureDao = database.workOrderSignatureDao(),
            settingsRepository = SettingsRepository(database.settingsDao(), audit),
            auditRepository = audit,
        )
        session = WorkOrderSessionState()
        controller = WorkOrderFormController(
            session = session,
            workOrderRepository = workOrderRepository,
            stockRepository = stockRepository,
            paymentRepository = paymentRepository,
            driveSyncRepository = driveSyncRepository,
            uiStateProvider = { uiState },
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertService(
        tipo: String = ServiceProductType.SERVICE,
        nome: String = "Servico Teste",
        price: Double = 100.0,
    ): ServiceProductEntity {
        val codigo = "SPX-${codigoSeq++}"
        val id = database.serviceProductDao().insert(
            ServiceProductEntity(
                codigo = codigo,
                nome = nome,
                tipo = tipo,
                unitPrice = price,
                createdAt = 1_000L,
                updatedAt = 1_000L,
            )
        )
        return ServiceProductEntity(
            id = id,
            codigo = codigo,
            nome = nome,
            tipo = tipo,
            unitPrice = price,
            createdAt = 1_000L,
            updatedAt = 1_000L,
        )
    }

    private suspend fun insertCustomer(): Long =
        database.customerDao().insert(TestFixtures.customer())

    private fun attachScope() {
        session.scope = CoroutineScope(UnconfinedTestDispatcher())
    }

    private fun String.normalizeNbsp(): String = replace(' ', ' ')

    @Test
    fun addSelectedItemWithoutServiceShowsMessage() = runTest {
        attachScope()

        controller.addSelectedItem()

        assertEquals("Selecione um servico/produto.", session.formState.message)
    }

    @Test
    fun addItemRejectsInvalidQuantity() = runTest {
        attachScope()
        val service = insertService()
        uiState = uiState.copy(services = listOf(service))
        controller.selectServiceProduct(service)
        controller.onQuantityChanged("0")

        controller.addSelectedItem()

        assertEquals("Quantidade deve ser maior que zero.", session.formState.message)
    }

    @Test
    fun addItemBlocksInsufficientStockForProduct() = runTest {
        attachScope()
        val product = insertService(tipo = ServiceProductType.PRODUCT, nome = "Parafuso")
        uiState = uiState.copy(
            services = listOf(product),
            stockByServiceProductId = mapOf(product.id to 2.0),
        )
        controller.selectServiceProduct(product)
        controller.onQuantityChanged("3")

        controller.addSelectedItem()

        assertTrue(session.formState.message.orEmpty().startsWith("Saldo insuficiente para Parafuso"))
    }

    @Test
    fun addItemResetsDraftOnSuccess() = runTest {
        attachScope()
        val service = insertService(price = 100.0)
        uiState = uiState.copy(services = listOf(service))
        controller.selectServiceProduct(service)
        controller.onQuantityChanged("2")

        controller.addSelectedItem()

        val form = session.formState
        assertEquals(1, form.items.size)
        assertEquals(service.id, form.items.first().serviceProductId)
        assertEquals("Servico Teste", form.items.first().name)
        assertEquals(2.0, form.items.first().quantity, 0.0)
        assertEquals(100.0, form.items.first().unitPrice, 0.0)
        assertNull(form.selectedServiceProductId)
        assertEquals("1", form.quantity)
        assertEquals("", form.unitPrice)
    }

    @Test
    fun saveRequiresCustomerAndItems() = runTest {
        attachScope()

        controller.saveWorkOrder()

        assertEquals("Selecione um cliente.", session.formState.message)
    }

    @Test
    fun saveCreatesOrderWithPaymentAndNoStockMovementForService() = runTest {
        attachScope()
        val customerId = insertCustomer()
        val service = insertService(price = 100.0)
        uiState = uiState.copy(services = listOf(service))
        controller.selectCustomer(customerId)
        controller.selectServiceProduct(service)
        controller.onQuantityChanged("2")
        addItemResetingDraft()
        var savedId = 0L

        controller.saveWorkOrderThenWithId(initialPaymentValue = "50,00") { savedId = it }

        val form = session.formState
        assertNotNull(form.editingId)
        assertEquals(savedId, form.editingId)
        assertEquals("OS criada com sucesso.", form.message)
        val order = workOrderRepository.findById(savedId)
        assertNotNull(order)
        assertEquals(200.0, order?.totalValue ?: 0.0, 0.0)
        val payments = paymentRepository.listByWorkOrder(savedId)
        assertEquals(1, payments.size)
        assertEquals(50.0, payments.first().valor, 0.0)
    }

    @Test
    fun savingProductConsumesStockAndEditingRestoresDelta() = runTest {
        attachScope()
        val customerId = insertCustomer()
        val product = insertService(tipo = ServiceProductType.PRODUCT, nome = "Parafuso", price = 5.0)
        uiState = uiState.copy(
            services = listOf(product),
            stockByServiceProductId = mapOf(product.id to 5.0),
        )
        controller.selectCustomer(customerId)
        controller.selectServiceProduct(product)
        controller.onQuantityChanged("2")
        addItemResetingDraft()
        controller.saveWorkOrderThen { }
        val savedId = requireNotNull(session.formState.editingId)

        val afterCreate = stockHistory(product.id)
        assertTrue(afterCreate.any { it.tipo == StockMovementType.OUT && it.quantidade == 2.0 })

        controller.editWorkOrder(savedId)
        controller.removeItem(0)
        controller.selectServiceProduct(product)
        controller.onQuantityChanged("1")
        addItemResetingDraft()
        controller.saveWorkOrder()

        val history = stockHistory(product.id)
        assertTrue(history.any { it.tipo == StockMovementType.IN && it.quantidade == 1.0 })
        assertEquals(5.0, workOrderRepository.findById(savedId)?.totalValue ?: -1.0, 0.0)
    }

    @Test
    fun editLoadsOriginalItemsAndNumber() = runTest {
        attachScope()
        val customerId = insertCustomer()
        val service = insertService(nome = "Troca de tela")
        uiState = uiState.copy(services = listOf(service))
        val orderId = workOrderRepository.create(
            customerId = customerId,
            status = WorkOrderStatus.Open.label,
            notes = null,
            items = listOf(WorkOrderItemInput(service.id, 1.0, service.unitPrice)),
            discountValue = 0.0,
            deliveryType = "Retirada no local",
            deliveryStatus = "Aguardando retirada",
            deliveryAddress = null,
            deliveryFee = 0.0,
            trackingCode = null,
            deliveryNotes = null,
        )

        controller.editWorkOrder(orderId)

        val form = session.formState
        assertEquals(orderId, form.editingId)
        assertNotNull(form.editingNumber)
        assertEquals(customerId, form.selectedCustomerId)
        assertEquals(1, form.items.size)
        assertEquals("Troca de tela", form.items.first().name)
        assertEquals(form.items, form.originalItems)
        assertEquals("Editando OS ${form.editingNumber}.", form.message)
    }

    @Test
    fun cancelEditClearsForm() = runTest {
        attachScope()

        controller.cancelEdit()

        assertNull(session.formState.editingId)
        assertEquals("Edicao cancelada.", session.formState.message)
    }

    @Test
    fun unitPriceFieldAppliesCurrencyMask() {
        controller.onUnitPriceChanged("123456")

        assertEquals("R$ 1.234,56", session.formState.unitPrice.normalizeNbsp())
    }

    @Test
    fun statusFromLabelMapsKnownLabelAndFallsBackToOpen() {
        assertEquals(WorkOrderStatus.Completed, WorkOrderFormController.statusFromLabel("Concluida"))
        assertEquals(WorkOrderStatus.Open, WorkOrderFormController.statusFromLabel("Inexistente"))
    }

    private fun addItemResetingDraft() {
        // addSelectedItem consome os campos de rascunho e limpa o seletor.
        controller.addSelectedItem()
    }

    private suspend fun stockHistory(serviceProductId: Long) =
        StockRepository(
            database.stockMovementDao(),
            TestFixtures.auditRepository(database),
        ).observeHistory(serviceProductId).first()
}
