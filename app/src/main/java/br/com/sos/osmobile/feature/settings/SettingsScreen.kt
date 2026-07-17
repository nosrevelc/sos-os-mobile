package br.com.sos.osmobile.feature.settings

import android.Manifest
import android.content.Intent
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
import br.com.sos.osmobile.ui.input.InputMasks
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var companyName by remember { mutableStateOf(settings.companyName) }
    var quoteMinAcceptanceValue by remember { mutableStateOf(settings.quoteMinAcceptanceValue) }
    var quoteMinDepositValue by remember { mutableStateOf(settings.quoteMinDepositValue) }
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
    var paymentPendingTemplate by remember { mutableStateOf(settings.paymentPendingTemplate) }
    var paymentConfirmedTemplate by remember { mutableStateOf(settings.paymentConfirmedTemplate) }
    var paymentProofRequestTemplate by remember { mutableStateOf(settings.paymentProofRequestTemplate) }
    var orderSentTemplate by remember { mutableStateOf(settings.orderSentTemplate) }
    var outForDeliveryTemplate by remember { mutableStateOf(settings.outForDeliveryTemplate) }
    var deliveredTemplate by remember { mutableStateOf(settings.deliveredTemplate) }
    var notDeliveredTemplate by remember { mutableStateOf(settings.notDeliveredTemplate) }
    var thankYouTemplate by remember { mutableStateOf(settings.thankYouTemplate) }
    var announcementTemplate by remember { mutableStateOf(settings.announcementTemplate) }
    var welcomeTemplate by remember { mutableStateOf(settings.welcomeTemplate) }
    var quoteExpiredTemplate by remember { mutableStateOf(settings.quoteExpiredTemplate) }
    var quoteReminderTemplate by remember { mutableStateOf(settings.quoteReminderTemplate) }
    var appointmentCreatedTemplate by remember { mutableStateOf(settings.appointmentCreatedTemplate) }
    var appointmentReminder2DaysTemplate by remember { mutableStateOf(settings.appointmentReminder2DaysTemplate) }
    var appointmentReminder1DayTemplate by remember { mutableStateOf(settings.appointmentReminder1DayTemplate) }
    var appointmentReminderTodayTemplate by remember { mutableStateOf(settings.appointmentReminderTodayTemplate) }
    var quoteTemplate by remember { mutableStateOf(settings.quoteTemplate) }
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
        quoteMinAcceptanceValue = settings.quoteMinAcceptanceValue
        quoteMinDepositValue = settings.quoteMinDepositValue
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
        paymentPendingTemplate = settings.paymentPendingTemplate
        paymentConfirmedTemplate = settings.paymentConfirmedTemplate
        paymentProofRequestTemplate = settings.paymentProofRequestTemplate
        orderSentTemplate = settings.orderSentTemplate
        outForDeliveryTemplate = settings.outForDeliveryTemplate
        deliveredTemplate = settings.deliveredTemplate
        notDeliveredTemplate = settings.notDeliveredTemplate
        thankYouTemplate = settings.thankYouTemplate
        announcementTemplate = settings.announcementTemplate
        welcomeTemplate = settings.welcomeTemplate
        quoteExpiredTemplate = settings.quoteExpiredTemplate
        quoteReminderTemplate = settings.quoteReminderTemplate
        appointmentCreatedTemplate = settings.appointmentCreatedTemplate
        appointmentReminder2DaysTemplate = settings.appointmentReminder2DaysTemplate
        appointmentReminder1DayTemplate = settings.appointmentReminder1DayTemplate
        appointmentReminderTodayTemplate = settings.appointmentReminderTodayTemplate
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

        Text("Google Drive", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
        viewModel.driveSyncMessage?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
        }
        Text(
            text = if (settings.driveRootUri.isBlank()) {
                "Selecione uma pasta do Google Drive no Android. OS e anexos ficam pendentes ate configurar."
            } else {
                "Pasta configurada. O app cria Cliente/OS/Fotos/Comprovantes e tenta sincronizar quando houver internet."
            },
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
            value = quoteMinAcceptanceValue,
            onValueChange = { quoteMinAcceptanceValue = InputMasks.currency(it) },
            label = { Text("Valor minimo para aceite de orcamento") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = quoteMinDepositValue,
            onValueChange = { quoteMinDepositValue = InputMasks.currency(it) },
            label = { Text("Sinal minimo padrao do orcamento") },
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
            value = paymentPendingTemplate,
            onValueChange = { paymentPendingTemplate = it },
            label = { Text("Pagamento pendente") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = paymentConfirmedTemplate,
            onValueChange = { paymentConfirmedTemplate = it },
            label = { Text("Pagamento confirmado") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = paymentProofRequestTemplate,
            onValueChange = { paymentProofRequestTemplate = it },
            label = { Text("Solicitar comprovante") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = orderSentTemplate,
            onValueChange = { orderSentTemplate = it },
            label = { Text("Pedido enviado") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = outForDeliveryTemplate,
            onValueChange = { outForDeliveryTemplate = it },
            label = { Text("Saiu para entrega") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = deliveredTemplate,
            onValueChange = { deliveredTemplate = it },
            label = { Text("Entregue") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = notDeliveredTemplate,
            onValueChange = { notDeliveredTemplate = it },
            label = { Text("Nao entregue") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = thankYouTemplate,
            onValueChange = { thankYouTemplate = it },
            label = { Text("Agradecimento") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = announcementTemplate,
            onValueChange = { announcementTemplate = it },
            label = { Text("Comunicado") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = welcomeTemplate,
            onValueChange = { welcomeTemplate = it },
            label = { Text("Boas-vindas") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = quoteExpiredTemplate,
            onValueChange = { quoteExpiredTemplate = it },
            label = { Text("Orcamento expirado") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = quoteReminderTemplate,
            onValueChange = { quoteReminderTemplate = it },
            label = { Text("Lembrete de orcamento") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = appointmentCreatedTemplate,
            onValueChange = { appointmentCreatedTemplate = it },
            label = { Text("Agendamento criado") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = appointmentReminder2DaysTemplate,
            onValueChange = { appointmentReminder2DaysTemplate = it },
            label = { Text("Lembrete agendamento 2 dias") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = appointmentReminder1DayTemplate,
            onValueChange = { appointmentReminder1DayTemplate = it },
            label = { Text("Lembrete agendamento 1 dia") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = appointmentReminderTodayTemplate,
            onValueChange = { appointmentReminderTodayTemplate = it },
            label = { Text("Lembrete agendamento hoje") },
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
            text = "Tokens: {nome}, {telefone}, {cpf}, {os}, {orcamento}, {valor}, {subtotal}, {desconto}, {linha_desconto}, {valor_minimo_aceite}, {sinal_minimo}, {linha_sinal_minimo}, {valor_pago}, {saldo}, {status_pagamento}, {status}, {tipo_entrega}, {status_entrega}, {endereco_entrega}, {taxa_entrega}, {codigo_rastreio}, {empresa}, {data}, {dias}, {agendamento}, {agendamento_tipo}, {agendamento_status}, {agendamento_data}, {agendamento_hora}, {itens}, {servicos}, {produtos}, {PIX}, {PIX_SEM_VALOR}, {PIX_QR}.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    viewModel.setCompanyName(companyName)
                    viewModel.setQuoteMinAcceptanceValue(quoteMinAcceptanceValue)
                    viewModel.setQuoteMinDepositValue(quoteMinDepositValue)
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
                    viewModel.setAppointmentTemplates(
                        appointmentCreatedTemplate,
                        appointmentReminder2DaysTemplate,
                        appointmentReminder1DayTemplate,
                        appointmentReminderTodayTemplate,
                    )
                    viewModel.setExtraMessageTemplates(
                        paymentPendingTemplate,
                        paymentConfirmedTemplate,
                        paymentProofRequestTemplate,
                        orderSentTemplate,
                        outForDeliveryTemplate,
                        deliveredTemplate,
                        notDeliveredTemplate,
                        thankYouTemplate,
                        announcementTemplate,
                        welcomeTemplate,
                        quoteExpiredTemplate,
                        quoteReminderTemplate,
                    )
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

        Text("Area perigosa", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
