package br.com.sos.osmobile.feature.workorders

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Payment
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
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
    var signatureName by remember(selectedCustomer?.nome) { mutableStateOf(selectedCustomer?.nome.orEmpty()) }
    var checklistDescription by remember { mutableStateOf("") }
    var warrantyDays by remember { mutableStateOf("90") }
    var warrantyTerms by remember { mutableStateOf("Garantia conforme politica da empresa. Apresente este comprovante para atendimento.") }
    var paymentValue by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("PIX") }
    var paymentNote by remember { mutableStateOf("") }
    val totalValue = form.items.sumOf { item -> item.subtotal }
    val pendingPayment = if (form.editingId == null) WorkOrderFormValidator.parseDecimal(paymentValue) ?: 0.0 else 0.0
    val paidTotalForMessage = viewModel.payments.sumOf { it.valor } + pendingPayment
    val pixPayload = PixPayloadGenerator.generate(uiState.pixKey, uiState.pixName, totalValue)
    val currentMessage = selectedCustomer?.let {
        renderWorkOrderMessage(
            customerName = it.nome,
            customerPhone = it.telefone,
            customerCpfCnpj = it.cpfCnpj.orEmpty(),
            workOrderNumber = form.editingNumber ?: "nova",
            status = form.status.label,
            totalValue = totalValue,
            paidTotal = paidTotalForMessage,
            companyName = uiState.companyName,
            pixName = uiState.pixName,
            pixKey = uiState.pixKey,
            template = uiState.workOrderStatusTemplates[form.status.label] ?: uiState.workOrderTemplate,
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
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) viewModel.addPhoto(uri)
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
                                    paidTotal = viewModel.payments.sumOf { it.valor },
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
                    viewModel.saveWorkOrderThenWithId(
                        initialPaymentValue = paymentValue,
                        initialPaymentMethod = paymentMethod,
                        initialPaymentNote = paymentNote,
                    ) { savedId ->
                        paymentValue = ""
                        paymentNote = ""
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
                Text("Fotos da OS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = { photoLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Text("Adicionar foto")
                }
                if (viewModel.photos.isEmpty()) {
                    Text("Nenhuma foto adicionada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    viewModel.photos.forEach { photo ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(photo.fileName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                                TextButton(
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
                                    Text("Abrir")
                                }
                                TextButton(onClick = { viewModel.deletePhoto(photo.id) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = null)
                                    Text("Remover")
                                }
                            }
                        }
                    }
                }
            }
            if (uiState.signatureEnabled && form.editingId != null) {
                Text("Assinatura", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                viewModel.signature?.let { signature ->
                    Text("Assinado por: ${signature.signerName}", style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW)
                                            .setDataAndType(viewModel.signatureUri(signature), "image/png")
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
                }
                SignatureCapture(
                    signerName = signatureName,
                    onSignerNameChanged = { signatureName = it },
                    onSave = { name, bitmap -> viewModel.saveSignature(name, bitmap) },
                )
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
                    "Total: ${formatCurrency(totalValue)} | Pago: ${formatCurrency(paidTotal)} | Saldo: ${formatCurrency(balance)}",
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
                            Text("${formatCurrency(payment.valor)} - ${payment.forma}", fontWeight = FontWeight.SemiBold)
                            Text(formatDate(payment.paidAt), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                paidTotal = paidTotalForMessage,
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
            Icon(Icons.Filled.Add, contentDescription = null)
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
                Icon(Icons.Filled.Save, contentDescription = null)
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
                Icon(Icons.Filled.Delete, contentDescription = null)
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

@Composable
private fun PaymentMethodSelector(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val methods = listOf("PIX", "Dinheiro", "Debito", "Credito", "Transferencia", "Outro")
    OutlinedButton(onClick = { expanded = true }, modifier = modifier.fillMaxWidth()) {
        Icon(paymentMethodIcon(selected), contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Forma: $selected")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        methods.forEach { method ->
            DropdownMenuItem(
                leadingIcon = { Icon(paymentMethodIcon(method), contentDescription = null) },
                text = { Text(if (method == selected) "${paymentMethodLabel(method)} *" else paymentMethodLabel(method)) },
                onClick = {
                    onSelected(method)
                    expanded = false
                },
            )
        }
    }
}

private fun paymentMethodLabel(method: String): String =
    if (method == "PIX") "PIX" else method

private fun paymentMethodIcon(method: String): ImageVector =
    when (method) {
        "PIX" -> Icons.Filled.Payment
        "Dinheiro" -> Icons.Filled.AttachMoney
        "Debito", "Credito" -> Icons.Filled.CreditCard
        "Transferencia" -> Icons.Filled.AccountBalance
        else -> Icons.Filled.MoreHoriz
    }

@Composable
private fun SignatureCapture(
    signerName: String,
    onSignerNameChanged: (String) -> Unit,
    onSave: (String, Bitmap) -> Unit,
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var expanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = signerName,
        onValueChange = onSignerNameChanged,
        label = { Text("Nome do assinante") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    SignaturePad(
        strokes = strokes,
        currentStroke = currentStroke,
        onCurrentStrokeChanged = { currentStroke = it },
        onStrokeFinished = { strokes += it },
        onSizeChanged = { padSize = it },
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                strokes.clear()
                currentStroke = emptyList()
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("Limpar")
        }
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Filled.OpenInFull, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Tela cheia")
        }
        Button(
            onClick = {
                val allStrokes = strokes.toList() + listOf(currentStroke).filter { it.size > 1 }
                if (padSize.width > 0 && padSize.height > 0 && allStrokes.isNotEmpty()) {
                    onSave(signerName, createSignatureBitmap(allStrokes, padSize.width, padSize.height))
                }
            },
            modifier = Modifier.weight(1f),
            enabled = strokes.isNotEmpty() || currentStroke.isNotEmpty(),
        ) {
            Text("Salvar assinatura")
        }
    }

    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .fillMaxHeight(0.92f),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                ) {
                    Text("Assinatura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = signerName,
                        onValueChange = onSignerNameChanged,
                        label = { Text("Nome do assinante") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SignaturePad(
                        strokes = strokes,
                        currentStroke = currentStroke,
                        onCurrentStrokeChanged = { currentStroke = it },
                        onStrokeFinished = { strokes += it },
                        onSizeChanged = { padSize = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                strokes.clear()
                                currentStroke = emptyList()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Limpar")
                        }
                        OutlinedButton(onClick = { expanded = false }, modifier = Modifier.weight(1f)) {
                            Text("Fechar")
                        }
                        Button(
                            onClick = {
                                val allStrokes = strokes.toList() + listOf(currentStroke).filter { it.size > 1 }
                                if (padSize.width > 0 && padSize.height > 0 && allStrokes.isNotEmpty()) {
                                    onSave(signerName, createSignatureBitmap(allStrokes, padSize.width, padSize.height))
                                    expanded = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = strokes.isNotEmpty() || currentStroke.isNotEmpty(),
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignaturePad(
    strokes: List<List<Offset>>,
    currentStroke: List<Offset>,
    onCurrentStrokeChanged: (List<Offset>) -> Unit,
    onStrokeFinished: (List<Offset>) -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .background(Color.White)
            .onSizeChanged(onSizeChanged)
            .pointerInput(Unit) {
                val inProgressStroke = mutableListOf<Offset>()
                detectDragGestures(
                    onDragStart = {
                        inProgressStroke.clear()
                        inProgressStroke += it
                        onCurrentStrokeChanged(inProgressStroke.toList())
                    },
                    onDrag = { change, _ ->
                        inProgressStroke += change.position
                        onCurrentStrokeChanged(inProgressStroke.toList())
                    },
                    onDragEnd = {
                        if (inProgressStroke.size > 1) onStrokeFinished(inProgressStroke.toList())
                        inProgressStroke.clear()
                        onCurrentStrokeChanged(emptyList())
                    },
                    onDragCancel = {
                        inProgressStroke.clear()
                        onCurrentStrokeChanged(emptyList())
                    },
                )
            },
    ) {
        (strokes + listOf(currentStroke)).forEach { stroke ->
            stroke.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = Color.Black,
                    start = start,
                    end = end,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round,
                )
            }
        }
        drawRect(color = Color.LightGray, style = Stroke(width = 1f))
    }
}

private fun createSignatureBitmap(strokes: List<List<Offset>>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    strokes.forEach { stroke ->
        stroke.zipWithNext().forEach { (start, end) ->
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }
    return bitmap
}

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
    paidTotal: Double,
    companyName: String,
    pixName: String,
    pixKey: String,
    template: String,
): String {
    val balance = (totalValue - paidTotal).coerceAtLeast(0.0)
    val paymentStatus = when {
        totalValue <= 0.0 || paidTotal <= 0.0 -> "Pendente"
        paidTotal + 0.009 >= totalValue -> "Pago"
        else -> "Parcial"
    }
    return MessageTemplateRenderer.render(
        template = template,
        tokens = mapOf(
            "nome" to customerName,
            "telefone" to customerPhone,
            "cpf" to customerCpfCnpj,
            "os" to workOrderNumber,
            "orcamento" to "",
            "status" to status,
            "valor" to formatCurrency(totalValue),
            "valor_pago" to formatCurrency(paidTotal),
            "saldo" to formatCurrency(balance),
            "status_pagamento" to paymentStatus,
            "empresa" to companyName,
            "data" to formatDate(System.currentTimeMillis()),
            "PIX" to PixPayloadGenerator.generate(pixKey, pixName, balance.takeIf { it > 0.0 } ?: totalValue),
            "PIX_QR" to "",
        ),
    )
}
