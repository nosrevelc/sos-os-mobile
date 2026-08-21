package br.com.sos.osmobile.feature.workorders

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.text.KeyboardOptions
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.message.PixPayloadGenerator
import br.com.sos.osmobile.data.message.WorkOrderMessageRenderer
import br.com.sos.osmobile.data.model.DeliveryStatus
import br.com.sos.osmobile.data.model.DeliveryType
import br.com.sos.osmobile.data.local.AttachmentNames
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.print.BluetoothThermalPrinter
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalTextBlock
import br.com.sos.osmobile.ui.components.CustomerSection
import br.com.sos.osmobile.ui.components.DocumentDraftItemRow
import br.com.sos.osmobile.ui.components.DocumentItemsEditor
import br.com.sos.osmobile.ui.components.StatusOption
import br.com.sos.osmobile.ui.components.StatusSelectorCompact
import br.com.sos.osmobile.ui.components.DriveSyncIndicator
import br.com.sos.osmobile.ui.components.DriveSyncStatusIcon
import br.com.sos.osmobile.ui.components.DriveSyncStatusText
import br.com.sos.osmobile.ui.components.MessageActionButtons
import br.com.sos.osmobile.ui.components.PixQrCode
import br.com.sos.osmobile.ui.components.ShareFileButton
import br.com.sos.osmobile.ui.components.SharePdfButton
import br.com.sos.osmobile.ui.components.ShareTextButton
import br.com.sos.osmobile.ui.components.ServiceProductSearchSelector
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
import br.com.sos.osmobile.core.format.Formatters

