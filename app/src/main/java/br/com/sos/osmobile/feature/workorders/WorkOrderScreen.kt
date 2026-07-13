package br.com.sos.osmobile.feature.workorders

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.message.PixPayloadGenerator
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.print.BluetoothThermalPrinter
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalTextBlock
import br.com.sos.osmobile.ui.components.CustomerSearchSelector
import br.com.sos.osmobile.ui.components.MessageActionButtons
import br.com.sos.osmobile.ui.components.PixQrCode
import br.com.sos.osmobile.ui.components.ShareFileButton
import br.com.sos.osmobile.ui.components.SharePdfButton
import br.com.sos.osmobile.ui.components.ShareTextButton
import br.com.sos.osmobile.ui.components.ServiceProductSearchSelector
import br.com.sos.osmobile.ui.components.WhatsAppTextButton
import br.com.sos.osmobile.ui.components.openEmail
import br.com.sos.osmobile.ui.components.openSms
import br.com.sos.osmobile.ui.components.openWhatsApp
import br.com.sos.osmobile.ui.components.sharePixQrJpeg
import br.com.sos.osmobile.ui.input.InputMasks
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

@Composable
fun WorkOrderScreen(
    viewModel: WorkOrderViewModel,
    initialEditId: Long? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingStatusMessage by remember { mutableStateOf<ClientMessage?>(null) }
    var pendingPixMessage by remember { mutableStateOf<ClientMessage?>(null) }
    var pendingReviewMessage by remember { mutableStateOf<ClientMessage?>(null) }
    var pendingThermalText by remember { mutableStateOf<String?>(null) }
    var pendingThermalContent by remember { mutableStateOf<ThermalPrintContent?>(null) }
    var pendingThermalBlocks by remember { mutableStateOf<List<ThermalTextBlock>?>(null) }
    var pendingPrintWorkOrderId by remember { mutableStateOf<Long?>(null) }
    var thermalPrintMessage by remember { mutableStateOf<String?>(null) }
    val selectedCustomer = uiState.customers.firstOrNull { it.id == form.selectedCustomerId }
    val totalValue = form.items.sumOf { item -> item.subtotal }
    val pixPayload = PixPayloadGenerator.generate(uiState.pixKey, uiState.pixName, totalValue)
    val currentMessage = selectedCustomer?.let {
        renderWorkOrderMessage(
            customerName = it.nome,
            customerPhone = it.telefone,
            customerCpfCnpj = it.cpfCnpj.orEmpty(),
            workOrderNumber = form.editingNumber ?: "nova",
            status = form.status.label,
            totalValue = totalValue,
            companyName = uiState.companyName,
            pixName = uiState.pixName,
            pixKey = uiState.pixKey,
            template = uiState.workOrderStatusTemplates[form.status.label] ?: uiState.workOrderTemplate,
        )
    }

    LaunchedEffect(initialEditId) {
        initialEditId?.let { viewModel.editWorkOrder(it) }
    }

    fun printThermalContent(content: ThermalPrintContent) {
        coroutineScope.launch {
            thermalPrintMessage = BluetoothThermalPrinter.print58mm(
                context = context,
                deviceAddress = uiState.printBluetoothAddress,
                content = content,
                copies = uiState.printWorkOrderCopies,
                style = uiState.printWorkOrderStyle,
            ).fold(
                onSuccess = { "Impressao enviada." },
                onFailure = { "Falha na impressao: ${it.message ?: "verifique a impressora"}" },
            )
        }
    }

    fun printThermalBlocks(blocks: List<ThermalTextBlock>) {
        coroutineScope.launch {
            thermalPrintMessage = BluetoothThermalPrinter.print58mmBlocks(
                context = context,
                deviceAddress = uiState.printBluetoothAddress,
                blocks = blocks,
                copies = uiState.printWorkOrderCopies,
            ).fold(
                onSuccess = { "Impressao enviada." },
                onFailure = { "Falha na impressao: ${it.message ?: "verifique a impressora"}" },
            )
        }
    }

    fun printThermal(text: String) {
        printThermalContent(ThermalPrintContent(body = text))
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val text = pendingThermalText
        val content = pendingThermalContent
        val blocks = pendingThermalBlocks
        pendingThermalText = null
        pendingThermalContent = null
        pendingThermalBlocks = null
        if (granted && blocks != null) {
            printThermalBlocks(blocks)
        } else if (granted && content != null) {
            printThermalContent(content)
        } else if (granted && text != null) {
            printThermal(text)
        } else {
            thermalPrintMessage = "Permissao Bluetooth negada."
        }
    }

    fun printThermalWithPermission(content: ThermalPrintContent) {
        if (BluetoothThermalPrinter.hasBluetoothPermission(context)) {
            printThermalContent(content)
        } else {
            pendingThermalContent = content
            bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    fun printThermalWithPermission(text: String) {
        printThermalWithPermission(ThermalPrintContent(body = text))
    }

    fun printThermalBlocksWithPermission(blocks: List<ThermalTextBlock>) {
        if (BluetoothThermalPrinter.hasBluetoothPermission(context)) {
            printThermalBlocks(blocks)
        } else {
            pendingThermalBlocks = blocks
            bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            WorkOrderForm(
                form = form,
                customers = uiState.customers,
                services = uiState.services,
                onCustomerSelected = viewModel::selectCustomer,
                onServiceSelected = viewModel::selectServiceProduct,
                onStatusSelected = { status ->
                    viewModel.selectStatus(status)
                    if (form.editingId != null && status != form.status) {
                        selectedCustomer?.let { customer ->
                            val message = ClientMessage(
                                phone = customer.telefone,
                                email = customer.email,
                                subject = "OS ${form.editingNumber ?: "nova"}",
                                text = renderWorkOrderMessage(
                                    customerName = customer.nome,
                                    customerPhone = customer.telefone,
                                    customerCpfCnpj = customer.cpfCnpj.orEmpty(),
                                    workOrderNumber = form.editingNumber ?: "nova",
                                    status = status.label,
                                    totalValue = form.items.sumOf { item -> item.subtotal },
                                    companyName = uiState.companyName,
                                    pixName = uiState.pixName,
                                    pixKey = uiState.pixKey,
                                    template = uiState.workOrderStatusTemplates[status.label] ?: uiState.workOrderTemplate,
                                ),
                            )
                            viewModel.saveWorkOrderThen {
                                pendingStatusMessage = message
                            }
                        }
                    }
                },
                onQuantityChanged = viewModel::onQuantityChanged,
                onUnitPriceChanged = viewModel::onUnitPriceChanged,
                onNotesChanged = viewModel::onNotesChanged,
                onAddItem = viewModel::addSelectedItem,
                onRemoveItem = viewModel::removeItem,
                onSave = {
                    val isNewWorkOrder = form.editingId == null
                    val shouldAutoPrint = isNewWorkOrder && uiState.printWorkOrderAuto && uiState.printWorkOrderCopies > 0
                    val shouldAskPrint = isNewWorkOrder && !uiState.printWorkOrderAuto && uiState.printWorkOrderCopies > 0
                    viewModel.saveWorkOrderThenWithId { savedId ->
                        if (shouldAutoPrint) {
                            viewModel.showThermalDocumentThen(savedId) { printThermalContent(it) }
                        } else if (shouldAskPrint) {
                            pendingPrintWorkOrderId = savedId
                        }
                    }
                },
                onCancelEdit = viewModel::cancelEdit,
            )
        }

        item {
            Text(
                text = "Documento e mensagens",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            viewModel.listMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            viewModel.documentText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
                ShareTextButton(label = "Compartilhar documento", text = it)
                ShareFileButton(label = "Compartilhar arquivo", fileName = "ordem_servico.txt", text = it)
                SharePdfButton(label = "Compartilhar PDF", fileName = "ordem_servico.pdf", text = it)
            }
            if (form.editingId != null && uiState.printWorkOrderCopies > 0) {
                OutlinedButton(
                    onClick = {
                        viewModel.showThermalDocumentThen(form.editingId) { printThermalWithPermission(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Imprimir OS")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.showShelfLabelThen(form.editingId) { printThermalBlocksWithPermission(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Imprimir etiqueta OS")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.showReceiptThen(form.editingId) { printThermalWithPermission(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Imprimir recibo")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.showWarrantyThen(form.editingId) { printThermalWithPermission(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Imprimir garantia")
                }
            }
            thermalPrintMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            viewModel.messageText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                ShareTextButton(label = "Compartilhar mensagem", text = it)
                WhatsAppTextButton(phone = viewModel.messagePhone, text = it)
            }
            if (selectedCustomer != null && currentMessage != null) {
                Text("Enviar mensagem ao cliente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                MessageActionButtons(
                    phone = selectedCustomer.telefone,
                    email = selectedCustomer.email,
                    subject = "OS ${form.editingNumber ?: "nova"}",
                    text = currentMessage,
                )
                OutlinedButton(
                    onClick = {
                        pendingPixMessage = ClientMessage(
                            phone = selectedCustomer.telefone,
                            email = selectedCustomer.email,
                            subject = "Codigo PIX OS ${form.editingNumber ?: "nova"}",
                            text = pixPayload,
                        )
                    },
                ) {
                    Text("Enviar Codigo PIX")
                }
                OutlinedButton(
                    onClick = {
                        pendingReviewMessage = ClientMessage(
                            phone = selectedCustomer.telefone,
                            email = selectedCustomer.email,
                            subject = "Avaliacao OS ${form.editingNumber ?: "nova"}",
                            text = renderWorkOrderMessage(
                                customerName = selectedCustomer.nome,
                                customerPhone = selectedCustomer.telefone,
                                customerCpfCnpj = selectedCustomer.cpfCnpj.orEmpty(),
                                workOrderNumber = form.editingNumber ?: "nova",
                                status = form.status.label,
                                totalValue = totalValue,
                                companyName = uiState.companyName,
                                pixName = uiState.pixName,
                                pixKey = uiState.pixKey,
                                template = uiState.reviewRequestTemplate,
                            ),
                        )
                    },
                ) {
                    Text("Solicitar avaliacao")
                }
                if (pixPayload.isNotBlank()) {
                    Text("QR Code PIX", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    PixQrCode(payload = pixPayload)
                }
            }
            viewModel.historyText?.let {
                Text("Historico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    pendingPrintWorkOrderId?.let { workOrderId ->
        AlertDialog(
            onDismissRequest = { pendingPrintWorkOrderId = null },
            title = { Text("Imprimir OS?") },
            text = { Text("OS criada. Deseja imprimir a entrada agora?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.showThermalDocumentThen(workOrderId) { printThermalWithPermission(it) }
                        pendingPrintWorkOrderId = null
                    },
                ) {
                    Text("Imprimir")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPrintWorkOrderId = null }) {
                    Text("Cancelar")
                }
            },
        )
    }

    pendingStatusMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingStatusMessage = null },
            title = { Text("Enviar mensagem?") },
            text = { Text("Status alterado. Deseja avisar o cliente agora?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        openWhatsApp(context, message.phone, message.text)
                        pendingStatusMessage = null
                    },
                ) {
                    Text("WhatsApp")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            openEmail(context, message.email, message.subject, message.text)
                            pendingStatusMessage = null
                        },
                    ) {
                        Text("Email")
                    }
                    TextButton(
                        onClick = {
                            openSms(context, message.phone, message.text)
                            pendingStatusMessage = null
                        },
                    ) {
                        Text("SMS")
                    }
                    TextButton(onClick = { pendingStatusMessage = null }) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }

    pendingPixMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingPixMessage = null },
            title = { Text("Enviar Codigo PIX") },
            text = { Text("Escolha o canal de envio.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        sharePixQrJpeg(context, message.text, message.text, whatsappOnly = true)
                        pendingPixMessage = null
                    },
                ) {
                    Text("WhatsApp")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            openEmail(context, message.email, message.subject, message.text)
                            pendingPixMessage = null
                        },
                    ) {
                        Text("Email")
                    }
                    TextButton(
                        onClick = {
                            openSms(context, message.phone, message.text)
                            pendingPixMessage = null
                        },
                    ) {
                        Text("SMS")
                    }
                    TextButton(onClick = { pendingPixMessage = null }) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }

    pendingReviewMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { pendingReviewMessage = null },
            title = { Text("Solicitar avaliacao") },
            text = { Text("Escolha o canal de envio.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        openWhatsApp(context, message.phone, message.text)
                        pendingReviewMessage = null
                    },
                ) {
                    Text("WhatsApp")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            openEmail(context, message.email, message.subject, message.text)
                            pendingReviewMessage = null
                        },
                    ) {
                        Text("Email")
                    }
                    TextButton(
                        onClick = {
                            openSms(context, message.phone, message.text)
                            pendingReviewMessage = null
                        },
                    ) {
                        Text("SMS")
                    }
                    TextButton(onClick = { pendingReviewMessage = null }) {
                        Text("Cancelar")
                    }
                }
            },
        )
    }
}

