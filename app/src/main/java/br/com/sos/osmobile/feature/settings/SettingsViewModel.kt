package br.com.sos.osmobile.feature.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import br.com.sos.osmobile.data.backup.BackupRepository
import br.com.sos.osmobile.data.repository.ContactAccount
import br.com.sos.osmobile.data.repository.ContactsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.COMPANY_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CONTACTS_GOOGLE_ACCOUNT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.CPF_CNPJ_POLICY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.FISCAL_API_TOKEN_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.FISCAL_CNPJ_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.FISCAL_ENVIRONMENT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.FISCAL_IE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.FISCAL_IM_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.FISCAL_PROVIDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.FISCAL_REGIME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.DRIVE_ROOT_URI_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.DRIVE_SYNC_ENABLED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PIX_KEY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PIX_NAME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_BLUETOOTH_ADDRESS_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_AUTO_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_COPIES_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_FONT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_FOOTER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_ALIGN_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_BOLD_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_HEADER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.PRINT_WORK_ORDER_TEXT_SIZE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.QUOTE_MIN_ACCEPTANCE_VALUE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.QUOTE_MIN_DEPOSIT_VALUE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PICKUP_REMINDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_ANNOUNCEMENT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_APPOINTMENT_CREATED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_APPOINTMENT_REMINDER_1D_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_APPOINTMENT_REMINDER_2D_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_APPOINTMENT_REMINDER_TODAY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_DELIVERED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_NOT_DELIVERED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_ORDER_SENT_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_OUT_FOR_DELIVERY_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_CONFIRMED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_PENDING_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_PAYMENT_PROOF_REQUEST_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_QUOTE_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_QUOTE_EXPIRED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_QUOTE_REMINDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_REVIEW_REQUEST_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_THANK_YOU_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WELCOME_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_CANCELED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_COMPLETED_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_KEY
import br.com.sos.osmobile.data.repository.SettingsRepository.Companion.TEMPLATE_WORK_ORDER_OPEN_KEY
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.drive.DriveSyncResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val orcamento: Boolean = true,
    val fotos: Boolean = false,
    val assinatura: Boolean = false,
    val checklist: Boolean = false,
    val garantia: Boolean = false,
    val financeiro: Boolean = false,
    val fiscal: Boolean = false,
    val cpfCnpjPolicy: CpfCnpjPolicy = CpfCnpjPolicy.Optional,
    val contactsGoogleAccount: String = "",
    val companyName: String = "",
    val quoteMinAcceptanceValue: String = "",
    val quoteMinDepositValue: String = "",
    val pixName: String = "",
    val pixKey: String = "",
    val fiscalEnvironment: String = "Homologacao",
    val fiscalProvider: String = "",
    val fiscalApiToken: String = "",
    val fiscalCnpj: String = "",
    val fiscalIe: String = "",
    val fiscalIm: String = "",
    val fiscalRegime: String = "",
    val printBluetoothAddress: String = "",
    val printWorkOrderAuto: Boolean = false,
    val printWorkOrderCopies: String = "0",
    val printWorkOrderHeader: String = "{empresa}\nOS {os}\n{data}",
    val printWorkOrderFooter: String = "Obrigado pela preferencia",
    val printWorkOrderFont: String = "A",
    val printWorkOrderTextSize: String = "normal",
    val printWorkOrderHeaderBold: Boolean = true,
    val printWorkOrderHeaderAlign: String = "center",
    val workOrderTemplate: String = MessageTemplateRenderer.workOrderDefaultTemplate,
    val workOrderOpenTemplate: String = MessageTemplateRenderer.workOrderOpenTemplate,
    val workOrderInProgressTemplate: String = MessageTemplateRenderer.workOrderInProgressTemplate,
    val workOrderCompletedTemplate: String = MessageTemplateRenderer.workOrderCompletedTemplate,
    val workOrderCanceledTemplate: String = MessageTemplateRenderer.workOrderCanceledTemplate,
    val reviewRequestTemplate: String = MessageTemplateRenderer.reviewRequestTemplate,
    val pickupReminderTemplate: String = MessageTemplateRenderer.pickupReminderTemplate,
    val paymentPendingTemplate: String = MessageTemplateRenderer.paymentPendingTemplate,
    val paymentConfirmedTemplate: String = MessageTemplateRenderer.paymentConfirmedTemplate,
    val paymentProofRequestTemplate: String = MessageTemplateRenderer.paymentProofRequestTemplate,
    val orderSentTemplate: String = MessageTemplateRenderer.orderSentTemplate,
    val outForDeliveryTemplate: String = MessageTemplateRenderer.outForDeliveryTemplate,
    val deliveredTemplate: String = MessageTemplateRenderer.deliveredTemplate,
    val notDeliveredTemplate: String = MessageTemplateRenderer.notDeliveredTemplate,
    val thankYouTemplate: String = MessageTemplateRenderer.thankYouTemplate,
    val announcementTemplate: String = MessageTemplateRenderer.announcementTemplate,
    val welcomeTemplate: String = MessageTemplateRenderer.welcomeTemplate,
    val quoteExpiredTemplate: String = MessageTemplateRenderer.quoteExpiredTemplate,
    val quoteReminderTemplate: String = MessageTemplateRenderer.quoteReminderTemplate,
    val appointmentCreatedTemplate: String = MessageTemplateRenderer.appointmentCreatedTemplate,
    val appointmentReminder2DaysTemplate: String = MessageTemplateRenderer.appointmentReminder2DaysTemplate,
    val appointmentReminder1DayTemplate: String = MessageTemplateRenderer.appointmentReminder1DayTemplate,
    val appointmentReminderTodayTemplate: String = MessageTemplateRenderer.appointmentReminderTodayTemplate,
    val quoteTemplate: String = MessageTemplateRenderer.quoteDefaultTemplate,
    val contactAccounts: List<ContactAccount> = emptyList(),
    val contactsMessage: String? = null,
    val driveSyncEnabled: Boolean = false,
    val driveRootUri: String = "",
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val contactsRepository: ContactsRepository,
    private val backupRepository: BackupRepository,
    private val driveSyncRepository: DriveSyncRepository,
) : ViewModel() {
    private val contactAccounts = MutableStateFlow<List<ContactAccount>>(emptyList())
    private val contactsMessage = MutableStateFlow<String?>(null)

    var resetMessage by mutableStateOf<String?>(null)
        private set

    var driveSyncMessage by mutableStateOf<String?>(null)
        private set

    val settings = combine(
        settingsRepository.observeAll(),
        contactAccounts,
        contactsMessage,
    ) { entities, accounts, message ->
        Triple(entities, accounts, message)
    }
        .map { entities ->
            val settingsEntities = entities.first
            val values = settingsEntities.associate { it.chave to it.valor.toBooleanStrictOrNull() }
            val rawValues = settingsEntities.associate { it.chave to it.valor }
            SettingsUiState(
                orcamento = values["modulo_orcamento"] ?: true,
                fotos = values["modulo_fotos"] ?: false,
                assinatura = values["modulo_assinatura"] ?: false,
                checklist = values["modulo_checklist"] ?: false,
                garantia = values["modulo_garantia"] ?: false,
                financeiro = values["modulo_financeiro"] ?: false,
                fiscal = values["modulo_fiscal"] ?: false,
                cpfCnpjPolicy = CpfCnpjPolicy.fromStorage(
                    settingsEntities.firstOrNull { it.chave == CPF_CNPJ_POLICY_KEY }?.valor,
                ),
                contactsGoogleAccount = rawValues[CONTACTS_GOOGLE_ACCOUNT_KEY].orEmpty(),
                companyName = rawValues[COMPANY_NAME_KEY].orEmpty(),
                quoteMinAcceptanceValue = rawValues[QUOTE_MIN_ACCEPTANCE_VALUE_KEY].orEmpty(),
                quoteMinDepositValue = rawValues[QUOTE_MIN_DEPOSIT_VALUE_KEY].orEmpty(),
                pixName = rawValues[PIX_NAME_KEY].orEmpty(),
                pixKey = rawValues[PIX_KEY_KEY].orEmpty(),
                fiscalEnvironment = rawValues[FISCAL_ENVIRONMENT_KEY] ?: "Homologacao",
                fiscalProvider = rawValues[FISCAL_PROVIDER_KEY].orEmpty(),
                fiscalApiToken = rawValues[FISCAL_API_TOKEN_KEY].orEmpty(),
                fiscalCnpj = rawValues[FISCAL_CNPJ_KEY].orEmpty(),
                fiscalIe = rawValues[FISCAL_IE_KEY].orEmpty(),
                fiscalIm = rawValues[FISCAL_IM_KEY].orEmpty(),
                fiscalRegime = rawValues[FISCAL_REGIME_KEY].orEmpty(),
                printBluetoothAddress = rawValues[PRINT_BLUETOOTH_ADDRESS_KEY].orEmpty(),
                printWorkOrderAuto = values[PRINT_WORK_ORDER_AUTO_KEY] ?: false,
                printWorkOrderCopies = rawValues[PRINT_WORK_ORDER_COPIES_KEY]?.takeIf { it.isNotBlank() } ?: "0",
                printWorkOrderHeader = rawValues[PRINT_WORK_ORDER_HEADER_KEY] ?: "{empresa}\nOS {os}\n{data}",
                printWorkOrderFooter = rawValues[PRINT_WORK_ORDER_FOOTER_KEY] ?: "Obrigado pela preferencia",
                printWorkOrderFont = rawValues[PRINT_WORK_ORDER_FONT_KEY] ?: "A",
                printWorkOrderTextSize = rawValues[PRINT_WORK_ORDER_TEXT_SIZE_KEY] ?: "normal",
                printWorkOrderHeaderBold = values[PRINT_WORK_ORDER_HEADER_BOLD_KEY] ?: true,
                printWorkOrderHeaderAlign = rawValues[PRINT_WORK_ORDER_HEADER_ALIGN_KEY] ?: "center",
                workOrderTemplate = rawValues[TEMPLATE_WORK_ORDER_KEY] ?: MessageTemplateRenderer.workOrderDefaultTemplate,
                workOrderOpenTemplate = rawValues[TEMPLATE_WORK_ORDER_OPEN_KEY] ?: MessageTemplateRenderer.workOrderOpenTemplate,
                workOrderInProgressTemplate = rawValues[TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY] ?: MessageTemplateRenderer.workOrderInProgressTemplate,
                workOrderCompletedTemplate = rawValues[TEMPLATE_WORK_ORDER_COMPLETED_KEY] ?: MessageTemplateRenderer.workOrderCompletedTemplate,
                workOrderCanceledTemplate = rawValues[TEMPLATE_WORK_ORDER_CANCELED_KEY] ?: MessageTemplateRenderer.workOrderCanceledTemplate,
                reviewRequestTemplate = rawValues[TEMPLATE_REVIEW_REQUEST_KEY] ?: MessageTemplateRenderer.reviewRequestTemplate,
                pickupReminderTemplate = rawValues[TEMPLATE_PICKUP_REMINDER_KEY] ?: MessageTemplateRenderer.pickupReminderTemplate,
                paymentPendingTemplate = rawValues[TEMPLATE_PAYMENT_PENDING_KEY] ?: MessageTemplateRenderer.paymentPendingTemplate,
                paymentConfirmedTemplate = rawValues[TEMPLATE_PAYMENT_CONFIRMED_KEY] ?: MessageTemplateRenderer.paymentConfirmedTemplate,
                paymentProofRequestTemplate = rawValues[TEMPLATE_PAYMENT_PROOF_REQUEST_KEY] ?: MessageTemplateRenderer.paymentProofRequestTemplate,
                orderSentTemplate = rawValues[TEMPLATE_ORDER_SENT_KEY] ?: MessageTemplateRenderer.orderSentTemplate,
                outForDeliveryTemplate = rawValues[TEMPLATE_OUT_FOR_DELIVERY_KEY] ?: MessageTemplateRenderer.outForDeliveryTemplate,
                deliveredTemplate = rawValues[TEMPLATE_DELIVERED_KEY] ?: MessageTemplateRenderer.deliveredTemplate,
                notDeliveredTemplate = rawValues[TEMPLATE_NOT_DELIVERED_KEY] ?: MessageTemplateRenderer.notDeliveredTemplate,
                thankYouTemplate = rawValues[TEMPLATE_THANK_YOU_KEY] ?: MessageTemplateRenderer.thankYouTemplate,
                announcementTemplate = rawValues[TEMPLATE_ANNOUNCEMENT_KEY] ?: MessageTemplateRenderer.announcementTemplate,
                welcomeTemplate = rawValues[TEMPLATE_WELCOME_KEY] ?: MessageTemplateRenderer.welcomeTemplate,
                quoteExpiredTemplate = rawValues[TEMPLATE_QUOTE_EXPIRED_KEY] ?: MessageTemplateRenderer.quoteExpiredTemplate,
                quoteReminderTemplate = rawValues[TEMPLATE_QUOTE_REMINDER_KEY] ?: MessageTemplateRenderer.quoteReminderTemplate,
                appointmentCreatedTemplate = rawValues[TEMPLATE_APPOINTMENT_CREATED_KEY] ?: MessageTemplateRenderer.appointmentCreatedTemplate,
                appointmentReminder2DaysTemplate = rawValues[TEMPLATE_APPOINTMENT_REMINDER_2D_KEY] ?: MessageTemplateRenderer.appointmentReminder2DaysTemplate,
                appointmentReminder1DayTemplate = rawValues[TEMPLATE_APPOINTMENT_REMINDER_1D_KEY] ?: MessageTemplateRenderer.appointmentReminder1DayTemplate,
                appointmentReminderTodayTemplate = rawValues[TEMPLATE_APPOINTMENT_REMINDER_TODAY_KEY] ?: MessageTemplateRenderer.appointmentReminderTodayTemplate,
                quoteTemplate = rawValues[TEMPLATE_QUOTE_KEY] ?: MessageTemplateRenderer.quoteDefaultTemplate,
                contactAccounts = entities.second,
                contactsMessage = entities.third,
                driveSyncEnabled = values[DRIVE_SYNC_ENABLED_KEY] ?: false,
                driveRootUri = rawValues[DRIVE_ROOT_URI_KEY].orEmpty(),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setModule(key: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.set(key, enabled.toString())
        }
    }

    fun setCpfCnpjPolicy(policy: CpfCnpjPolicy) {
        viewModelScope.launch {
            settingsRepository.set(CPF_CNPJ_POLICY_KEY, policy.storageValue)
        }
    }

    fun setContactsGoogleAccount(value: String) {
        viewModelScope.launch {
            settingsRepository.set(CONTACTS_GOOGLE_ACCOUNT_KEY, value.trim())
        }
    }

    fun setDriveSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.set(DRIVE_SYNC_ENABLED_KEY, enabled.toString())
            driveSyncMessage = if (enabled) {
                "Sincronizacao do Drive ativada. Selecione uma pasta e sincronize pendentes."
            } else {
                "Sincronizacao do Drive desativada."
            }
        }
    }

    fun setDriveRootUri(uri: String) {
        viewModelScope.launch {
            settingsRepository.set(DRIVE_ROOT_URI_KEY, uri)
            settingsRepository.set(DRIVE_SYNC_ENABLED_KEY, true.toString())
            driveSyncMessage = "Pasta do Drive configurada. Sincronizacao pendente sera tentada automaticamente."
        }
    }

    fun syncDrivePending() {
        viewModelScope.launch {
            driveSyncMessage = "Sincronizando pendentes do Drive..."
            driveSyncMessage = when (val result = driveSyncRepository.syncAllPending()) {
                is DriveSyncResult.Done -> "Sincronizacao concluida: ${result.syncedItems} item(ns)."
                is DriveSyncResult.Skipped -> result.reason
            }
        }
    }

    fun setCompanyName(value: String) {
        viewModelScope.launch {
            settingsRepository.set(COMPANY_NAME_KEY, value.trim())
        }
    }

    fun setQuoteMinAcceptanceValue(value: String) {
        viewModelScope.launch {
            settingsRepository.set(QUOTE_MIN_ACCEPTANCE_VALUE_KEY, value.trim())
        }
    }

    fun setQuoteMinDepositValue(value: String) {
        viewModelScope.launch {
            settingsRepository.set(QUOTE_MIN_DEPOSIT_VALUE_KEY, value.trim())
        }
    }

    fun setPixData(name: String, key: String) {
        viewModelScope.launch {
            settingsRepository.set(PIX_NAME_KEY, name.trim())
            settingsRepository.set(PIX_KEY_KEY, key.trim())
        }
    }

    fun setFiscalSettings(
        environment: String,
        provider: String,
        apiToken: String,
        cnpj: String,
        ie: String,
        im: String,
        regime: String,
    ) {
        viewModelScope.launch {
            settingsRepository.set(FISCAL_ENVIRONMENT_KEY, environment)
            settingsRepository.set(FISCAL_PROVIDER_KEY, provider.trim())
            settingsRepository.set(FISCAL_API_TOKEN_KEY, apiToken.trim())
            settingsRepository.set(FISCAL_CNPJ_KEY, cnpj.trim())
            settingsRepository.set(FISCAL_IE_KEY, ie.trim())
            settingsRepository.set(FISCAL_IM_KEY, im.trim())
            settingsRepository.set(FISCAL_REGIME_KEY, regime.trim())
        }
    }

    fun setPrintSettings(
        bluetoothAddress: String,
        autoWorkOrder: Boolean,
        workOrderCopies: String,
        workOrderHeader: String,
        workOrderFooter: String,
        workOrderFont: String,
        workOrderTextSize: String,
        workOrderHeaderBold: Boolean,
        workOrderHeaderAlign: String,
    ) {
        viewModelScope.launch {
            settingsRepository.set(PRINT_BLUETOOTH_ADDRESS_KEY, bluetoothAddress.trim())
            settingsRepository.set(PRINT_WORK_ORDER_AUTO_KEY, autoWorkOrder.toString())
            settingsRepository.set(PRINT_WORK_ORDER_COPIES_KEY, (workOrderCopies.toIntOrNull() ?: 0).coerceIn(0, 9).toString())
            settingsRepository.set(PRINT_WORK_ORDER_HEADER_KEY, workOrderHeader.trim())
            settingsRepository.set(PRINT_WORK_ORDER_FOOTER_KEY, workOrderFooter.trim())
            settingsRepository.set(PRINT_WORK_ORDER_FONT_KEY, workOrderFont)
            settingsRepository.set(PRINT_WORK_ORDER_TEXT_SIZE_KEY, workOrderTextSize)
            settingsRepository.set(PRINT_WORK_ORDER_HEADER_BOLD_KEY, workOrderHeaderBold.toString())
            settingsRepository.set(PRINT_WORK_ORDER_HEADER_ALIGN_KEY, workOrderHeaderAlign)
        }
    }

    fun setWorkOrderTemplate(value: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_WORK_ORDER_KEY, value.trim())
        }
    }

    fun setQuoteTemplate(value: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_QUOTE_KEY, value.trim())
        }
    }

    fun setReviewRequestTemplate(value: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_REVIEW_REQUEST_KEY, value.trim())
        }
    }

    fun setPickupReminderTemplate(value: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_PICKUP_REMINDER_KEY, value.trim())
        }
    }

    fun setExtraMessageTemplates(
        paymentPending: String,
        paymentConfirmed: String,
        paymentProofRequest: String,
        orderSent: String,
        outForDelivery: String,
        delivered: String,
        notDelivered: String,
        thankYou: String,
        announcement: String,
        welcome: String,
        quoteExpired: String,
        quoteReminder: String,
    ) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_PAYMENT_PENDING_KEY, paymentPending.trim())
            settingsRepository.set(TEMPLATE_PAYMENT_CONFIRMED_KEY, paymentConfirmed.trim())
            settingsRepository.set(TEMPLATE_PAYMENT_PROOF_REQUEST_KEY, paymentProofRequest.trim())
            settingsRepository.set(TEMPLATE_ORDER_SENT_KEY, orderSent.trim())
            settingsRepository.set(TEMPLATE_OUT_FOR_DELIVERY_KEY, outForDelivery.trim())
            settingsRepository.set(TEMPLATE_DELIVERED_KEY, delivered.trim())
            settingsRepository.set(TEMPLATE_NOT_DELIVERED_KEY, notDelivered.trim())
            settingsRepository.set(TEMPLATE_THANK_YOU_KEY, thankYou.trim())
            settingsRepository.set(TEMPLATE_ANNOUNCEMENT_KEY, announcement.trim())
            settingsRepository.set(TEMPLATE_WELCOME_KEY, welcome.trim())
            settingsRepository.set(TEMPLATE_QUOTE_EXPIRED_KEY, quoteExpired.trim())
            settingsRepository.set(TEMPLATE_QUOTE_REMINDER_KEY, quoteReminder.trim())
        }
    }

    fun setAppointmentTemplates(created: String, reminder2Days: String, reminder1Day: String, reminderToday: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_APPOINTMENT_CREATED_KEY, created.trim())
            settingsRepository.set(TEMPLATE_APPOINTMENT_REMINDER_2D_KEY, reminder2Days.trim())
            settingsRepository.set(TEMPLATE_APPOINTMENT_REMINDER_1D_KEY, reminder1Day.trim())
            settingsRepository.set(TEMPLATE_APPOINTMENT_REMINDER_TODAY_KEY, reminderToday.trim())
        }
    }

    fun setWorkOrderStatusTemplates(open: String, inProgress: String, completed: String, canceled: String) {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_WORK_ORDER_OPEN_KEY, open.trim())
            settingsRepository.set(TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY, inProgress.trim())
            settingsRepository.set(TEMPLATE_WORK_ORDER_COMPLETED_KEY, completed.trim())
            settingsRepository.set(TEMPLATE_WORK_ORDER_CANCELED_KEY, canceled.trim())
        }
    }

    fun resetTemplates() {
        viewModelScope.launch {
            settingsRepository.set(TEMPLATE_WORK_ORDER_KEY, MessageTemplateRenderer.workOrderDefaultTemplate)
            settingsRepository.set(TEMPLATE_WORK_ORDER_OPEN_KEY, MessageTemplateRenderer.workOrderOpenTemplate)
            settingsRepository.set(TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY, MessageTemplateRenderer.workOrderInProgressTemplate)
            settingsRepository.set(TEMPLATE_WORK_ORDER_COMPLETED_KEY, MessageTemplateRenderer.workOrderCompletedTemplate)
            settingsRepository.set(TEMPLATE_WORK_ORDER_CANCELED_KEY, MessageTemplateRenderer.workOrderCanceledTemplate)
            settingsRepository.set(TEMPLATE_REVIEW_REQUEST_KEY, MessageTemplateRenderer.reviewRequestTemplate)
            settingsRepository.set(TEMPLATE_PICKUP_REMINDER_KEY, MessageTemplateRenderer.pickupReminderTemplate)
            settingsRepository.set(TEMPLATE_PAYMENT_PENDING_KEY, MessageTemplateRenderer.paymentPendingTemplate)
            settingsRepository.set(TEMPLATE_PAYMENT_CONFIRMED_KEY, MessageTemplateRenderer.paymentConfirmedTemplate)
            settingsRepository.set(TEMPLATE_PAYMENT_PROOF_REQUEST_KEY, MessageTemplateRenderer.paymentProofRequestTemplate)
            settingsRepository.set(TEMPLATE_ORDER_SENT_KEY, MessageTemplateRenderer.orderSentTemplate)
            settingsRepository.set(TEMPLATE_OUT_FOR_DELIVERY_KEY, MessageTemplateRenderer.outForDeliveryTemplate)
            settingsRepository.set(TEMPLATE_DELIVERED_KEY, MessageTemplateRenderer.deliveredTemplate)
            settingsRepository.set(TEMPLATE_NOT_DELIVERED_KEY, MessageTemplateRenderer.notDeliveredTemplate)
            settingsRepository.set(TEMPLATE_THANK_YOU_KEY, MessageTemplateRenderer.thankYouTemplate)
            settingsRepository.set(TEMPLATE_ANNOUNCEMENT_KEY, MessageTemplateRenderer.announcementTemplate)
            settingsRepository.set(TEMPLATE_WELCOME_KEY, MessageTemplateRenderer.welcomeTemplate)
            settingsRepository.set(TEMPLATE_QUOTE_EXPIRED_KEY, MessageTemplateRenderer.quoteExpiredTemplate)
            settingsRepository.set(TEMPLATE_QUOTE_REMINDER_KEY, MessageTemplateRenderer.quoteReminderTemplate)
            settingsRepository.set(TEMPLATE_QUOTE_KEY, MessageTemplateRenderer.quoteDefaultTemplate)
            settingsRepository.set(TEMPLATE_APPOINTMENT_CREATED_KEY, MessageTemplateRenderer.appointmentCreatedTemplate)
            settingsRepository.set(TEMPLATE_APPOINTMENT_REMINDER_2D_KEY, MessageTemplateRenderer.appointmentReminder2DaysTemplate)
            settingsRepository.set(TEMPLATE_APPOINTMENT_REMINDER_1D_KEY, MessageTemplateRenderer.appointmentReminder1DayTemplate)
            settingsRepository.set(TEMPLATE_APPOINTMENT_REMINDER_TODAY_KEY, MessageTemplateRenderer.appointmentReminderTodayTemplate)
        }
    }

    fun resetOperationalData(confirmation: String) {
        if (confirmation != "ZERAR") {
            resetMessage = "Digite ZERAR para confirmar."
            return
        }
        viewModelScope.launch {
            resetMessage = runCatching {
                backupRepository.resetOperationalData()
            }.fold(
                onSuccess = {
                    "Dados zerados: ${it.customers} cliente(s), ${it.services} servico(s), ${it.quotes} orcamento(s), ${it.workOrders} OS. Configuracoes mantidas."
                },
                onFailure = {
                    "Nao foi possivel zerar os dados: ${it.message ?: "erro desconhecido"}"
                },
            )
        }
    }

    fun loadContactAccounts() {
        viewModelScope.launch {
            runCatching {
                contactsRepository.listContactAccounts()
            }.fold(
                onSuccess = {
                    contactAccounts.value = it
                    contactsMessage.value = if (it.isEmpty()) {
                        "Nenhuma conta de contatos encontrada. Use agenda local ou verifique a sincronizacao do aparelho."
                    } else {
                        "${it.size} agenda(s) encontrada(s)."
                    }
                },
                onFailure = {
                    contactsMessage.value = "Nao foi possivel ler as agendas: ${it.message ?: "verifique a permissao"}"
                },
            )
        }
    }

    companion object {
        fun factory(
            repository: SettingsRepository,
            contactsRepository: ContactsRepository,
            backupRepository: BackupRepository,
            driveSyncRepository: DriveSyncRepository,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(repository, contactsRepository, backupRepository, driveSyncRepository) as T
                }
            }
    }
}