@OptIn(ExperimentalLayoutApi::class)
internal fun WorkOrderDraftItem.toMessageItem(): MessageTemplateRenderer.ItemData =
    MessageTemplateRenderer.ItemData(name = name, quantity = quantity, unitPrice = unitPrice)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkOrderForm(
    form: WorkOrderFormState,
    customers: List<CustomerEntity>,
    services: List<ServiceProductEntity>,
    onCustomerSelected: (Long) -> Unit,
    onServiceSelected: (ServiceProductEntity) -> Unit,
    onStatusSelected: (WorkOrderStatus) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
    onDiscountChanged: (String) -> Unit,
    onDeliveryTypeChanged: (String) -> Unit,
    onDeliveryStatusChanged: (String) -> Unit,
    onDeliveryAddressChanged: (String) -> Unit,
    onDeliveryFeeChanged: (String) -> Unit,
    onTrackingCodeChanged: (String) -> Unit,
    onDeliveryNotesChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    var discountField by remember { mutableStateOf(TextFieldValue(form.discount, TextRange(form.discount.length))) }
    var deliveryFeeField by remember { mutableStateOf(TextFieldValue(form.deliveryFee, TextRange(form.deliveryFee.length))) }
    LaunchedEffect(form.discount) {
        if (discountField.text != form.discount) discountField = TextFieldValue(form.discount, TextRange(form.discount.length))
    }
    LaunchedEffect(form.deliveryFee) {
        if (deliveryFeeField.text != form.deliveryFee) deliveryFeeField = TextFieldValue(form.deliveryFee, TextRange(form.deliveryFee.length))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (form.editingId == null) "Nova OS" else "Editar OS",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        CustomerSection(
            customers = customers,
            selectedCustomerId = form.selectedCustomerId,
            onCustomerSelected = onCustomerSelected,
            emptyText = "Cadastre um cliente antes de criar OS.",
        )

        Text("Status", style = MaterialTheme.typography.titleSmall)
        StatusSelectorCompact(
            options = WorkOrderStatus.entries.map { StatusOption(it, it.label) },
            selected = form.status,
            onSelected = onStatusSelected,
        )

        DocumentItemsEditor(
            services = services,
            selectedServiceProductId = form.selectedServiceProductId,
            onServiceSelected = onServiceSelected,
            emptyServicesText = "Cadastre servicos/produtos antes de adicionar itens.",
            quantity = form.quantity,
            unitPrice = form.unitPrice,
            onQuantityChanged = onQuantityChanged,
            onUnitPriceChanged = onUnitPriceChanged,
            onAddItem = onAddItem,
            showAddIcon = true,
        ) {
            form.items.forEachIndexed { index, item ->
                DocumentDraftItemRow(
                    name = item.name,
                    quantity = item.quantity.toString(),
                    unitPrice = item.unitPrice,
                    subtotal = item.subtotal,
                    onRemoveItem = { onRemoveItem(index) },
                    showDeleteIcon = true,
                )
            }
        }

        OutlinedTextField(
            value = discountField,
            onValueChange = {
                val masked = InputMasks.currency(it.text)
                discountField = TextFieldValue(masked, TextRange(masked.length))
                onDiscountChanged(masked)
            },
            label = { Text("Desconto") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        val subtotal = form.items.sumOf { it.subtotal }
        val discount = WorkOrderFormValidator.parseDecimal(form.discount)?.coerceIn(0.0, subtotal) ?: 0.0
        Text(
            text = "Subtotal: ${Formatters.currency(subtotal)} | Desconto: ${Formatters.currency(discount)} | Total: ${Formatters.currency((subtotal - discount).coerceAtLeast(0.0))}",
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

        Text("Entrega/Retirada", style = MaterialTheme.typography.titleSmall)
        DeliveryOptionSelector(
            label = "Tipo",
            selected = form.deliveryType,
            options = DeliveryType.all,
            onSelected = onDeliveryTypeChanged,
        )
        DeliveryOptionSelector(
            label = "Status",
            selected = form.deliveryStatus,
            options = DeliveryStatus.all,
            onSelected = onDeliveryStatusChanged,
        )
        OutlinedTextField(
            value = form.deliveryAddress,
            onValueChange = onDeliveryAddressChanged,
            label = { Text("Endereco de entrega") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = deliveryFeeField,
                onValueChange = {
                    val masked = InputMasks.currency(it.text)
                    deliveryFeeField = TextFieldValue(masked, TextRange(masked.length))
                    onDeliveryFeeChanged(masked)
                },
                label = { Text("Taxa") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = form.trackingCode,
                onValueChange = onTrackingCodeChanged,
                label = { Text("Rastreio") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = form.deliveryNotes,
            onValueChange = onDeliveryNotesChanged,
            label = { Text("Obs. entrega") },
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
                Icon(Icons.Filled.Save, contentDescription = null)
                Text(if (form.editingId == null) "Salvar OS" else "Atualizar OS")
            }
        }
    }
}

@Composable
fun WorkOrderScreen(
    viewModel: WorkOrderViewModel,
    initialEditId: Long? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
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
    var signatureName by remember(selectedCustomer?.nome) { mutableStateOf(selectedCustomer?.nome.orEmpty()) }
    var checklistDescription by remember { mutableStateOf("") }
    var warrantyDays by remember { mutableStateOf("90") }
    var warrantyTerms by remember { mutableStateOf("Garantia conforme politica da empresa. Apresente este comprovante para atendimento.") }
    var paymentValue by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("PIX") }
    var paymentNote by remember { mutableStateOf("") }
    val subtotalValue = form.items.sumOf { item -> item.subtotal }
    val discountValue = WorkOrderFormValidator.parseDecimal(form.discount)?.coerceIn(0.0, subtotalValue) ?: 0.0
    val totalValue = (subtotalValue - discountValue).coerceAtLeast(0.0)
    val pendingPayment = if (form.editingId == null) WorkOrderFormValidator.parseDecimal(paymentValue) ?: 0.0 else 0.0
    val paidTotalForMessage = viewModel.payments.sumOf { it.valor } + pendingPayment
    val pixPayload = PixPayloadGenerator.generate(uiState.pixKey, uiState.pixName, totalValue)
    val openPixPayload = PixPayloadGenerator.generateOpenAmount(uiState.pixKey, uiState.pixName)
    fun renderCurrentWorkOrderTemplate(template: String): String =
        selectedCustomer?.let {
            WorkOrderMessageRenderer.render(
                customerName = it.nome,
                customerPhone = it.telefone,
                customerCpfCnpj = it.cpfCnpj.orEmpty(),
                workOrderNumber = form.editingNumber ?: "nova",
                status = form.status.label,
                totalValue = totalValue,
                discountValue = discountValue,
                minAcceptanceValue = uiState.quoteMinAcceptanceValue,
                deliveryType = form.deliveryType,
                deliveryStatus = form.deliveryStatus,
                deliveryAddress = form.deliveryAddress,
                deliveryFee = WorkOrderFormValidator.parseDecimal(form.deliveryFee) ?: 0.0,
                trackingCode = form.trackingCode,
                paidTotal = paidTotalForMessage,
                companyName = uiState.companyName,
                pixName = uiState.pixName,
                pixKey = uiState.pixKey,
                template = template,
                items = form.items.map { it.toMessageItem() },
            )
        }.orEmpty()

    fun pendingTemplateMessage(subject: String, template: String) {
        selectedCustomer?.let {
            pendingReviewMessage = ClientMessage(
                phone = it.telefone,
                email = it.email,
                subject = subject,
                text = renderCurrentWorkOrderTemplate(template),
            )
        }
    }
    val currentMessage = selectedCustomer?.let {
        WorkOrderMessageRenderer.render(
            customerName = it.nome,
            customerPhone = it.telefone,
            customerCpfCnpj = it.cpfCnpj.orEmpty(),
            workOrderNumber = form.editingNumber ?: "nova",
            status = form.status.label,
            totalValue = totalValue,
            discountValue = discountValue,
            minAcceptanceValue = uiState.quoteMinAcceptanceValue,
            deliveryType = form.deliveryType,
            deliveryStatus = form.deliveryStatus,
            deliveryAddress = form.deliveryAddress,
            deliveryFee = WorkOrderFormValidator.parseDecimal(form.deliveryFee) ?: 0.0,
            trackingCode = form.trackingCode,
            paidTotal = paidTotalForMessage,
            companyName = uiState.companyName,
            pixName = uiState.pixName,
            pixKey = uiState.pixKey,
            template = uiState.workOrderStatusTemplates[form.status.label] ?: uiState.workOrderTemplate,
            items = form.items.map { it.toMessageItem() },
        )
    }

    LaunchedEffect(initialEditId) {
        initialEditId?.let { viewModel.editWorkOrder(it) }
    }
    LaunchedEffect(viewModel.warranty) {
        viewModel.warranty?.let {
            warrantyDays = it.warrantyDays.toString()
            warrantyTerms = it.termos
        }
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
    var showDocumentDialog by remember { mutableStateOf(false) }
    var documentDescription by remember { mutableStateOf("") }
    var selectedDocumentDescription by remember { mutableStateOf("") }
    var doNotAlertDesignAgain by remember { mutableStateOf(false) }
    val selectedDesignUris = remember { mutableStateListOf<String>() }
    val designCandidates = viewModel.pendingDesignImportCandidates
    LaunchedEffect(designCandidates) {
        selectedDesignUris.clear()
        selectedDesignUris.addAll(designCandidates.map { it.uri })
        doNotAlertDesignAgain = false
    }
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.addPhoto(uri)
    }
    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.addPhoto(uri, isDocument = true, documentDescription = selectedDocumentDescription)
        selectedDocumentDescription = ""
    }

    if (showDocumentDialog) {
        AlertDialog(
            onDismissRequest = { showDocumentDialog = false },
            title = { Text("Qual documento?") },
            text = {
                OutlinedTextField(
                    value = documentDescription,
                    onValueChange = { documentDescription = it },
                    label = { Text("Ex: comprovante, contrato, nota, RG") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = documentDescription.isNotBlank(),
                    onClick = {
                        selectedDocumentDescription = documentDescription.trim()
                        documentDescription = ""
                        showDocumentDialog = false
                        documentLauncher.launch("*/*")
                    },
                ) {
                    Text("Selecionar arquivo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDocumentDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (designCandidates.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDesignImportPrompt(doNotAlertDesignAgain) },
            title = { Text("Arquivos Design no Drive") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Selecione os arquivos que deseja importar para esta OS.")
                    LazyColumn(modifier = Modifier.height(240.dp)) {
                        items(designCandidates, key = { it.uri }) { file ->
                            val checked = file.uri in selectedDesignUris
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (checked) selectedDesignUris.remove(file.uri) else selectedDesignUris.add(file.uri)
                                    },
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        if (it) {
                                            if (file.uri !in selectedDesignUris) selectedDesignUris.add(file.uri)
                                        } else {
                                            selectedDesignUris.remove(file.uri)
                                        }
                                    },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(file.name, style = MaterialTheme.typography.bodyMedium)
                                    file.sizeBytes?.let {
                                        Text(Formatters.fileSize(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { doNotAlertDesignAgain = !doNotAlertDesignAgain },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = doNotAlertDesignAgain,
                            onCheckedChange = { doNotAlertDesignAgain = it },
                        )
                        Text("Nao alertar mais nesta OS")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedDesignUris.isNotEmpty(),
                    onClick = {
                        viewModel.importSelectedDesignFromDriveNow(selectedDesignUris.toSet(), doNotAlertDesignAgain)
                    },
                ) {
                    Text("Importar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDesignImportPrompt(doNotAlertDesignAgain) }) {
                    Text("Cancelar")
                }
            },
        )
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
                                text = WorkOrderMessageRenderer.render(
                                    customerName = customer.nome,
                                    customerPhone = customer.telefone,
                                    customerCpfCnpj = customer.cpfCnpj.orEmpty(),
                                    workOrderNumber = form.editingNumber ?: "nova",
                                    status = status.label,
                                    totalValue = totalValue,
                                    discountValue = discountValue,
                                    minAcceptanceValue = uiState.quoteMinAcceptanceValue,
                                    deliveryType = form.deliveryType,
                                    deliveryStatus = form.deliveryStatus,
                                    deliveryAddress = form.deliveryAddress,
                                    deliveryFee = WorkOrderFormValidator.parseDecimal(form.deliveryFee) ?: 0.0,
                                    trackingCode = form.trackingCode,
                                    paidTotal = viewModel.payments.sumOf { it.valor },
                                    companyName = uiState.companyName,
                                    pixName = uiState.pixName,
                                    pixKey = uiState.pixKey,
                                    template = uiState.workOrderStatusTemplates[status.label] ?: uiState.workOrderTemplate,
                                    items = form.items.map { it.toMessageItem() },
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
                onDiscountChanged = viewModel::onDiscountChanged,
                onDeliveryTypeChanged = viewModel::onDeliveryTypeChanged,
                onDeliveryStatusChanged = viewModel::onDeliveryStatusChanged,
                onDeliveryAddressChanged = viewModel::onDeliveryAddressChanged,
                onDeliveryFeeChanged = viewModel::onDeliveryFeeChanged,
                onTrackingCodeChanged = viewModel::onTrackingCodeChanged,
                onDeliveryNotesChanged = viewModel::onDeliveryNotesChanged,
                onNotesChanged = viewModel::onNotesChanged,
                onAddItem = viewModel::addSelectedItem,
                onRemoveItem = viewModel::removeItem,
                onSave = {
                    val isNewWorkOrder = form.editingId == null
                    val shouldAutoPrint = isNewWorkOrder && uiState.printWorkOrderAuto && uiState.printWorkOrderCopies > 0
                    val shouldAskPrint = isNewWorkOrder && !uiState.printWorkOrderAuto && uiState.printWorkOrderCopies > 0
                    viewModel.saveWorkOrderThenWithId(
                        initialPaymentValue = paymentValue,
                        initialPaymentMethod = paymentMethod,
                        initialPaymentNote = paymentNote,
                    ) { savedId ->
                        paymentValue = ""
                        paymentNote = ""
                        if (shouldAutoPrint) {
                            viewModel.showShelfLabelThen(savedId) { printThermalBlocks(it) }
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
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Text("Imprimir OS")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.showShelfLabelThen(form.editingId) { printThermalBlocksWithPermission(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Text("Imprimir etiqueta OS")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.showReceiptThen(form.editingId) { printThermalWithPermission(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Text("Imprimir recibo")
                }
                OutlinedButton(
                    onClick = {
                        viewModel.showWarrantyThen(form.editingId) { printThermalWithPermission(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Print, contentDescription = null)
                    Text("Imprimir garantia")
                }
            }
            thermalPrintMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
            if (form.editingId != null) {
                DriveSyncIndicator(
                    status = form.driveSyncStatus,
                    error = form.driveSyncError,
                )
                Text("Imagens e documentos", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text("Adicionar imagem")
                }
                OutlinedButton(
                    onClick = { showDocumentDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AttachFile, contentDescription = null)
                    Text("Anexar documento")
                }
                OutlinedButton(
                    onClick = viewModel::importDesignFromDriveNow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Text("Importar Design do Drive")
                }
                if (viewModel.photos.isEmpty()) {
                    Text("Nenhum anexo adicionado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    viewModel.photos.forEach { photo ->
                        val isDesign = AttachmentNames.isDesign(photo.fileName)
                        val isDocument = AttachmentNames.isDocument(photo.fileName)
                        val documentLabel = AttachmentNames.documentDescription(photo.fileName)
                        val originalFileName = AttachmentNames.originalName(photo.fileName)
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (isDesign || isDocument) Icons.Filled.Description else Icons.Filled.PhotoCamera,
                                    contentDescription = when {
                                        isDesign -> "Design"
                                        isDocument -> "Documento"
                                        else -> "Imagem"
                                    },
                                )
                                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                                    Text(
                                        text = when {
                                            isDesign -> "Design"
                                            isDocument -> "Documento${documentLabel?.let { ": $it" }.orEmpty()}"
                                            else -> "Imagem"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = originalFileName,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                        )
                                        DriveSyncStatusIcon(
                                            status = photo.driveSyncStatus,
                                            error = photo.driveSyncError.orEmpty(),
                                        )
                                    }
                                    DriveSyncStatusText(
                                        status = photo.driveSyncStatus,
                                        error = photo.driveSyncError.orEmpty(),
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW)
                                                    .setDataAndType(viewModel.photoUri(photo), photo.mimeType)
                                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                            )
                                        }.onFailure {
                                            thermalPrintMessage = "Nao foi possivel abrir a foto."
                                        }
                                    },
                                ) {
                                    Icon(Icons.Filled.OpenInFull, contentDescription = "Abrir anexo")
                                }
                                IconButton(onClick = { viewModel.deletePhoto(photo.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Remover anexo")
                                }
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = viewModel::smartSyncDriveNow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.CloudUpload, contentDescription = null)
                    Text("Sincronizar Drive")
                }
                OutlinedButton(
                    onClick = viewModel::buildDriveDebugReport,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Description, contentDescription = null)
                    Text("Gerar debug Drive")
                }
                if (viewModel.driveDebugReport.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(viewModel.driveDebugReport))
                            thermalPrintMessage = "Debug do Drive copiado."
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null)
                        Text("Copiar debug Drive")
                    }
                    Text(
                        text = "Copie e envie este debug para analise.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (uiState.signatureEnabled && form.editingId != null) {
                Text("Assinatura", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val savedSignature = viewModel.signature
                if (savedSignature != null) {
                    SavedSignaturePreview(signatureUri = viewModel.signatureUri(savedSignature))
                    Text("Assinado por: ${savedSignature.signerName}", style = MaterialTheme.typography.bodySmall)
                    DriveSyncIndicator(
                        status = savedSignature.driveSyncStatus,
                        error = savedSignature.driveSyncError.orEmpty(),
                    )
                    Text(
                        text = "Assinatura bloqueada. Remova a assinatura atual para coletar uma nova.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(viewModel.signatureUri(savedSignature), "image/png")
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                    )
                                }.onFailure {
                                    thermalPrintMessage = "Nao foi possivel abrir a assinatura."
                                }
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Abrir assinatura")
                        }
                        OutlinedButton(onClick = viewModel::deleteSignature, modifier = Modifier.weight(1f)) {
                            Text("Remover")
                        }
                    }
                } else {
                    SignatureCapture(
                        signerName = signatureName,
                        onSignerNameChanged = { signatureName = it },
                        onSave = { name, bitmap -> viewModel.saveSignature(name, bitmap) },
                    )
                }
            }
            if (uiState.checklistEnabled && form.editingId != null) {
                Text("Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = checklistDescription,
                        onValueChange = { checklistDescription = it },
                        label = { Text("Novo item") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            viewModel.addChecklistItem(checklistDescription)
                            checklistDescription = ""
                        },
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text("Adicionar")
                    }
                }
                if (viewModel.checklist.isEmpty()) {
                    Text("Nenhum item no checklist.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    viewModel.checklist.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = item.concluido,
                                onCheckedChange = { viewModel.setChecklistChecked(item.id, it) },
                            )
                            Text(item.descricao, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.deleteChecklistItem(item.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Text("Remover")
                            }
                        }
                    }
                }
            }
            if (uiState.warrantyEnabled && form.editingId != null) {
                Text("Garantia", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = warrantyDays,
                    onValueChange = { warrantyDays = it.filter(Char::isDigit).take(4) },
                    label = { Text("Prazo em dias") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = warrantyTerms,
                    onValueChange = { warrantyTerms = it },
                    label = { Text("Termos da garantia") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { viewModel.saveWarranty(warrantyDays, warrantyTerms) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Text("Salvar garantia")
                    }
                    OutlinedButton(
                        onClick = viewModel::deleteWarranty,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text("Remover")
                    }
                }
            }
            if (uiState.financeEnabled) {
                Text("Financeiro", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                val paidTotal = viewModel.payments.sumOf { it.valor } + pendingPayment
                val balance = (totalValue - paidTotal).coerceAtLeast(0.0)
                Text(
                    "Total: ${Formatters.currency(totalValue)} | Pago: ${Formatters.currency(paidTotal)} | Saldo: ${Formatters.currency(balance)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = paymentValue,
                        onValueChange = { paymentValue = InputMasks.currency(it) },
                        label = { Text("Valor pago") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    PaymentMethodSelector(
                        selected = paymentMethod,
                        onSelected = { paymentMethod = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = paymentNote,
                    onValueChange = { paymentNote = it },
                    label = { Text("Observacao do pagamento") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (form.editingId == null) {
                    Text(
                        "Este pagamento sera registrado automaticamente ao salvar a OS.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Button(
                        onClick = {
                            viewModel.addPayment(paymentValue, paymentMethod, paymentNote)
                            paymentValue = ""
                            paymentNote = ""
                        },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null)
                    Text("Registrar pagamento")
                }
                }
                viewModel.payments.forEach { payment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${Formatters.currency(payment.valor)} - ${payment.forma}", fontWeight = FontWeight.SemiBold)
                            Text(Formatters.dateTimeShort(payment.paidAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            payment.observacao?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        TextButton(onClick = { viewModel.deletePayment(payment.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Text("Remover")
                        }
                    }
                }
            }
            if (selectedCustomer != null && currentMessage != null) {
                Text("Enviar mensagem ao cliente", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
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
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Pix, contentDescription = null)
                        Text("Enviar Codigo PIX")
                    }
                    if (openPixPayload.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                pendingPixMessage = ClientMessage(
                                    phone = selectedCustomer.telefone,
                                    email = selectedCustomer.email,
                                    subject = "PIX sem valor OS ${form.editingNumber ?: "nova"}",
                                    text = openPixPayload,
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Filled.Pix, contentDescription = null)
                            Text("Codigo PIX sem valor")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            pendingReviewMessage = ClientMessage(
                                phone = selectedCustomer.telefone,
                                email = selectedCustomer.email,
                                subject = "Avaliacao OS ${form.editingNumber ?: "nova"}",
                                text = WorkOrderMessageRenderer.render(
                                    customerName = selectedCustomer.nome,
                                    customerPhone = selectedCustomer.telefone,
                                    customerCpfCnpj = selectedCustomer.cpfCnpj.orEmpty(),
                                    workOrderNumber = form.editingNumber ?: "nova",
                                    status = form.status.label,
                                    totalValue = totalValue,
                                    discountValue = discountValue,
                                    minAcceptanceValue = uiState.quoteMinAcceptanceValue,
                                    deliveryType = form.deliveryType,
                                    deliveryStatus = form.deliveryStatus,
                                    deliveryAddress = form.deliveryAddress,
                                    deliveryFee = WorkOrderFormValidator.parseDecimal(form.deliveryFee) ?: 0.0,
                                    trackingCode = form.trackingCode,
                                    paidTotal = paidTotalForMessage,
                                    companyName = uiState.companyName,
                                    pixName = uiState.pixName,
                                    pixKey = uiState.pixKey,
                                    template = uiState.reviewRequestTemplate,
                                    items = form.items.map { it.toMessageItem() },
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.RateReview, contentDescription = null)
                        Text("Solicitar avaliacao")
                    }
                    OutlinedButton(
                        onClick = { pendingTemplateMessage("Pagamento pendente OS ${form.editingNumber ?: "nova"}", uiState.paymentPendingTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Payment, contentDescription = null)
                        Text("Pagamento pendente")
                    }
                    OutlinedButton(
                        onClick = { pendingTemplateMessage("Pagamento confirmado OS ${form.editingNumber ?: "nova"}", uiState.paymentConfirmedTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Payment, contentDescription = null)
                        Text("Pagamento confirmado")
                    }
                    OutlinedButton(
                        onClick = { pendingTemplateMessage("Comprovante OS ${form.editingNumber ?: "nova"}", uiState.paymentProofRequestTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.AttachMoney, contentDescription = null)
                        Text("Pedir comprovante")
                    }
                    OutlinedButton(
                        onClick = { pendingTemplateMessage("Pedido enviado OS ${form.editingNumber ?: "nova"}", uiState.orderSentTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null)
                        Text("Pedido enviado")
                    }
                    OutlinedButton(
                        onClick = { pendingTemplateMessage("Saiu para entrega OS ${form.editingNumber ?: "nova"}", uiState.outForDeliveryTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null)
                        Text("Saiu para entrega")
                    }
                    OutlinedButton(
                        onClick = { pendingTemplateMessage("Entregue OS ${form.editingNumber ?: "nova"}", uiState.deliveredTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Text("Entregue")
                    }
                    OutlinedButton(
                        onClick = { pendingTemplateMessage("Nao entregue OS ${form.editingNumber ?: "nova"}", uiState.notDeliveredTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null)
                        Text("Nao entregue")
                    }
                    OutlinedButton(
                        onClick = { pendingTemplateMessage("Obrigado OS ${form.editingNumber ?: "nova"}", uiState.thankYouTemplate) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.RateReview, contentDescription = null)
                        Text("Agradecimento")
                    }
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
            title = { Text("Imprimir etiqueta da OS?") },
            text = { Text("OS criada. Deseja imprimir a etiqueta agora?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.showShelfLabelThen(workOrderId) { printThermalBlocksWithPermission(it) }
                        pendingPrintWorkOrderId = null
                    },
                ) {
                    Text("Imprimir etiqueta")
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

























