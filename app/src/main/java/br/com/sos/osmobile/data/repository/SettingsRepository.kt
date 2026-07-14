package br.com.sos.osmobile.data.repository

import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.dao.SettingsDao
import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import br.com.sos.osmobile.data.model.CpfCnpjPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
    private val settingsDao: SettingsDao,
    private val auditRepository: AuditRepository,
) {
    fun observeAll(): Flow<List<AppSettingEntity>> = settingsDao.observeAll()

    fun observeCpfCnpjPolicy(): Flow<CpfCnpjPolicy> =
        settingsDao.observeAll()
            .map { settings ->
                CpfCnpjPolicy.fromStorage(settings.firstOrNull { it.chave == CPF_CNPJ_POLICY_KEY }?.valor)
            }

    suspend fun set(key: String, value: String) {
        settingsDao.upsert(AppSettingEntity(key, value, Clock.nowMillis()))
        auditRepository.record("Configuracoes", "Configuracao alterada", "configuracoes", details = "$key=$value")
    }

    suspend fun getString(key: String): String? =
        settingsDao.findByKey(key)?.valor

    suspend fun setContactRawId(customerId: Long, rawContactId: Long) {
        set(contactRawIdKey(customerId), rawContactId.toString())
    }

    suspend fun getContactRawId(customerId: Long): Long? =
        getString(contactRawIdKey(customerId))?.toLongOrNull()

    companion object {
        const val CPF_CNPJ_POLICY_KEY = "cpf_cnpj_policy"
        const val CONTACTS_GOOGLE_ACCOUNT_KEY = "contacts_google_account"
        const val COMPANY_NAME_KEY = "company_name"
        const val PIX_NAME_KEY = "pix_name"
        const val PIX_KEY_KEY = "pix_key"
        const val FISCAL_ENVIRONMENT_KEY = "fiscal_environment"
        const val FISCAL_PROVIDER_KEY = "fiscal_provider"
        const val FISCAL_API_TOKEN_KEY = "fiscal_api_token"
        const val FISCAL_CNPJ_KEY = "fiscal_cnpj"
        const val FISCAL_IE_KEY = "fiscal_ie"
        const val FISCAL_IM_KEY = "fiscal_im"
        const val FISCAL_REGIME_KEY = "fiscal_regime"
        const val TEMPLATE_WORK_ORDER_KEY = "template_work_order"
        const val TEMPLATE_QUOTE_KEY = "template_quote"
        const val TEMPLATE_WORK_ORDER_OPEN_KEY = "template_work_order_open"
        const val TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY = "template_work_order_in_progress"
        const val TEMPLATE_WORK_ORDER_COMPLETED_KEY = "template_work_order_completed"
        const val TEMPLATE_WORK_ORDER_CANCELED_KEY = "template_work_order_canceled"
        const val TEMPLATE_REVIEW_REQUEST_KEY = "template_review_request"
        const val TEMPLATE_PICKUP_REMINDER_KEY = "template_pickup_reminder"
        const val PRINT_BLUETOOTH_ADDRESS_KEY = "print_bluetooth_address"
        const val PRINT_WORK_ORDER_AUTO_KEY = "print_work_order_auto"
        const val PRINT_WORK_ORDER_COPIES_KEY = "print_work_order_copies"
        const val PRINT_WORK_ORDER_HEADER_KEY = "print_work_order_header"
        const val PRINT_WORK_ORDER_FOOTER_KEY = "print_work_order_footer"
        const val PRINT_WORK_ORDER_FONT_KEY = "print_work_order_font"
        const val PRINT_WORK_ORDER_TEXT_SIZE_KEY = "print_work_order_text_size"
        const val PRINT_WORK_ORDER_HEADER_BOLD_KEY = "print_work_order_header_bold"
        const val PRINT_WORK_ORDER_HEADER_ALIGN_KEY = "print_work_order_header_align"

        fun contactRawIdKey(customerId: Long): String = "contact_raw_id_customer_$customerId"
    }
}