@Composable
fun WorkOrderListScreen(
    viewModel: WorkOrderViewModel,
    onEdit: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var visibleCount by remember { mutableStateOf(20) }
    val visibleItems = uiState.workOrders.take(visibleCount)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Lista de OS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Mais recentes primeiro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.workOrders.isEmpty()) {
            item {
                Text("Nenhuma OS cadastrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(visibleItems, key = { it.id }) { workOrder ->
                WorkOrderListRow(workOrder = workOrder, onClick = { onEdit(workOrder.id) })
            }
            if (visibleCount < uiState.workOrders.size) {
                item {
                    OutlinedButton(
                        onClick = { visibleCount += 20 },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ver mais")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkOrderPickupScreen(viewModel: WorkOrderViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val items = uiState.workOrders
        .filter { it.status == WorkOrderStatus.Completed.label }
        .sortedByDescending { pickupDays(it, now) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("OS prontas para retirada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (items.isEmpty()) {
            item { Text("Nenhuma OS concluida aguardando retirada.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(items, key = { it.id }) { workOrder ->
                val days = pickupDays(workOrder, now)
                PickupRow(
                    workOrder = workOrder,
                    days = days,
                    onSend = {
                        val text = renderPickupMessage(
                            template = uiState.pickupReminderTemplate,
                            workOrder = workOrder,
                            days = days,
                            companyName = uiState.companyName,
                        )
                        openWhatsApp(context, workOrder.customerPhone, text)
                    },
                )
            }
        }
    }
}

@Composable
private fun PickupRow(
    workOrder: WorkOrderSummary,
    days: Long,
    onSend: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workOrder.customerName, fontWeight = FontWeight.SemiBold)
                Text("${workOrder.number} - $days dia(s) aguardando retirada", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onSend) {
                Text("Enviar MSG")
            }
        }
    }
}

private fun pickupDays(workOrder: WorkOrderSummary, now: Long): Long {
    val base = workOrder.concludedAt ?: workOrder.openedAt
    return TimeUnit.MILLISECONDS.toDays((now - base).coerceAtLeast(0))
}

private fun renderPickupMessage(
    template: String,
    workOrder: WorkOrderSummary,
    days: Long,
    companyName: String,
): String =
    MessageTemplateRenderer.render(
        template = template,
        tokens = mapOf(
            "nome" to workOrder.customerName,
            "telefone" to workOrder.customerPhone,
            "cpf" to "",
            "os" to workOrder.number,
            "orcamento" to "",
            "valor" to formatCurrency(workOrder.totalValue),
            "status" to workOrder.status,
            "empresa" to companyName,
            "data" to formatDate(System.currentTimeMillis()),
            "dias" to days.toString(),
        ),
    )

@Composable
private fun WorkOrderListRow(
    workOrder: WorkOrderSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(workOrder.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${workOrder.number} - ${workOrder.status}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${formatCurrency(workOrder.totalValue)} - ${formatDate(workOrder.openedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkOrderForm(
    form: WorkOrderFormState,
    customers: List<CustomerEntity>,
    services: List<ServiceProductEntity>,
    onCustomerSelected: (Long) -> Unit,
    onServiceSelected: (ServiceProductEntity) -> Unit,
    onStatusSelected: (WorkOrderStatus) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    var quantityField by remember { mutableStateOf(TextFieldValue(form.quantity, TextRange(form.quantity.length))) }
    var unitPriceField by remember { mutableStateOf(TextFieldValue(form.unitPrice, TextRange(form.unitPrice.length))) }
    LaunchedEffect(form.quantity) {
        if (quantityField.text != form.quantity) quantityField = TextFieldValue(form.quantity, TextRange(form.quantity.length))
    }
    LaunchedEffect(form.unitPrice) {
        if (unitPriceField.text != form.unitPrice) unitPriceField = TextFieldValue(form.unitPrice, TextRange(form.unitPrice.length))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (form.editingId == null) "Nova OS" else "Editar OS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text("Cliente", style = MaterialTheme.typography.titleSmall)
        CustomerSearchSelector(
            customers = customers,
            selectedCustomerId = form.selectedCustomerId,
            onCustomerSelected = onCustomerSelected,
            emptyText = "Cadastre um cliente antes de criar OS.",
        )

        Text("Status", style = MaterialTheme.typography.titleSmall)
        WorkOrderStatusSelector(status = form.status, onStatusSelected = onStatusSelected)

        Text("Itens", style = MaterialTheme.typography.titleSmall)
        ServiceProductSearchSelector(
            services = services,
            selectedServiceProductId = form.selectedServiceProductId,
            onServiceSelected = onServiceSelected,
            emptyText = "Cadastre servicos/produtos antes de adicionar itens.",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = quantityField,
                onValueChange = {
                    val masked = InputMasks.decimal(it.text, integerDigits = 5, decimalDigits = 2)
                    quantityField = TextFieldValue(masked, TextRange(masked.length))
                    onQuantityChanged(masked)
                },
                label = { Text("Qtd") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = unitPriceField,
                onValueChange = {
                    val masked = InputMasks.currency(it.text)
                    unitPriceField = TextFieldValue(masked, TextRange(masked.length))
                    onUnitPriceChanged(masked)
                },
                label = { Text("Valor") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(2f),
            )
        }

        OutlinedButton(onClick = onAddItem, modifier = Modifier.fillMaxWidth()) {
            Text("Adicionar item")
        }

        form.items.forEachIndexed { index, item ->
            DraftItemRow(index = index, item = item, onRemoveItem = onRemoveItem)
        }

        Text(
            text = "Total: ${formatCurrency(form.items.sumOf { it.subtotal })}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )

        OutlinedTextField(
            value = form.notes,
            onValueChange = onNotesChanged,
            label = { Text("Observacoes") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        form.message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (form.editingId != null) {
                OutlinedButton(onClick = onCancelEdit) {
                    Text("Cancelar")
                }
            }
            Button(onClick = onSave) {
                Text(if (form.editingId == null) "Salvar OS" else "Atualizar OS")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkOrderStatusSelector(
    status: WorkOrderStatus,
    onStatusSelected: (WorkOrderStatus) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        WorkOrderStatus.entries.forEach { option ->
            val selected = option == status
            Button(
                onClick = {
                    onStatusSelected(option)
                },
                colors = if (selected) {
                    ButtonDefaults.buttonColors()
                } else {
                    ButtonDefaults.outlinedButtonColors()
                },
            ) {
                Text(option.label)
            }
        }
    }
}

@Composable
private fun SelectionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(if (selected) "$label *" else label)
    }
}

@Composable
private fun DraftItemRow(
    index: Int,
    item: WorkOrderDraftItem,
    onRemoveItem: (Int) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${item.quantity} x ${formatCurrency(item.unitPrice)} = ${formatCurrency(item.subtotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { onRemoveItem(index) }) {
                Text("Remover")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkOrderRow(
    workOrder: WorkOrderSummary,
    onStatusSelected: (WorkOrderStatus) -> Unit,
    onShowDocument: () -> Unit,
    onShowMessage: () -> Unit,
    onShowHistory: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(workOrder.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(workOrder.customerName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${workOrder.status} - ${workOrder.itemCount} item(ns) - ${formatCurrency(workOrder.totalValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDate(workOrder.openedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (workOrder.status != WorkOrderStatus.Completed.label && workOrder.status != WorkOrderStatus.Canceled.label) {
                    TextButton(onClick = onEdit) {
                        Text("Editar")
                    }
                }
                TextButton(onClick = onShowDocument) {
                    Text("Documento")
                }
                TextButton(onClick = onShowMessage) {
                    Text("Mensagem")
                }
                TextButton(onClick = onShowHistory) {
                    Text("Historico")
                }
                WorkOrderStatus.entries.forEach { status ->
                    TextButton(onClick = { onStatusSelected(status) }) {
                        Text(status.label)
                    }
                }
            }
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

private data class ClientMessage(
    val phone: String,
    val email: String?,
    val subject: String,
    val text: String,
)

private fun renderWorkOrderMessage(
    customerName: String,
    customerPhone: String,
    customerCpfCnpj: String,
    workOrderNumber: String,
    status: String,
    totalValue: Double,
    companyName: String,
    pixName: String,
    pixKey: String,
    template: String,
): String =
    MessageTemplateRenderer.render(
        template = template,
        tokens = mapOf(
            "nome" to customerName,
            "telefone" to customerPhone,
            "cpf" to customerCpfCnpj,
            "os" to workOrderNumber,
            "orcamento" to "",
            "status" to status,
            "valor" to formatCurrency(totalValue),
            "empresa" to companyName,
            "data" to formatDate(System.currentTimeMillis()),
            "PIX" to PixPayloadGenerator.generate(pixKey, pixName, totalValue),
            "PIX_QR" to "",
        ),
    )
