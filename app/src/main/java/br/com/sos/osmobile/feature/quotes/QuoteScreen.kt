package br.com.sos.osmobile.feature.quotes

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Unpublished
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.model.QuoteSummary
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.message.PixPayloadGenerator
import br.com.sos.osmobile.data.model.QuoteStatus
import br.com.sos.osmobile.data.print.BluetoothThermalPrinter
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.ui.components.CustomerSearchSelector
import br.com.sos.osmobile.ui.components.MessageActionButtons
import br.com.sos.osmobile.ui.components.PixQrCode
import br.com.sos.osmobile.ui.components.PrintDocumentButton
import br.com.sos.osmobile.ui.components.ShareFileButton
import br.com.sos.osmobile.ui.components.SharePdfButton
import br.com.sos.osmobile.ui.components.ShareTextButton
import br.com.sos.osmobile.ui.components.ServiceProductSearchSelector
import br.com.sos.osmobile.ui.components.WhatsAppTextButton
import br.com.sos.osmobile.ui.input.InputMasks
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun QuoteScreen(
    viewModel: QuoteViewModel,
    initialEditId: Long? = null,
) {
    val uiState by viewModel.uiState.collectAsState()
    val form = viewModel.formState
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingThermalContent by remember { mutableStateOf<ThermalPrintContent?>(null) }
    var thermalPrintMessage by remember { mutableStateOf<String?>(null) }
    val selectedCustomer = uiState.customers.firstOrNull { it.id == form.selectedCustomerId }
    val subtotalValue = form.items.sumOf { item -> item.subtotal }
    val discountValue = QuoteFormValidator.parseDecimal(form.discount)?.coerceIn(0.0, subtotalValue) ?: 0.0
    val totalValue = (subtotalValue - discountValue).coerceAtLeast(0.0)
    val minimumDepositValue = (
        QuoteFormValidator.parseDecimal(form.minimumDeposit)
            ?: QuoteFormValidator.parseDecimal(uiState.quoteMinDepositValue)
            ?: 0.0
        ).coerceIn(0.0, totalValue)
    val pixPayload = PixPayloadGenerator.generate(uiState.pixKey, uiState.pixName, totalValue)
    val openPixPayload = PixPayloadGenerator.generateOpenAmount(uiState.pixKey, uiState.pixName)
    var showOpenPixActions by remember { mutableStateOf(false) }
    LaunchedEffect(initialEditId) {
        initialEditId?.let(viewModel::editQuote)
    }
    fun printThermal(content: ThermalPrintContent) {
        coroutineScope.launch {
            thermalPrintMessage = BluetoothThermalPrinter.print58mm(
                context = context,
                deviceAddress = uiState.printBluetoothAddress,
                content = content,
                copies = uiState.printCopies,
                style = uiState.printStyle,
            ).fold(
                onSuccess = { "Impressao enviada." },
                onFailure = { "Falha na impressao: ${it.message ?: "verifique a impressora"}" },
            )
        }
    }
    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val content = pendingThermalContent
        pendingThermalContent = null
        if (granted && content != null) {
            printThermal(content)
        } else {
            thermalPrintMessage = "Permissao Bluetooth negada."
        }
    }
    fun printThermalWithPermission(content: ThermalPrintContent) {
        if (BluetoothThermalPrinter.hasBluetoothPermission(context)) {
            printThermal(content)
        } else {
            pendingThermalContent = content
            bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
    val currentMessage = selectedCustomer?.let {
        renderQuoteMessage(
            customerName = it.nome,
            customerPhone = it.telefone,
            customerCpfCnpj = it.cpfCnpj.orEmpty(),
            quoteNumber = form.editingNumber ?: "novo",
            status = form.status.label,
            totalValue = totalValue,
            discountValue = discountValue,
            minimumDepositValue = minimumDepositValue,
            minAcceptanceValue = uiState.quoteMinAcceptanceValue,
            companyName = uiState.companyName,
            pixName = uiState.pixName,
            pixKey = uiState.pixKey,
            template = uiState.quoteTemplate,
            items = form.items,
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            QuoteForm(
                form = form,
                customers = uiState.customers,
                services = uiState.services,
                onCustomerSelected = viewModel::selectCustomer,
                onServiceSelected = viewModel::selectServiceProduct,
                onStatusSelected = viewModel::selectStatus,
                onQuantityChanged = viewModel::onQuantityChanged,
                onUnitPriceChanged = viewModel::onUnitPriceChanged,
                onDiscountChanged = viewModel::onDiscountChanged,
                onMinimumDepositChanged = viewModel::onMinimumDepositChanged,
                onNotesChanged = viewModel::onNotesChanged,
                onAddItem = viewModel::addSelectedItem,
                onRemoveItem = viewModel::removeItem,
                onSave = viewModel::saveQuote,
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
                ShareFileButton(label = "Compartilhar arquivo", fileName = "orcamento.txt", text = it)
                SharePdfButton(label = "Compartilhar PDF", fileName = "orcamento.pdf", text = it)
                PrintDocumentButton(label = "Imprimir", jobName = "Orcamento", text = it)
            }
            if (form.editingId != null && uiState.printCopies > 0) {
                OutlinedButton(
                    onClick = {
                        viewModel.showThermalDocumentThen(form.editingId) { printThermalWithPermission(it) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Imprimir orcamento")
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
                    subject = "Orcamento ${form.editingNumber ?: "novo"}",
                    text = currentMessage,
                )
                if (pixPayload.isNotBlank()) {
                    Text("QR Code PIX", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    PixQrCode(payload = pixPayload)
                }
                if (openPixPayload.isNotBlank()) {
                    OutlinedButton(
                        onClick = { showOpenPixActions = !showOpenPixActions },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Pix, contentDescription = null)
                        Text("Codigo PIX sem valor")
                    }
                    if (showOpenPixActions) {
                        MessageActionButtons(
                            phone = selectedCustomer.telefone,
                            email = selectedCustomer.email,
                            subject = "PIX sem valor",
                            text = openPixPayload,
                        )
                    }
                }
            }
            viewModel.historyText?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }

    }
}

@Composable
fun QuoteListScreen(
    viewModel: QuoteViewModel,
    onEdit: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Lista de Orcamentos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            viewModel.listMessage?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
        if (uiState.quotes.isEmpty()) {
            item {
                Text("Nenhum orcamento cadastrado.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(uiState.quotes, key = { it.id }) { quote ->
                QuoteRow(
                    quote = quote,
                    onEdit = { onEdit(quote.id) },
                )
            }
        }
    }
}

@Composable
private fun QuoteForm(
    form: QuoteFormState,
    customers: List<CustomerEntity>,
    services: List<ServiceProductEntity>,
    onCustomerSelected: (Long) -> Unit,
    onServiceSelected: (ServiceProductEntity) -> Unit,
    onStatusSelected: (QuoteStatus) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
    onDiscountChanged: (String) -> Unit,
    onMinimumDepositChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    var quantityField by remember { mutableStateOf(TextFieldValue(form.quantity, TextRange(form.quantity.length))) }
    var unitPriceField by remember { mutableStateOf(TextFieldValue(form.unitPrice, TextRange(form.unitPrice.length))) }
    var discountField by remember { mutableStateOf(TextFieldValue(form.discount, TextRange(form.discount.length))) }
    var minimumDepositField by remember { mutableStateOf(TextFieldValue(form.minimumDeposit, TextRange(form.minimumDeposit.length))) }

    LaunchedEffect(form.quantity) {
        if (quantityField.text != form.quantity) {
            quantityField = TextFieldValue(form.quantity, TextRange(form.quantity.length))
        }
    }
    LaunchedEffect(form.unitPrice) {
        if (unitPriceField.text != form.unitPrice) {
            unitPriceField = TextFieldValue(form.unitPrice, TextRange(form.unitPrice.length))
        }
    }
    LaunchedEffect(form.discount) {
        if (discountField.text != form.discount) {
            discountField = TextFieldValue(form.discount, TextRange(form.discount.length))
        }
    }
    LaunchedEffect(form.minimumDeposit) {
        if (minimumDepositField.text != form.minimumDeposit) {
            minimumDepositField = TextFieldValue(form.minimumDeposit, TextRange(form.minimumDeposit.length))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = if (form.editingId == null) "Novo orcamento" else "Editar orcamento",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text("Cliente", style = MaterialTheme.typography.titleSmall)
        CustomerSearchSelector(
            customers = customers,
            selectedCustomerId = form.selectedCustomerId,
            onCustomerSelected = onCustomerSelected,
            emptyText = "Cadastre um cliente antes de criar orcamentos.",
        )

        Text("Status", style = MaterialTheme.typography.titleSmall)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            QuoteStatus.entries.forEach { status ->
                SelectionButton(
                    label = status.label,
                    icon = quoteStatusIcon(status),
                    selected = form.status == status,
                    onClick = { onStatusSelected(status) },
                )
            }
        }

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
        OutlinedTextField(
            value = minimumDepositField,
            onValueChange = {
                val masked = InputMasks.currency(it.text)
                minimumDepositField = TextFieldValue(masked, TextRange(masked.length))
                onMinimumDepositChanged(masked)
            },
            label = { Text("Sinal minimo") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        val subtotal = form.items.sumOf { it.subtotal }
        val discount = QuoteFormValidator.parseDecimal(form.discount) ?: 0.0
        val minimumDeposit = QuoteFormValidator.parseDecimal(form.minimumDeposit) ?: 0.0
        Text(
            text = "Subtotal: ${formatCurrency(subtotal)} | Desconto: ${formatCurrency(discount)} | Sinal: ${formatCurrency(minimumDeposit)} | Total: ${formatCurrency((subtotal - discount).coerceAtLeast(0.0))}",
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
                modifier = Modifier.fillMaxWidth(),
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
                Text(if (form.editingId == null) "Salvar orcamento" else "Atualizar orcamento")
            }
        }
    }
}

@Composable
private fun SelectionButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null)
        Text(if (selected) "$label *" else label)
    }
}

private fun quoteStatusIcon(status: QuoteStatus): ImageVector =
    when (status) {
        QuoteStatus.Pending -> Icons.Filled.HourglassEmpty
        QuoteStatus.Approved -> Icons.Filled.CheckCircle
        QuoteStatus.Rejected -> Icons.Filled.Unpublished
        QuoteStatus.Converted -> Icons.Filled.SyncAlt
    }

@Composable
private fun DraftItemRow(
    index: Int,
    item: QuoteDraftItem,
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
private fun QuoteRow(
    quote: QuoteSummary,
    onEdit: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.clickable(onClick = onEdit),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(quote.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(quote.customerName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${quote.status} - ${quote.itemCount} item(ns) - ${formatCurrency(quote.totalValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDate(quote.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("Toque para editar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun formatCurrency(value: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(Locale("pt", "BR"), value)

private fun formatDate(timestamp: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(timestamp))

private fun renderQuoteMessage(
    customerName: String,
    customerPhone: String,
    customerCpfCnpj: String,
    quoteNumber: String,
    status: String,
    totalValue: Double,
    discountValue: Double,
    minimumDepositValue: Double,
    minAcceptanceValue: String,
    companyName: String,
    pixName: String,
    pixKey: String,
    template: String,
    items: List<QuoteDraftItem> = emptyList(),
): String {
    val subtotalValue = totalValue + discountValue
    return MessageTemplateRenderer.render(
        template = template,
        tokens = mapOf(
            "nome" to customerName,
            "telefone" to customerPhone,
            "cpf" to customerCpfCnpj,
            "os" to "",
            "orcamento" to quoteNumber,
            "status" to status,
            "valor" to formatCurrency(totalValue),
            "subtotal" to formatCurrency(subtotalValue),
            "desconto" to formatCurrency(discountValue),
            "linha_desconto" to if (discountValue > 0.0) "Desconto: ${formatCurrency(discountValue)}" else "",
            "valor_minimo_aceite" to minAcceptanceValue,
            "sinal_minimo" to formatCurrency(minimumDepositValue),
            "linha_sinal_minimo" to if (minimumDepositValue > 0.0) "Sinal minimo: ${formatCurrency(minimumDepositValue)}" else "",
            "empresa" to companyName,
            "data" to formatDate(System.currentTimeMillis()),
            "PIX" to PixPayloadGenerator.generate(pixKey, pixName, totalValue),
            "PIX_SEM_VALOR" to PixPayloadGenerator.generateOpenAmount(pixKey, pixName),
            "PIX_QR" to "",
        ) + quoteItemTokens(items),
    )
}

private fun quoteItemTokens(items: List<QuoteDraftItem>): Map<String, String> {
    val formatted = items.joinToString("\n") {
        "- ${it.name}: ${formatQuantity(it.quantity)} x ${formatCurrency(it.unitPrice)} = ${formatCurrency(it.subtotal)}"
    }
    return mapOf(
        "itens" to formatted,
        "servicos" to formatted,
        "produtos" to formatted,
        "qtd_itens" to formatQuantity(items.sumOf { it.quantity }),
        "total_itens" to formatCurrency(items.sumOf { it.subtotal }),
    )
}
