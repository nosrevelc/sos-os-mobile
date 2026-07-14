package br.com.sos.osmobile.feature.quotes

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
    val totalValue = form.items.sumOf { item -> item.subtotal }
    val pixPayload = PixPayloadGenerator.generate(uiState.pixKey, uiState.pixName, totalValue)
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
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var pendingThermalContent by remember { mutableStateOf<ThermalPrintContent?>(null) }
    var thermalPrintMessage by remember { mutableStateOf<String?>(null) }
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
            thermalPrintMessage?.let {
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
                    showPrint = uiState.printCopies > 0,
                    onConvert = { viewModel.convertToWorkOrder(quote.id) },
                    onStatusSelected = { viewModel.updateQuoteStatus(quote.id, it) },
                    onShowDocument = { viewModel.showDocument(quote.id) },
                    onPrintThermal = {
                        viewModel.showThermalDocumentThen(quote.id) { printThermalWithPermission(it) }
                    },
                    onShowMessage = { viewModel.showMessage(quote) },
                    onShowHistory = { viewModel.showHistory(quote.id) },
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
    onNotesChanged: (String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    var quantityField by remember { mutableStateOf(TextFieldValue(form.quantity, TextRange(form.quantity.length))) }
    var unitPriceField by remember { mutableStateOf(TextFieldValue(form.unitPrice, TextRange(form.unitPrice.length))) }

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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuoteStatus.entries.forEach { status ->
                SelectionButton(
                    label = status.label,
                    selected = form.status == status,
                    onClick = { onStatusSelected(status) },
                    modifier = Modifier.weight(1f),
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = form.message.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
            )
            if (form.editingId != null) {
                OutlinedButton(onClick = onCancelEdit) {
                    Text("Cancelar")
                }
            }
            Button(onClick = onSave) {
                Text(if (form.editingId == null) "Salvar orcamento" else "Atualizar orcamento")
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
    showPrint: Boolean,
    onConvert: () -> Unit,
    onStatusSelected: (QuoteStatus) -> Unit,
    onShowDocument: () -> Unit,
    onPrintThermal: () -> Unit,
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
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (quote.status != QuoteStatus.Converted.label) {
                    TextButton(onClick = onEdit) {
                        Text("Editar")
                    }
                }
                TextButton(onClick = onShowDocument) {
                    Text("Documento")
                }
                if (showPrint) {
                    TextButton(onClick = onPrintThermal) {
                        Text("Imprimir")
                    }
                }
                TextButton(onClick = onShowMessage) {
                    Text("Mensagem")
                }
                TextButton(onClick = onShowHistory) {
                    Text("Historico")
                }
                QuoteStatus.entries
                    .filter { it != QuoteStatus.Converted }
                    .forEach { status ->
                        TextButton(onClick = { onStatusSelected(status) }) {
                            Text(status.label)
                        }
                    }
            }
            if (quote.status == QuoteStatus.Approved.label) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onConvert) {
                        Text("Converter em OS")
                    }
                }
            }
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
    companyName: String,
    pixName: String,
    pixKey: String,
    template: String,
    items: List<QuoteDraftItem> = emptyList(),
): String =
    MessageTemplateRenderer.render(
        template = template,
        tokens = mapOf(
            "nome" to customerName,
            "telefone" to customerPhone,
            "cpf" to customerCpfCnpj,
            "os" to "",
            "orcamento" to quoteNumber,
            "status" to status,
            "valor" to formatCurrency(totalValue),
            "empresa" to companyName,
            "data" to formatDate(System.currentTimeMillis()),
            "PIX" to PixPayloadGenerator.generate(pixKey, pixName, totalValue),
            "PIX_QR" to "",
        ) + quoteItemTokens(items),
    )

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
