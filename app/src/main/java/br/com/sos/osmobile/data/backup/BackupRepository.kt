package br.com.sos.osmobile.data.backup

import android.content.Context
import android.util.Base64
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import br.com.sos.osmobile.data.local.entity.QuoteItemEntity
import br.com.sos.osmobile.data.local.entity.SaleEntity
import br.com.sos.osmobile.data.local.entity.SaleItemEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductType
import br.com.sos.osmobile.data.local.entity.StockMovementEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderChecklistItemEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderWarrantyEntity
import br.com.sos.osmobile.data.repository.SettingsRepository
import androidx.room.withTransaction
import java.io.File
import org.json.JSONObject

data class BackupImportResult(
    val customers: Int,
    val services: Int,
    val quotes: Int,
    val workOrders: Int,
)

data class SettingsBackupImportResult(
    val settings: Int,
)

data class CsvImportResult(
    val imported: Int,
    val ignored: Int,
)

data class OperationalResetResult(
    val customers: Int,
    val services: Int,
    val quotes: Int,
    val workOrders: Int,
)

class BackupRepository(
    private val database: AppDatabase,
    private val context: Context,
) {
    suspend fun exportSettingsJson(): String {
        val settings = database.settingsDao().listAll()

        return buildString {
            appendLine("{")
            appendLine("\"tipo\":\"configuracoes\",")
            appendLine("\"configuracoes\":${settings.joinToString(prefix = "[", postfix = "]") { """{"chave":${str(it.chave)},"valor":${str(it.valor)},"updatedAt":${it.updatedAt}}""" }}")
            appendLine("}")
        }
    }

    suspend fun exportJson(): String {
        val customers = database.customerDao().listAll()
        val services = database.serviceProductDao().listAll()
        val quotes = database.quoteDao().listAll()
        val quoteItems = database.quoteDao().listAllItems()
        val workOrders = database.workOrderDao().listAll()
        val workOrderItems = database.workOrderDao().listAllItems()
        val photos = database.workOrderPhotoDao().listAll()
        val signatures = database.workOrderSignatureDao().listAll()
        val checklist = database.workOrderChecklistDao().listAll()
        val warranties = database.workOrderWarrantyDao().listAll()
        val payments = database.workOrderPaymentDao().listAll()
        val stockMovements = database.stockMovementDao().listAll()
        val sales = database.saleDao().listAll()
        val saleItems = database.saleDao().listAllItems()

        return buildString {
            appendLine("{")
            appendLine("\"clientes\":${customers.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"nome":${str(it.nome)},"cpfCnpj":${str(it.cpfCnpj)},"telefone":${str(it.telefone)},"email":${str(it.email)},"endereco":${str(it.endereco)},"observacoes":${str(it.observacoes)},"ativo":${it.ativo},"createdAt":${it.createdAt},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"servicos_produtos\":${services.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"codigo":${str(it.codigo)},"nome":${str(it.nome)},"tipo":${str(it.tipo)},"categoria":${str(it.categoria)},"descricao":${str(it.descricao)},"unitPrice":${it.unitPrice},"minimumStock":${it.minimumStock},"ncm":${str(it.ncm)},"cfop":${str(it.cfop)},"unidade":${str(it.unidade)},"cstCsosn":${str(it.cstCsosn)},"ativo":${it.ativo},"createdAt":${it.createdAt},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"orcamentos\":${quotes.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"numero":${str(it.numero)},"customerId":${it.customerId},"createdAt":${it.createdAt},"validUntil":${it.validUntil ?: "null"},"status":${str(it.status)},"observacoes":${str(it.observacoes)},"totalValue":${it.totalValue},"discountValue":${it.discountValue},"minimumDepositValue":${it.minimumDepositValue},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"itens_orcamento\":${quoteItems.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"quoteId":${it.quoteId},"serviceProductId":${it.serviceProductId},"quantidade":${it.quantidade},"practicedUnitPrice":${it.practicedUnitPrice},"subtotal":${it.subtotal}}""" }},")
            appendLine("\"ordens_servico\":${workOrders.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"numero":${str(it.numero)},"customerId":${it.customerId},"openedAt":${it.openedAt},"expectedConclusionAt":${it.expectedConclusionAt ?: "null"},"status":${str(it.status)},"observacoes":${str(it.observacoes)},"totalValue":${it.totalValue},"discountValue":${it.discountValue},"deliveryType":${str(it.deliveryType)},"deliveryStatus":${str(it.deliveryStatus)},"deliveryAddress":${str(it.deliveryAddress)},"deliveryFee":${it.deliveryFee},"trackingCode":${str(it.trackingCode)},"deliveryNotes":${str(it.deliveryNotes)},"concludedAt":${it.concludedAt ?: "null"},"fiscalStatus":${str(it.fiscalStatus)},"fiscalKey":${str(it.fiscalKey)},"fiscalProtocol":${str(it.fiscalProtocol)},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"itens_os\":${workOrderItems.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"workOrderId":${it.workOrderId},"serviceProductId":${it.serviceProductId},"quantidade":${it.quantidade},"practicedUnitPrice":${it.practicedUnitPrice},"subtotal":${it.subtotal}}""" }},")
            appendLine("\"fotos_os\":${photos.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"workOrderId":${it.workOrderId},"fileName":${str(it.fileName)},"relativePath":${str(it.relativePath)},"mimeType":${str(it.mimeType)},"createdAt":${it.createdAt},"conteudoBase64":${str(fileBase64(it.relativePath))}}""" }},")
            appendLine("\"assinaturas_os\":${signatures.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"workOrderId":${it.workOrderId},"fileName":${str(it.fileName)},"relativePath":${str(it.relativePath)},"signerName":${str(it.signerName)},"createdAt":${it.createdAt},"conteudoBase64":${str(fileBase64(it.relativePath))}}""" }},")
            appendLine("\"checklist_os\":${checklist.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"workOrderId":${it.workOrderId},"descricao":${str(it.descricao)},"concluido":${it.concluido},"createdAt":${it.createdAt},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"garantias_os\":${warranties.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"workOrderId":${it.workOrderId},"warrantyDays":${it.warrantyDays},"termos":${str(it.termos)},"createdAt":${it.createdAt},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"pagamentos_os\":${payments.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"workOrderId":${it.workOrderId},"valor":${it.valor},"forma":${str(it.forma)},"observacao":${str(it.observacao)},"paidAt":${it.paidAt}}""" }},")
            appendLine("\"movimentacoes_estoque\":${stockMovements.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"serviceProductId":${it.serviceProductId},"tipo":${str(it.tipo)},"quantidade":${it.quantidade},"motivo":${str(it.motivo)},"workOrderId":${it.workOrderId ?: "null"},"createdAt":${it.createdAt}}""" }},")
            appendLine("\"vendas\":${sales.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"numero":${str(it.numero)},"customerId":${it.customerId},"totalValue":${it.totalValue},"paidValue":${it.paidValue},"paymentMethod":${str(it.paymentMethod)},"fiscalStatus":${str(it.fiscalStatus)},"createdAt":${it.createdAt},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"itens_venda\":${saleItems.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"saleId":${it.saleId},"serviceProductId":${it.serviceProductId},"quantidade":${it.quantidade},"unitPrice":${it.unitPrice},"subtotal":${it.subtotal}}""" }}")
            appendLine("}")
        }
    }

    suspend fun importJson(json: String): BackupImportResult {
        val root = JSONObject(json)
        val now = Clock.nowMillis()
        val customers = root.array("clientes").mapObjects { item ->
            CustomerEntity(
                id = item.requiredId(),
                nome = item.getString("nome"),
                cpfCnpj = item.nullableString("cpfCnpj"),
                telefone = item.getString("telefone"),
                email = item.nullableString("email"),
                endereco = item.nullableString("endereco"),
                observacoes = item.nullableString("observacoes"),
                ativo = item.optBoolean("ativo", true),
                createdAt = item.optLong("createdAt", now),
                updatedAt = item.optLong("updatedAt", now),
            )
        }
        val services = root.array("servicos_produtos").mapObjects { item ->
            ServiceProductEntity(
                id = item.requiredId(),
                codigo = item.getString("codigo"),
                nome = item.getString("nome"),
                tipo = item.optString("tipo", "Servico"),
                categoria = item.nullableString("categoria"),
                descricao = item.nullableString("descricao"),
                unitPrice = item.optDouble("unitPrice", item.optDouble("valor", 0.0)),
                minimumStock = item.optDouble("minimumStock", item.optDouble("estoqueMinimo", 0.0)),
                ncm = item.nullableString("ncm"),
                cfop = item.nullableString("cfop"),
                unidade = item.nullableString("unidade"),
                cstCsosn = item.nullableString("cstCsosn"),
                ativo = item.optBoolean("ativo", true),
                createdAt = item.optLong("createdAt", now),
                updatedAt = item.optLong("updatedAt", now),
            )
        }
        val quotes = root.array("orcamentos").mapObjects { item ->
            QuoteEntity(
                id = item.requiredId(),
                numero = item.getString("numero"),
                customerId = item.optLong("customerId", item.optLong("cliente")),
                createdAt = item.optLong("createdAt", now),
                validUntil = item.nullableLong("validUntil"),
                status = item.getString("status"),
                observacoes = item.nullableString("observacoes"),
                totalValue = item.optDouble("totalValue", item.optDouble("total", 0.0)),
                discountValue = item.optDouble("discountValue", 0.0),
                minimumDepositValue = item.optDouble("minimumDepositValue", 0.0),
                updatedAt = item.optLong("updatedAt", now),
            )
        }
        val quoteItems = root.array("itens_orcamento").mapObjects { item ->
            QuoteItemEntity(
                id = item.optLong("id", 0),
                quoteId = item.optLong("quoteId", item.optLong("orcamento")),
                serviceProductId = item.optLong("serviceProductId", item.optLong("servico")),
                quantidade = item.optDouble("quantidade", item.optDouble("qtd", 0.0)),
                practicedUnitPrice = item.optDouble("practicedUnitPrice", item.optDouble("valor", 0.0)),
                subtotal = item.optDouble("subtotal", 0.0),
            )
        }
        val workOrders = root.array("ordens_servico").mapObjects { item ->
            WorkOrderEntity(
                id = item.requiredId(),
                numero = item.getString("numero"),
                customerId = item.optLong("customerId", item.optLong("cliente")),
                openedAt = item.optLong("openedAt", now),
                expectedConclusionAt = item.nullableLong("expectedConclusionAt"),
                status = item.getString("status"),
                observacoes = item.nullableString("observacoes"),
                totalValue = item.optDouble("totalValue", item.optDouble("total", 0.0)),
                discountValue = item.optDouble("discountValue", 0.0),
                deliveryType = item.optString("deliveryType", "Retirada no local"),
                deliveryStatus = item.optString("deliveryStatus", "Aguardando retirada"),
                deliveryAddress = item.nullableString("deliveryAddress"),
                deliveryFee = item.optDouble("deliveryFee", 0.0),
                trackingCode = item.nullableString("trackingCode"),
                deliveryNotes = item.nullableString("deliveryNotes"),
                concludedAt = item.nullableLong("concludedAt"),
                fiscalStatus = item.optString("fiscalStatus", "Nao emitida"),
                fiscalKey = item.nullableString("fiscalKey"),
                fiscalProtocol = item.nullableString("fiscalProtocol"),
                updatedAt = item.optLong("updatedAt", now),
            )
        }
        val workOrderItems = root.array("itens_os").mapObjects { item ->
            WorkOrderItemEntity(
                id = item.optLong("id", 0),
                workOrderId = item.optLong("workOrderId", item.optLong("os")),
                serviceProductId = item.optLong("serviceProductId", item.optLong("servico")),
                quantidade = item.optDouble("quantidade", item.optDouble("qtd", 0.0)),
                practicedUnitPrice = item.optDouble("practicedUnitPrice", item.optDouble("valor", 0.0)),
                subtotal = item.optDouble("subtotal", 0.0),
            )
        }
        val photos = root.optionalArray("fotos_os").mapObjects { item ->
            WorkOrderPhotoEntity(
                id = item.optLong("id", 0),
                workOrderId = item.optLong("workOrderId", item.optLong("os")),
                fileName = item.getString("fileName"),
                relativePath = item.getString("relativePath"),
                mimeType = item.optString("mimeType", "image/jpeg"),
                createdAt = item.optLong("createdAt", now),
            )
        }
        val signatures = root.optionalArray("assinaturas_os").mapObjects { item ->
            WorkOrderSignatureEntity(
                id = item.optLong("id", 0),
                workOrderId = item.optLong("workOrderId", item.optLong("os")),
                fileName = item.getString("fileName"),
                relativePath = item.getString("relativePath"),
                signerName = item.optString("signerName", "Cliente"),
                createdAt = item.optLong("createdAt", now),
            )
        }
        val checklist = root.optionalArray("checklist_os").mapObjects { item ->
            WorkOrderChecklistItemEntity(
                id = item.optLong("id", 0),
                workOrderId = item.optLong("workOrderId", item.optLong("os")),
                descricao = item.getString("descricao"),
                concluido = item.optBoolean("concluido", false),
                createdAt = item.optLong("createdAt", now),
                updatedAt = item.optLong("updatedAt", now),
            )
        }
        val warranties = root.optionalArray("garantias_os").mapObjects { item ->
            WorkOrderWarrantyEntity(
                id = item.optLong("id", 0),
                workOrderId = item.optLong("workOrderId", item.optLong("os")),
                warrantyDays = item.optInt("warrantyDays", item.optInt("prazoDias", 0)),
                termos = item.optString("termos", "Garantia conforme politica da empresa."),
                createdAt = item.optLong("createdAt", now),
                updatedAt = item.optLong("updatedAt", now),
            )
        }
        val payments = root.optionalArray("pagamentos_os").mapObjects { item ->
            WorkOrderPaymentEntity(
                id = item.optLong("id", 0),
                workOrderId = item.optLong("workOrderId", item.optLong("os")),
                valor = item.optDouble("valor", 0.0),
                forma = item.optString("forma", "Nao informado"),
                observacao = item.nullableString("observacao"),
                paidAt = item.optLong("paidAt", now),
            )
        }
        val stockMovements = root.optionalArray("movimentacoes_estoque").mapObjects { item ->
            StockMovementEntity(
                id = item.optLong("id", 0),
                serviceProductId = item.optLong("serviceProductId", item.optLong("servico")),
                tipo = item.getString("tipo"),
                quantidade = item.optDouble("quantidade", 0.0),
                motivo = item.nullableString("motivo"),
                workOrderId = item.nullableLong("workOrderId"),
                createdAt = item.optLong("createdAt", now),
            )
        }
        val sales = root.optionalArray("vendas").mapObjects { item ->
            SaleEntity(
                id = item.optLong("id", 0),
                numero = item.getString("numero"),
                customerId = item.optLong("customerId", item.optLong("cliente")),
                totalValue = item.optDouble("totalValue", 0.0),
                paidValue = item.optDouble("paidValue", 0.0),
                paymentMethod = item.optString("paymentMethod", "Nao informado"),
                fiscalStatus = item.optString("fiscalStatus", "Nao emitida"),
                createdAt = item.optLong("createdAt", now),
                updatedAt = item.optLong("updatedAt", now),
            )
        }
        val saleItems = root.optionalArray("itens_venda").mapObjects { item ->
            SaleItemEntity(
                id = item.optLong("id", 0),
                saleId = item.optLong("saleId", item.optLong("venda")),
                serviceProductId = item.optLong("serviceProductId", item.optLong("servico")),
                quantidade = item.optDouble("quantidade", item.optDouble("qtd", 0.0)),
                unitPrice = item.optDouble("unitPrice", item.optDouble("valor", 0.0)),
                subtotal = item.optDouble("subtotal", 0.0),
            )
        }

        database.withTransaction {
            database.saleDao().deleteAllItems()
            database.saleDao().deleteAll()
            database.stockMovementDao().deleteAll()
            database.workOrderPhotoDao().deleteAll()
            database.workOrderSignatureDao().deleteAll()
            database.workOrderChecklistDao().deleteAll()
            database.workOrderWarrantyDao().deleteAll()
            database.workOrderPaymentDao().deleteAll()
            database.quoteDao().deleteAllItems()
            database.workOrderDao().deleteAllItems()
            database.quoteDao().deleteAll()
            database.workOrderDao().deleteAll()
            database.serviceProductDao().deleteAll()
            database.customerDao().deleteAll()
            database.customerDao().upsertBackup(customers)
            database.serviceProductDao().upsertBackup(services)
            database.quoteDao().upsertBackup(quotes)
            database.workOrderDao().upsertBackup(workOrders)
            database.quoteDao().upsertBackupItems(quoteItems)
            database.workOrderDao().upsertBackupItems(workOrderItems)
            database.workOrderPhotoDao().upsertBackup(photos)
            database.workOrderSignatureDao().upsertBackup(signatures)
            database.workOrderChecklistDao().upsertBackup(checklist)
            database.workOrderWarrantyDao().upsertBackup(warranties)
            database.workOrderPaymentDao().upsertBackup(payments)
            database.stockMovementDao().upsertBackup(stockMovements)
            database.saleDao().upsertBackup(sales)
            database.saleDao().upsertBackupItems(saleItems)
        }
        deleteFilesDir("work_order_photos")
        deleteFilesDir("work_order_signatures")
        root.optionalArray("fotos_os").forEachObject { item ->
            writeBase64File(item.getString("relativePath"), item.nullableString("conteudoBase64"))
        }
        root.optionalArray("assinaturas_os").forEachObject { item ->
            writeBase64File(item.getString("relativePath"), item.nullableString("conteudoBase64"))
        }

        return BackupImportResult(
            customers = customers.size,
            services = services.size,
            quotes = quotes.size,
            workOrders = workOrders.size,
        )
    }

    suspend fun importSettingsJson(json: String): SettingsBackupImportResult {
        val root = JSONObject(json)
        val settings = root.array("configuracoes").mapObjects { item ->
            AppSettingEntity(
                chave = item.getString("chave"),
                valor = item.getString("valor"),
                updatedAt = item.optLong("updatedAt", Clock.nowMillis()),
            )
        }
        require(settings.isNotEmpty()) { "Backup de configuracoes vazio." }

        database.settingsDao().upsertAll(settings)

        return SettingsBackupImportResult(settings = settings.size)
    }

    suspend fun importCustomersCsv(csv: String): CsvImportResult {
        val rows = parseCsv(csv)
        val now = Clock.nowMillis()
        val customers = rows.mapNotNull { row ->
            val nome = row.value("nome", "cliente")
            val telefone = row.value("telefone", "celular", "whatsapp")
            if (nome.isBlank() || telefone.isBlank()) return@mapNotNull null
            CustomerEntity(
                nome = nome,
                cpfCnpj = row.value("cpf_cnpj", "cpf", "cnpj").blankToNull(),
                telefone = telefone,
                email = row.value("email", "e-mail").blankToNull(),
                endereco = row.value("endereco", "endereço").blankToNull(),
                observacoes = row.value("observacoes", "observações", "obs").blankToNull(),
                createdAt = now,
                updatedAt = now,
            )
        }
        require(customers.isNotEmpty()) { "CSV sem clientes validos." }
        database.customerDao().upsertBackup(customers)
        return CsvImportResult(imported = customers.size, ignored = rows.size - customers.size)
    }

    suspend fun importServiceProductsCsv(csv: String): CsvImportResult {
        val rows = parseCsv(csv)
        val now = Clock.nowMillis()
        val items = rows.mapNotNull { row ->
            val codigo = row.value("codigo", "código", "sku")
            val nome = row.value("nome", "descricao", "descrição")
            if (codigo.isBlank() || nome.isBlank()) return@mapNotNull null
            ServiceProductEntity(
                codigo = codigo,
                nome = nome,
                tipo = normalizedServiceType(row.value("tipo")),
                categoria = row.value("categoria").blankToNull(),
                descricao = row.value("descricao", "descrição").blankToNull(),
                unitPrice = row.value("valor", "preco", "preço", "preco_unitario", "preço_unitario").parseMoney(),
                minimumStock = row.value("estoque_minimo", "estoque mínimo", "minimo", "mínimo").parseMoney(),
                ncm = row.value("ncm").blankToNull(),
                cfop = row.value("cfop").blankToNull(),
                unidade = row.value("unidade", "un").blankToNull(),
                cstCsosn = row.value("cst_csosn", "cst", "csosn").blankToNull(),
                createdAt = now,
                updatedAt = now,
            )
        }
        require(items.isNotEmpty()) { "CSV sem servicos/produtos validos." }
        database.serviceProductDao().upsertBackup(items)
        return CsvImportResult(imported = items.size, ignored = rows.size - items.size)
    }

    suspend fun importSettingsCsv(csv: String): CsvImportResult {
        val rows = parseCsv(csv)
        val now = Clock.nowMillis()
        val settings = rows.mapNotNull { row ->
            val key = row.value("chave", "key")
            if (key.isBlank()) return@mapNotNull null
            AppSettingEntity(
                chave = key,
                valor = row.value("valor", "value"),
                updatedAt = now,
            )
        }
        require(settings.isNotEmpty()) { "CSV sem configuracoes validas." }
        database.settingsDao().upsertAll(settings)
        return CsvImportResult(imported = settings.size, ignored = rows.size - settings.size)
    }

    suspend fun importMessageTemplatesCsv(csv: String): CsvImportResult {
        val rows = parseCsv(csv)
        val now = Clock.nowMillis()
        val templateKeys = messageTemplateKeys()
        val settings = rows.mapNotNull { row ->
            val code = row.value("codigo", "código", "chave").lowercase()
            val key = templateKeys[code] ?: return@mapNotNull null
            AppSettingEntity(
                chave = key,
                valor = row.value("template", "mensagem", "valor").decodeCsvEscapedLines(),
                updatedAt = now,
            )
        }
        require(settings.isNotEmpty()) { "CSV sem templates validos." }
        database.settingsDao().upsertAll(settings)
        return CsvImportResult(imported = settings.size, ignored = rows.size - settings.size)
    }

    suspend fun resetOperationalData(): OperationalResetResult {
        val customersCount = database.customerDao().listAll().size
        val servicesCount = database.serviceProductDao().listAll().size
        val quotesCount = database.quoteDao().listAll().size
        val workOrdersCount = database.workOrderDao().listAll().size

        database.withTransaction {
            database.saleDao().deleteAllItems()
            database.saleDao().deleteAll()
            database.stockMovementDao().deleteAll()
            database.workOrderPhotoDao().deleteAll()
            database.workOrderSignatureDao().deleteAll()
            database.workOrderChecklistDao().deleteAll()
            database.workOrderWarrantyDao().deleteAll()
            database.workOrderPaymentDao().deleteAll()
            database.quoteDao().deleteAllItems()
            database.workOrderDao().deleteAllItems()
            database.quoteDao().deleteAll()
            database.workOrderDao().deleteAll()
            database.serviceProductDao().deleteAll()
            database.customerDao().deleteAll()
            database.auditLogDao().deleteAll()
            database.settingsDao().deleteContactLinks()
        }
        deleteFilesDir("work_order_photos")
        deleteFilesDir("work_order_signatures")

        return OperationalResetResult(
            customers = customersCount,
            services = servicesCount,
            quotes = quotesCount,
            workOrders = workOrdersCount,
        )
    }

    private fun esc(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun str(value: String?): String =
        value?.let { "\"${esc(it)}\"" } ?: "null"

    private fun parseCsv(csv: String): List<Map<String, String>> {
        val records = mutableListOf<List<String>>()
        val currentRecord = mutableListOf<String>()
        val currentField = StringBuilder()
        var quoted = false
        var index = 0

        while (index < csv.length) {
            val char = csv[index]
            when {
                char == '"' && quoted && index + 1 < csv.length && csv[index + 1] == '"' -> {
                    currentField.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    currentRecord += currentField.toString()
                    currentField.clear()
                }
                (char == '\n' || char == '\r') && !quoted -> {
                    if (char == '\r' && index + 1 < csv.length && csv[index + 1] == '\n') index++
                    currentRecord += currentField.toString()
                    currentField.clear()
                    if (currentRecord.any { it.isNotBlank() }) records += currentRecord.toList()
                    currentRecord.clear()
                }
                else -> currentField.append(char)
            }
            index++
        }
        currentRecord += currentField.toString()
        if (currentRecord.any { it.isNotBlank() }) records += currentRecord.toList()

        require(records.size >= 2) { "CSV precisa de cabecalho e ao menos uma linha." }
        val headers = records.first().map { it.trim().removePrefix("\uFEFF").lowercase() }
        return records.drop(1).map { record ->
            headers.mapIndexed { column, header -> header to record.getOrElse(column) { "" }.trim() }.toMap()
        }
    }

    private fun Map<String, String>.value(vararg keys: String): String =
        keys.firstNotNullOfOrNull { key -> this[key.lowercase()] }?.trim().orEmpty()

    private fun String.blankToNull(): String? = takeIf { it.isNotBlank() }

    private fun String.parseMoney(): Double {
        val clean = replace(Regex("[^0-9,.-]"), "")
        if (clean.isBlank()) return 0.0
        val normalized = if (clean.contains(",")) clean.replace(".", "").replace(",", ".") else clean
        return normalized.toDoubleOrNull() ?: 0.0
    }

    private fun String.decodeCsvEscapedLines(): String =
        replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\r", "\n")

    private fun normalizedServiceType(value: String): String =
        when (value.trim().lowercase()) {
            "produto", "product" -> ServiceProductType.PRODUCT
            "insumo", "supply" -> ServiceProductType.SUPPLY
            else -> ServiceProductType.SERVICE
        }

    private fun messageTemplateKeys(): Map<String, String> = mapOf(
        "orcamento" to SettingsRepository.TEMPLATE_QUOTE_KEY,
        "os_geral" to SettingsRepository.TEMPLATE_WORK_ORDER_KEY,
        "os_aberta" to SettingsRepository.TEMPLATE_WORK_ORDER_OPEN_KEY,
        "os_em_andamento" to SettingsRepository.TEMPLATE_WORK_ORDER_IN_PROGRESS_KEY,
        "os_concluida" to SettingsRepository.TEMPLATE_WORK_ORDER_COMPLETED_KEY,
        "os_cancelada" to SettingsRepository.TEMPLATE_WORK_ORDER_CANCELED_KEY,
        "solicitacao_avaliacao" to SettingsRepository.TEMPLATE_REVIEW_REQUEST_KEY,
        "aviso_retirada" to SettingsRepository.TEMPLATE_PICKUP_REMINDER_KEY,
        "pagamento_pendente" to SettingsRepository.TEMPLATE_PAYMENT_PENDING_KEY,
        "pagamento_confirmado" to SettingsRepository.TEMPLATE_PAYMENT_CONFIRMED_KEY,
        "solicitar_comprovante" to SettingsRepository.TEMPLATE_PAYMENT_PROOF_REQUEST_KEY,
        "pedido_enviado" to SettingsRepository.TEMPLATE_ORDER_SENT_KEY,
        "saiu_entrega" to SettingsRepository.TEMPLATE_OUT_FOR_DELIVERY_KEY,
        "entregue" to SettingsRepository.TEMPLATE_DELIVERED_KEY,
        "nao_entregue" to SettingsRepository.TEMPLATE_NOT_DELIVERED_KEY,
        "agradecimento" to SettingsRepository.TEMPLATE_THANK_YOU_KEY,
        "comunicado" to SettingsRepository.TEMPLATE_ANNOUNCEMENT_KEY,
        "boas_vindas" to SettingsRepository.TEMPLATE_WELCOME_KEY,
        "orcamento_expirado" to SettingsRepository.TEMPLATE_QUOTE_EXPIRED_KEY,
        "lembrete_orcamento" to SettingsRepository.TEMPLATE_QUOTE_REMINDER_KEY,
    )

    private fun JSONObject.array(name: String) = optJSONArray(name) ?: error("Backup sem a lista $name.")

    private fun JSONObject.optionalArray(name: String) = optJSONArray(name) ?: org.json.JSONArray()

    private fun JSONObject.requiredId(): Long =
        optLong("id", 0).takeIf { it > 0 } ?: error("Backup possui registro sem id valido.")

    private fun JSONObject.nullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null

    private fun JSONObject.nullableLong(name: String): Long? =
        if (has(name) && !isNull(name)) getLong(name) else null

    private inline fun <T> org.json.JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private inline fun org.json.JSONArray.forEachObject(action: (JSONObject) -> Unit) {
        repeat(length()) { index -> action(getJSONObject(index)) }
    }

    private fun fileBase64(relativePath: String): String? {
        val file = File(context.filesDir, relativePath)
        if (!file.exists()) return null
        return Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)
    }

    private fun writeBase64File(relativePath: String, base64: String?) {
        if (base64.isNullOrBlank()) return
        val file = File(context.filesDir, relativePath)
        file.parentFile?.mkdirs()
        file.writeBytes(Base64.decode(base64, Base64.DEFAULT))
    }

    private fun deleteFilesDir(relativePath: String) {
        File(context.filesDir, relativePath).deleteRecursively()
    }
}
