package br.com.sos.osmobile.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.documentfile.provider.DocumentFile
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.data.repository.CalendarAccount
import br.com.sos.osmobile.data.print.BluetoothPrinterDevice
import br.com.sos.osmobile.data.print.BluetoothThermalPrinter
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalPrintStyle
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val calendars by viewModel.calendars.collectAsState()
    val calendarMessage by viewModel.calendarStatusMessage.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val driveRootName = remember(context, settings.driveRootUri) {
        driveFolderDisplayName(context, settings.driveRootUri)
    }
    var companyName by remember { mutableStateOf(settings.companyName) }
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
    var resetConfirmation by remember { mutableStateOf("") }
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
    val calendarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted[Manifest.permission.READ_CALENDAR] == true && granted[Manifest.permission.WRITE_CALENDAR] == true) {
            viewModel.loadCalendars()
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
    val driveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            val selectedFolder = DocumentFile.fromTreeUri(context, it)
            if (selectedFolder == null || !selectedFolder.canWrite()) {
                viewModel.rejectDriveRootSelection("Nao foi possivel usar esta pasta. Selecione uma pasta gravavel dentro do Meu Drive.")
                return@let
            }
            if (selectedFolder.isDriveRootLike()) {
                viewModel.rejectDriveRootSelection("Selecione uma pasta dentro do Meu Drive, como ClientesTopEstampa. A raiz Meu Drive nao deve ser usada.")
                return@let
            }
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }
            viewModel.setDriveRootUri(it.toString())
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
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SettingsSection("Modulos", initiallyExpanded = true) {
        SettingSwitch("Orcamento", checked = settings.orcamento, onCheckedChange = { viewModel.setModule("modulo_orcamento", it) })
        SettingSwitch("Imagens", checked = settings.fotos, onCheckedChange = { viewModel.setModule("modulo_fotos", it) })
        SettingSwitch("Assinatura", checked = settings.assinatura, onCheckedChange = { viewModel.setModule("modulo_assinatura", it) })
        SettingSwitch("Checklist", checked = settings.checklist, onCheckedChange = { viewModel.setModule("modulo_checklist", it) })
        SettingSwitch("Garantia", checked = settings.garantia, onCheckedChange = { viewModel.setModule("modulo_garantia", it) })
        SettingSwitch("Agenda/Agendamentos", checked = settings.agenda, onCheckedChange = { viewModel.setModule("modulo_agenda", it) })
        SettingSwitch("Financeiro", checked = settings.financeiro, onCheckedChange = { viewModel.setModule("modulo_financeiro", it) })
        SettingSwitch("Fiscal", checked = settings.fiscal, onCheckedChange = { viewModel.setModule("modulo_fiscal", it) })

        SettingSwitch(
            label = "Usar CPF/CNPJ",
            checked = settings.cpfCnpjPolicy != CpfCnpjPolicy.NotUsed,
            onCheckedChange = {
                viewModel.setCpfCnpjPolicy(if (it) CpfCnpjPolicy.Optional else CpfCnpjPolicy.NotUsed)
            },
        )
        if (settings.cpfCnpjPolicy != CpfCnpjPolicy.NotUsed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PolicyButton("Opcional", settings.cpfCnpjPolicy == CpfCnpjPolicy.Optional, modifier = Modifier.weight(1f)) {
                    viewModel.setCpfCnpjPolicy(CpfCnpjPolicy.Optional)
                }
                PolicyButton("Obrigatorio", settings.cpfCnpjPolicy == CpfCnpjPolicy.Required, modifier = Modifier.weight(1f)) {
                    viewModel.setCpfCnpjPolicy(CpfCnpjPolicy.Required)
                }
            }
        }
        }

        if (settings.agenda) {
            SettingsSection("Agenda") {
                OutlinedButton(
                    onClick = {
                        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                        val hasWrite = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                        if (hasRead && hasWrite) {
                            viewModel.loadCalendars()
                        } else {
                            calendarLauncher.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Buscar calendarios do aparelho")
                }
                CalendarSelector(
                    selectedCalendarId = settings.calendarId,
                    selectedCalendarLabel = settings.calendarLabel,
                    calendars = calendars,
                    onSelected = viewModel::setDefaultCalendar,
                )
                Text(
                    text = "Agenda padrao: ${settings.calendarLabel.ifBlank { "nenhuma agenda selecionada" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                calendarMessage?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
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
                    Text("Buscar agendas de contatos do aparelho")
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
                    text = "Selecione tambem a agenda de contatos para salvar clientes no aparelho.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SettingsSection("Google Drive") {
        SettingSwitch(
            label = "Sincronizar documentos no Drive",
            checked = settings.driveSyncEnabled,
            onCheckedChange = viewModel::setDriveSyncEnabled,
        )
        OutlinedButton(
            onClick = { driveFolderLauncher.launch(null) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (settings.driveRootUri.isBlank()) "Selecionar pasta do Drive" else "Alterar pasta do Drive")
        }
        OutlinedButton(
            onClick = viewModel::syncDrivePending,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sincronizar pendentes agora")
        }
        Text(
            text = "Pasta para receber arquivos: $driveRootName",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        viewModel.driveSyncMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Text(
            text = if (settings.driveRootUri.isBlank()) {
                "Entre em Meu Drive, crie/selecione uma pasta como OS Mobile e toque em Usar esta pasta. A raiz do Drive nao pode ser usada."
            } else {
                "Status: pasta configurada. O app cria Cliente/OS/Imagens/Documentos e tenta sincronizar quando houver internet."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        }

        SettingsSection("Impressao") {
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
            label = "Imprimir etiqueta automaticamente ao criar OS",
            checked = printWorkOrderAuto,
            onCheckedChange = { printWorkOrderAuto = it },
        )
        OutlinedTextField(
            value = printWorkOrderCopies,
            onValueChange = { printWorkOrderCopies = it.filter(Char::isDigit).take(1) },
            label = { Text("Quantidade de vias de impressao") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Use 0 para desativar a impressao. Acima de 0 habilita os botoes de imprimir e define a quantidade de vias.",
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
        }

        SettingsSection("Fiscal") {
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
        }


        MessagesSettingsSection(
            viewModel = viewModel,
            settings = settings,
            companyName = companyName,
            onCompanyNameChanged = { companyName = it },
            fiscalEnvironment = fiscalEnvironment,
            fiscalProvider = fiscalProvider,
            fiscalApiToken = fiscalApiToken,
            fiscalCnpj = fiscalCnpj,
            fiscalIe = fiscalIe,
            fiscalIm = fiscalIm,
            fiscalRegime = fiscalRegime,
        )

        SettingsSection("Area perigosa") {
        Text(
            text = "Use apenas para sair da fase de testes. Apaga clientes, servicos/produtos, OS, orcamentos, pagamentos, fotos, assinaturas, checklist, garantias, estoque, vendas e historico. Mantem configuracoes, templates, Pix e impressao.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        OutlinedTextField(
            value = resetConfirmation,
            onValueChange = { resetConfirmation = it.uppercase().take(5) },
            label = { Text("Digite ZERAR para confirmar") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = {
                viewModel.resetOperationalData(resetConfirmation)
                resetConfirmation = ""
            },
            enabled = resetConfirmation == "ZERAR",
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Zerar dados de teste")
        }
        viewModel.resetMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.secondary)
        }
        }
    }
}
