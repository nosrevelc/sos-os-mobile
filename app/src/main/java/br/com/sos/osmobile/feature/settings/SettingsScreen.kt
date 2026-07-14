package br.com.sos.osmobile.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.data.print.BluetoothPrinterDevice
import br.com.sos.osmobile.data.print.BluetoothThermalPrinter
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalPrintStyle
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var companyName by remember { mutableStateOf(settings.companyName) }
    var pixName by remember { mutableStateOf(settings.pixName) }
    var pixKey by remember { mutableStateOf(settings.pixKey) }
    var fiscalEnvironment by remember { mutableStateOf(settings.fiscalEnvironment) }
    var fiscalProvider by remember { mutableStateOf(settings.fiscalProvider) }
    var fiscalApiToken by remember { mutableStateOf(settings.fiscalApiToken) }
    var fiscalCnpj by remember { mutableStateOf(settings.fiscalCnpj) }
    var fiscalIe by remember { mutableStateOf(settings.fiscalIe) }
    var fiscalIm by remember { mutableStateOf(settings.fiscalIm) }
    var fiscalRegime by remember { mutableStateOf(settings.fiscalRegime) }
    var printBluetoothAddress by remember { mutableStateOf(settings.printBluetoothAddress) }
    var printWorkOrderAuto by remember { mutableStateOf(settings.printWorkOrderAuto) }
    var printWorkOrderCopies by remember { mutableStateOf(settings.printWorkOrderCopies) }
    var printWorkOrderHeader by remember { mutableStateOf(settings.printWorkOrderHeader) }
    var printWorkOrderFooter by remember { mutableStateOf(settings.printWorkOrderFooter) }
    var printWorkOrderFont by remember { mutableStateOf(settings.printWorkOrderFont) }
    var printWorkOrderTextSize by remember { mutableStateOf(settings.printWorkOrderTextSize) }
    var printWorkOrderHeaderBold by remember { mutableStateOf(settings.printWorkOrderHeaderBold) }
    var printWorkOrderHeaderAlign by remember { mutableStateOf(settings.printWorkOrderHeaderAlign) }
    var showPrintModel by remember { mutableStateOf(false) }
    var workOrderTemplate by remember { mutableStateOf(settings.workOrderTemplate) }
    var workOrderOpenTemplate by remember { mutableStateOf(settings.workOrderOpenTemplate) }
    var workOrderInProgressTemplate by remember { mutableStateOf(settings.workOrderInProgressTemplate) }
    var workOrderCompletedTemplate by remember { mutableStateOf(settings.workOrderCompletedTemplate) }
    var workOrderCanceledTemplate by remember { mutableStateOf(settings.workOrderCanceledTemplate) }
    var reviewRequestTemplate by remember { mutableStateOf(settings.reviewRequestTemplate) }
    var pickupReminderTemplate by remember { mutableStateOf(settings.pickupReminderTemplate) }
    var quoteTemplate by remember { mutableStateOf(settings.quoteTemplate) }
    var bluetoothPrinters by remember { mutableStateOf(emptyList<BluetoothPrinterDevice>()) }
    var bluetoothMessage by remember { mutableStateOf<String?>(null) }
    fun loadBluetoothPrinters() {
        bluetoothPrinters = BluetoothThermalPrinter.listBondedPrinters(context)
        bluetoothMessage = if (bluetoothPrinters.isEmpty()) {
            "Nenhuma impressora pareada encontrada."
        } else {
            "${bluetoothPrinters.size} dispositivo(s) pareado(s) encontrado(s)."
        }
    }
    val readContactsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.loadContactAccounts()
        }
    }
    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            loadBluetoothPrinters()
        } else {
            bluetoothMessage = "Permissao Bluetooth negada."
        }
    }
    fun testBluetoothPrinter() {
        if (!BluetoothThermalPrinter.hasBluetoothPermission(context)) {
            bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            bluetoothMessage = "Permita Bluetooth e toque em testar novamente."
            return
        }
        coroutineScope.launch {
            bluetoothMessage = BluetoothThermalPrinter.print58mm(
                context = context,
                deviceAddress = printBluetoothAddress,
                content = ThermalPrintContent(
                    header = printWorkOrderHeader
                        .replace("{empresa}", companyName.ifBlank { "OS Mobile" })
                        .replace("{os}", "TESTE")
                        .replace("{data}", "10/07/2026")
                        .replace("{nome}", "Cliente Teste")
                        .replace("{telefone}", "35999999999")
                        .replace("{valor}", "R$ 20,00")
                        .replace("{status}", "Aberta"),
                    body = "It is Work\nTeste OS Mobile\nItem teste\n1 x R$ 20,00 = R$ 20,00",
                    footer = printWorkOrderFooter
                        .replace("{empresa}", companyName.ifBlank { "OS Mobile" })
                        .replace("{os}", "TESTE")
                        .replace("{data}", "10/07/2026")
                        .replace("{nome}", "Cliente Teste")
                        .replace("{telefone}", "35999999999")
                        .replace("{valor}", "R$ 20,00")
                        .replace("{status}", "Aberta"),
                ),
                copies = 1,
                style = ThermalPrintStyle(
                    font = printWorkOrderFont,
                    textSize = printWorkOrderTextSize,
                    headerBold = printWorkOrderHeaderBold,
                    headerAlignment = printWorkOrderHeaderAlign,
                ),
            ).fold(
                onSuccess = { "Teste enviado para a impressora." },
                onFailure = { "Falha no teste: ${it.message ?: "verifique a impressora selecionada"}" },
            )
        }
    }
    LaunchedEffect(settings) {
        companyName = settings.companyName
        pixName = settings.pixName
        pixKey = settings.pixKey
        fiscalEnvironment = settings.fiscalEnvironment
        fiscalProvider = settings.fiscalProvider
        fiscalApiToken = settings.fiscalApiToken
        fiscalCnpj = settings.fiscalCnpj
        fiscalIe = settings.fiscalIe
        fiscalIm = settings.fiscalIm
        fiscalRegime = settings.fiscalRegime
        printBluetoothAddress = settings.printBluetoothAddress
        printWorkOrderAuto = settings.printWorkOrderAuto
        printWorkOrderCopies = settings.printWorkOrderCopies
        printWorkOrderHeader = settings.printWorkOrderHeader
        printWorkOrderFooter = settings.printWorkOrderFooter
        printWorkOrderFont = settings.printWorkOrderFont
        printWorkOrderTextSize = settings.printWorkOrderTextSize
        printWorkOrderHeaderBold = settings.printWorkOrderHeaderBold
        printWorkOrderHeaderAlign = settings.printWorkOrderHeaderAlign
        workOrderTemplate = settings.workOrderTemplate
        workOrderOpenTemplate = settings.workOrderOpenTemplate
        workOrderInProgressTemplate = settings.workOrderInProgressTemplate
        workOrderCompletedTemplate = settings.workOrderCompletedTemplate
        workOrderCanceledTemplate = settings.workOrderCanceledTemplate
        reviewRequestTemplate = settings.reviewRequestTemplate
        pickupReminderTemplate = settings.pickupReminderTemplate
        quoteTemplate = settings.quoteTemplate
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Modulos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        SettingSwitch("Orcamento", checked = settings.orcamento, onCheckedChange = { viewModel.setModule("modulo_orcamento", it) })
        SettingSwitch("Fotos", checked = settings.fotos, onCheckedChange = { viewModel.setModule("modulo_fotos", it) })
        SettingSwitch("Assinatura", checked = settings.assinatura, onCheckedChange = { viewModel.setModule("modulo_assinatura", it) })
        SettingSwitch("Checklist", checked = settings.checklist, onCheckedChange = { viewModel.setModule("modulo_checklist", it) })
        SettingSwitch("Garantia", checked = settings.garantia, onCheckedChange = { viewModel.setModule("modulo_garantia", it) })
        SettingSwitch("Financeiro", checked = settings.financeiro, onCheckedChange = { viewModel.setModule("modulo_financeiro", it) })
        SettingSwitch("Fiscal", checked = settings.fiscal, onCheckedChange = { viewModel.setModule("modulo_fiscal", it) })

        Text("CPF/CNPJ", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PolicyButton("Nao usar", settings.cpfCnpjPolicy == CpfCnpjPolicy.NotUsed, modifier = Modifier.weight(1f)) {
                viewModel.setCpfCnpjPolicy(CpfCnpjPolicy.NotUsed)
            }
            PolicyButton("Opcional", settings.cpfCnpjPolicy == CpfCnpjPolicy.Optional, modifier = Modifier.weight(1f)) {
                viewModel.setCpfCnpjPolicy(CpfCnpjPolicy.Optional)
            }
            PolicyButton("Obrigatorio", settings.cpfCnpjPolicy == CpfCnpjPolicy.Required, modifier = Modifier.weight(1f)) {
                viewModel.setCpfCnpjPolicy(CpfCnpjPolicy.Required)
            }
        }

        Text("Agenda", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    viewModel.loadContactAccounts()
                } else {
                    readContactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Buscar agendas do aparelho")
        }
        ContactAccountSelector(
            selectedAccount = settings.contactsGoogleAccount,
            accounts = settings.contactAccounts,
            onSelected = viewModel::setContactsGoogleAccount,
        )
        settings.contactsMessage?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = "Selecione uma conta configurada no Android ou use a agenda local.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Impressao", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedButton(
            onClick = {
                if (BluetoothThermalPrinter.hasBluetoothPermission(context)) {
                    loadBluetoothPrinters()
                } else {
                    bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Buscar impressoras Bluetooth")
        }
        BluetoothPrinterSelector(
            selectedAddress = printBluetoothAddress,
            printers = bluetoothPrinters,
            onSelected = { printBluetoothAddress = it },
        )
        SettingSwitch(
            label = "Imprimir OS automaticamente",
            checked = printWorkOrderAuto,
            onCheckedChange = { printWorkOrderAuto = it },
        )
        OutlinedTextField(
            value = printWorkOrderCopies,
            onValueChange = { printWorkOrderCopies = it.filter(Char::isDigit).take(1) },
            label = { Text("Quantidade de vias da OS") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Use 0 para desativar a impressao da OS. Acima de 0 habilita o botao de imprimir e define a quantidade de vias.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { showPrintModel = !showPrintModel },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (showPrintModel) "Fechar modelo da OS" else "Configurar modelo da OS")
        }
        if (showPrintModel) {
            OutlinedTextField(
                value = printWorkOrderHeader,
                onValueChange = { printWorkOrderHeader = it },
                label = { Text("Cabecalho da OS") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = printWorkOrderFooter,
                onValueChange = { printWorkFooter -> printWorkOrderFooter = printWorkFooter },
                label = { Text("Rodape da OS") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            PrintOptionSelector(
                label = "Fonte",
                selectedValue = printWorkOrderFont,
                options = listOf("A" to "Fonte A", "B" to "Fonte B"),
                onSelected = { printWorkOrderFont = it },
            )
            PrintOptionSelector(
                label = "Tamanho",
                selectedValue = printWorkOrderTextSize,
                options = listOf("normal" to "Normal", "large" to "Grande"),
                onSelected = { printWorkOrderTextSize = it },
            )
            PrintOptionSelector(
                label = "Alinhamento do cabecalho",
                selectedValue = printWorkOrderHeaderAlign,
                options = listOf("left" to "Esquerda", "center" to "Centro", "right" to "Direita"),
                onSelected = { printWorkOrderHeaderAlign = it },
            )
            SettingSwitch(
                label = "Cabecalho em negrito",
                checked = printWorkOrderHeaderBold,
                onCheckedChange = { printWorkOrderHeaderBold = it },
            )
            Text(
                text = "Tokens: {empresa}, {data}, {os}, {nome}, {telefone}, {valor}, {valor_pago}, {saldo}, {status_pagamento}, {status}.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = {
                viewModel.setPrintSettings(
                    printBluetoothAddress,
                    printWorkOrderAuto,
                    printWorkOrderCopies,
                    printWorkOrderHeader,
                    printWorkOrderFooter,
                    printWorkOrderFont,
                    printWorkOrderTextSize,
                    printWorkOrderHeaderBold,
                    printWorkOrderHeaderAlign,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Salvar impressao")
        }
        OutlinedButton(
            onClick = ::testBluetoothPrinter,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Testar impressao direta")
        }
        bluetoothMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }

        Text("Fiscal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        FiscalEnvironmentSelector(
            selected = fiscalEnvironment,
            onSelected = { fiscalEnvironment = it },
        )
        OutlinedTextField(
            value = fiscalProvider,
            onValueChange = { fiscalProvider = it },
            label = { Text("API fiscal/provedor") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = fiscalApiToken,
            onValueChange = { fiscalApiToken = it },
            label = { Text("Token da API fiscal") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = fiscalCnpj,
            onValueChange = { fiscalCnpj = it.filter(Char::isDigit).take(14) },
            label = { Text("CNPJ") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = fiscalIe,
                onValueChange = { fiscalIe = it },
                label = { Text("IE") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = fiscalIm,
                onValueChange = { fiscalIm = it },
                label = { Text("IM") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = fiscalRegime,
            onValueChange = { fiscalRegime = it },
            label = { Text("Regime tributario") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Emissao fiscal deve iniciar em homologacao via API fiscal externa.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Mensagens", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Empresa") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pixName,
            onValueChange = { pixName = it },
            label = { Text("Nome Pix") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pixKey,
            onValueChange = { pixKey = it },
            label = { Text("Chave Pix") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = workOrderTemplate,
            onValueChange = { workOrderTemplate = it },
            label = { Text("Template OS geral") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = workOrderOpenTemplate,
            onValueChange = { workOrderOpenTemplate = it },
            label = { Text("OS Aberta") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = workOrderInProgressTemplate,
            onValueChange = { workOrderInProgressTemplate = it },
            label = { Text("OS Em andamento") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = workOrderCompletedTemplate,
            onValueChange = { workOrderCompletedTemplate = it },
            label = { Text("OS Concluida") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = workOrderCanceledTemplate,
            onValueChange = { workOrderCanceledTemplate = it },
            label = { Text("OS Cancelada") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = reviewRequestTemplate,
            onValueChange = { reviewRequestTemplate = it },
            label = { Text("Solicitacao de avaliacao") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = pickupReminderTemplate,
            onValueChange = { pickupReminderTemplate = it },
            label = { Text("Aviso para retirada") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = quoteTemplate,
            onValueChange = { quoteTemplate = it },
            label = { Text("Template orcamento") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Tokens: {nome}, {telefone}, {cpf}, {os}, {orcamento}, {valor}, {valor_pago}, {saldo}, {status_pagamento}, {status}, {empresa}, {data}, {dias}, {PIX}, {PIX_QR}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    viewModel.setCompanyName(companyName)
                    viewModel.setPixData(pixName, pixKey)
                    viewModel.setFiscalSettings(
                        fiscalEnvironment,
                        fiscalProvider,
                        fiscalApiToken,
                        fiscalCnpj,
                        fiscalIe,
                        fiscalIm,
                        fiscalRegime,
                    )
                    viewModel.setWorkOrderTemplate(workOrderTemplate)
                    viewModel.setWorkOrderStatusTemplates(
                        workOrderOpenTemplate,
                        workOrderInProgressTemplate,
                        workOrderCompletedTemplate,
                        workOrderCanceledTemplate,
                    )
                    viewModel.setReviewRequestTemplate(reviewRequestTemplate)
                    viewModel.setPickupReminderTemplate(pickupReminderTemplate)
                    viewModel.setQuoteTemplate(quoteTemplate)
                },
                modifier = Modifier.weight(1f),
            ) {
                Text("Salvar mensagens")
            }
            OutlinedButton(onClick = viewModel::resetTemplates) {
                Text("Padrao")
            }
        }
    }
}

@Composable
private fun ContactAccountSelector(
    selectedAccount: String,
    accounts: List<br.com.sos.osmobile.data.repository.ContactAccount>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = accounts.firstOrNull { it.storageKey == selectedAccount || it.name == selectedAccount }?.label ?: "Agenda local do aparelho"

    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(selectedLabel)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(if (selectedAccount.isBlank()) "Agenda local do aparelho *" else "Agenda local do aparelho") },
            onClick = {
                onSelected("")
                expanded = false
            },
        )
        accounts.forEach { account ->
            DropdownMenuItem(
                text = { Text(if (account.storageKey == selectedAccount || account.name == selectedAccount) "${account.label} *" else account.label) },
                onClick = {
                onSelected(account.storageKey)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun BluetoothPrinterSelector(
    selectedAddress: String,
    printers: List<BluetoothPrinterDevice>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = printers.firstOrNull { it.address == selectedAddress }?.label
        ?: selectedAddress.takeIf { it.isNotBlank() }
        ?: "Nenhuma impressora selecionada"

    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text(selectedLabel)
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(if (selectedAddress.isBlank()) "Nenhuma impressora *" else "Nenhuma impressora") },
            onClick = {
                onSelected("")
                expanded = false
            },
        )
        printers.forEach { printer ->
            DropdownMenuItem(
                text = { Text(if (printer.address == selectedAddress) "${printer.label} *" else printer.label) },
                onClick = {
                    onSelected(printer.address)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun FiscalEnvironmentSelector(
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Homologacao", "Producao")
    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text("Ambiente: $selected")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(if (option == selected) "$option *" else option) },
                onClick = {
                    onSelected(option)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun PrintOptionSelector(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: label

    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
        Text("$label: $selectedLabel")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { (value, text) ->
            DropdownMenuItem(
                text = { Text(if (value == selectedValue) "$text *" else text) },
                onClick = {
                    onSelected(value)
                    expanded = false
                },
            )
        }
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PolicyButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Text(if (selected) "$label *" else label)
    }
}
