package br.com.sos.osmobile.data.backup

import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.core.time.Clock
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import br.com.sos.osmobile.data.local.entity.QuoteItemEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity
import androidx.room.withTransaction
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

class BackupRepository(
    private val database: AppDatabase,
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

        return buildString {
            appendLine("{")
            appendLine("\"clientes\":${customers.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"nome":${str(it.nome)},"cpfCnpj":${str(it.cpfCnpj)},"telefone":${str(it.telefone)},"email":${str(it.email)},"endereco":${str(it.endereco)},"observacoes":${str(it.observacoes)},"ativo":${it.ativo},"createdAt":${it.createdAt},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"servicos_produtos\":${services.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"codigo":${str(it.codigo)},"nome":${str(it.nome)},"categoria":${str(it.categoria)},"descricao":${str(it.descricao)},"unitPrice":${it.unitPrice},"ativo":${it.ativo},"createdAt":${it.createdAt},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"orcamentos\":${quotes.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"numero":${str(it.numero)},"customerId":${it.customerId},"createdAt":${it.createdAt},"validUntil":${it.validUntil ?: "null"},"status":${str(it.status)},"observacoes":${str(it.observacoes)},"totalValue":${it.totalValue},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"itens_orcamento\":${quoteItems.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"quoteId":${it.quoteId},"serviceProductId":${it.serviceProductId},"quantidade":${it.quantidade},"practicedUnitPrice":${it.practicedUnitPrice},"subtotal":${it.subtotal}}""" }},")
            appendLine("\"ordens_servico\":${workOrders.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"numero":${str(it.numero)},"customerId":${it.customerId},"openedAt":${it.openedAt},"expectedConclusionAt":${it.expectedConclusionAt ?: "null"},"status":${str(it.status)},"observacoes":${str(it.observacoes)},"totalValue":${it.totalValue},"concludedAt":${it.concludedAt ?: "null"},"updatedAt":${it.updatedAt}}""" }},")
            appendLine("\"itens_os\":${workOrderItems.joinToString(prefix = "[", postfix = "]") { """{"id":${it.id},"workOrderId":${it.workOrderId},"serviceProductId":${it.serviceProductId},"quantidade":${it.quantidade},"practicedUnitPrice":${it.practicedUnitPrice},"subtotal":${it.subtotal}}""" }}")
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
                categoria = item.nullableString("categoria"),
                descricao = item.nullableString("descricao"),
                unitPrice = item.optDouble("unitPrice", item.optDouble("valor", 0.0)),
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
                concludedAt = item.nullableLong("concludedAt"),
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

        database.withTransaction {
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

    private fun esc(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun str(value: String?): String =
        value?.let { "\"${esc(it)}\"" } ?: "null"

    private fun JSONObject.array(name: String) = optJSONArray(name) ?: error("Backup sem a lista $name.")

    private fun JSONObject.requiredId(): Long =
        optLong("id", 0).takeIf { it > 0 } ?: error("Backup possui registro sem id valido.")

    private fun JSONObject.nullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null

    private fun JSONObject.nullableLong(name: String): Long? =
        if (has(name) && !isNull(name)) getLong(name) else null

    private inline fun <T> org.json.JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }
}
