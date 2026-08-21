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
internal fun MessagesSettingsSection(
    viewModel: SettingsViewModel,
    settings: SettingsUiState,
    companyName: String,
    onCompanyNameChanged: (String) -> Unit,
    fiscalEnvironment: String,
    fiscalProvider: String,
    fiscalApiToken: String,
    fiscalCnpj: String,
    fiscalIe: String,
    fiscalIm: String,
    fiscalRegime: String,
) {
    var quoteMinAcceptanceValue by remember { mutableStateOf(settings.quoteMinAcceptanceValue) }
    var quoteMinDepositValue by remember { mutableStateOf(settings.quoteMinDepositValue) }
    var pixName by remember { mutableStateOf(settings.pixName) }
    var pixKey by remember { mutableStateOf(settings.pixKey) }
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
    LaunchedEffect(settings) {
        quoteMinAcceptanceValue = settings.quoteMinAcceptanceValue
        quoteMinDepositValue = settings.quoteMinDepositValue
        pixName = settings.pixName
        pixKey = settings.pixKey
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
        SettingsSection("Mensagens") {
        OutlinedTextField(
            value = companyName,
            onValueChange = { onCompanyNameChanged(it) },
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
        }
}
