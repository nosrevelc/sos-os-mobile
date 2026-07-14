package br.com.sos.osmobile.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.data.backup.BackupRepository
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.ContactsRepository
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.QuoteConversionRepository
import br.com.sos.osmobile.data.repository.QuoteRepository
import br.com.sos.osmobile.data.repository.SaleRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.StockRepository
import br.com.sos.osmobile.data.repository.WorkOrderChecklistRepository
import br.com.sos.osmobile.data.repository.WorkOrderPaymentRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.data.repository.WorkOrderPhotoRepository
import br.com.sos.osmobile.data.repository.WorkOrderSignatureRepository
import br.com.sos.osmobile.data.repository.WorkOrderWarrantyRepository

class AppContainer(context: Context) {
    private val migration1To2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS fotos_os (
                    id_foto_os INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    id_os INTEGER NOT NULL,
                    nome_arquivo TEXT NOT NULL,
                    caminho_relativo TEXT NOT NULL,
                    mime_type TEXT NOT NULL,
                    data_criacao INTEGER NOT NULL,
                    FOREIGN KEY(id_os) REFERENCES ordens_servico(id_os) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_fotos_os_id_os ON fotos_os(id_os)")
        }
    }

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS assinaturas_os (
                    id_assinatura_os INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    id_os INTEGER NOT NULL,
                    nome_arquivo TEXT NOT NULL,
                    caminho_relativo TEXT NOT NULL,
                    nome_assinante TEXT NOT NULL,
                    data_criacao INTEGER NOT NULL,
                    FOREIGN KEY(id_os) REFERENCES ordens_servico(id_os) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_assinaturas_os_id_os ON assinaturas_os(id_os)")
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS checklist_os (
                    id_checklist_os INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    id_os INTEGER NOT NULL,
                    descricao TEXT NOT NULL,
                    concluido INTEGER NOT NULL,
                    data_criacao INTEGER NOT NULL,
                    data_atualizacao INTEGER NOT NULL,
                    FOREIGN KEY(id_os) REFERENCES ordens_servico(id_os) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_checklist_os_id_os ON checklist_os(id_os)")
        }
    }

    private val migration4To5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS garantias_os (
                    id_garantia_os INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    id_os INTEGER NOT NULL,
                    prazo_dias INTEGER NOT NULL,
                    termos TEXT NOT NULL,
                    data_criacao INTEGER NOT NULL,
                    data_atualizacao INTEGER NOT NULL,
                    FOREIGN KEY(id_os) REFERENCES ordens_servico(id_os) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_garantias_os_id_os ON garantias_os(id_os)")
        }
    }

    private val migration5To6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS pagamentos_os (
                    id_pagamento_os INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    id_os INTEGER NOT NULL,
                    valor REAL NOT NULL,
                    forma TEXT NOT NULL,
                    observacao TEXT,
                    data_pagamento INTEGER NOT NULL,
                    FOREIGN KEY(id_os) REFERENCES ordens_servico(id_os) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_pagamentos_os_id_os ON pagamentos_os(id_os)")
        }
    }

    private val migration6To7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE servicos_produtos ADD COLUMN tipo TEXT NOT NULL DEFAULT 'Servico'")
        }
    }

    private val migration7To8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE servicos_produtos ADD COLUMN estoque_minimo REAL NOT NULL DEFAULT 0.0")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS movimentacoes_estoque (
                    id_movimentacao_estoque INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    id_servico_produto INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    quantidade REAL NOT NULL,
                    motivo TEXT,
                    id_os INTEGER,
                    data_criacao INTEGER NOT NULL,
                    FOREIGN KEY(id_servico_produto) REFERENCES servicos_produtos(id_servico_produto) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movimentacoes_estoque_id_servico_produto ON movimentacoes_estoque(id_servico_produto)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_movimentacoes_estoque_data_criacao ON movimentacoes_estoque(data_criacao)")
        }
    }

    private val migration8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE servicos_produtos ADD COLUMN ncm TEXT")
            db.execSQL("ALTER TABLE servicos_produtos ADD COLUMN cfop TEXT")
            db.execSQL("ALTER TABLE servicos_produtos ADD COLUMN unidade TEXT")
            db.execSQL("ALTER TABLE servicos_produtos ADD COLUMN cst_csosn TEXT")
            db.execSQL("ALTER TABLE ordens_servico ADD COLUMN status_fiscal TEXT NOT NULL DEFAULT 'Nao emitida'")
            db.execSQL("ALTER TABLE ordens_servico ADD COLUMN chave_fiscal TEXT")
            db.execSQL("ALTER TABLE ordens_servico ADD COLUMN protocolo_fiscal TEXT")
        }
    }

    private val migration9To10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS vendas (
                    id_venda INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    numero TEXT NOT NULL,
                    id_cliente INTEGER NOT NULL,
                    valor_total REAL NOT NULL,
                    valor_pago REAL NOT NULL,
                    forma_pagamento TEXT NOT NULL,
                    status_fiscal TEXT NOT NULL,
                    data_criacao INTEGER NOT NULL,
                    data_atualizacao INTEGER NOT NULL,
                    FOREIGN KEY(id_cliente) REFERENCES clientes(id_cliente) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_vendas_numero ON vendas(numero)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_vendas_id_cliente ON vendas(id_cliente)")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS itens_venda (
                    id_item_venda INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    id_venda INTEGER NOT NULL,
                    id_servico_produto INTEGER NOT NULL,
                    quantidade REAL NOT NULL,
                    preco_unitario REAL NOT NULL,
                    subtotal REAL NOT NULL,
                    FOREIGN KEY(id_venda) REFERENCES vendas(id_venda) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(id_servico_produto) REFERENCES servicos_produtos(id_servico_produto) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_itens_venda_id_venda ON itens_venda(id_venda)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_itens_venda_id_servico_produto ON itens_venda(id_servico_produto)")
        }
    }

    private val migration10To11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE orcamentos ADD COLUMN valor_desconto REAL NOT NULL DEFAULT 0.0")
            db.execSQL("ALTER TABLE ordens_servico ADD COLUMN valor_desconto REAL NOT NULL DEFAULT 0.0")
        }
    }

    private val migration11To12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE orcamentos ADD COLUMN valor_sinal_minimo REAL NOT NULL DEFAULT 0.0")
        }
    }

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "os_mobile.db",
    )
        .addMigrations(
            migration1To2,
            migration2To3,
            migration3To4,
            migration4To5,
            migration5To6,
            migration6To7,
            migration7To8,
            migration8To9,
            migration9To10,
            migration10To11,
            migration11To12,
        )
        .build()

    val auditRepository = AuditRepository(database.auditLogDao())
    val contactsRepository = ContactsRepository(context.applicationContext)
    val customerRepository = CustomerRepository(database.customerDao(), auditRepository)
    val serviceProductRepository = ServiceProductRepository(database.serviceProductDao(), auditRepository)
    val stockRepository = StockRepository(database.stockMovementDao(), auditRepository)
    val workOrderRepository = WorkOrderRepository(database.workOrderDao(), auditRepository)
    val quoteRepository = QuoteRepository(database.quoteDao(), auditRepository)
    val saleRepository = SaleRepository(database.saleDao(), auditRepository)
    val quoteConversionRepository = QuoteConversionRepository(database, auditRepository)
    val settingsRepository = SettingsRepository(database.settingsDao(), auditRepository)
    val workOrderPhotoRepository = WorkOrderPhotoRepository(
        context.applicationContext,
        database.workOrderPhotoDao(),
        auditRepository,
    )
    val workOrderSignatureRepository = WorkOrderSignatureRepository(
        context.applicationContext,
        database.workOrderSignatureDao(),
        auditRepository,
    )
    val workOrderChecklistRepository = WorkOrderChecklistRepository(
        database.workOrderChecklistDao(),
        auditRepository,
    )
    val workOrderWarrantyRepository = WorkOrderWarrantyRepository(
        database.workOrderWarrantyDao(),
        auditRepository,
    )
    val workOrderPaymentRepository = WorkOrderPaymentRepository(
        database.workOrderPaymentDao(),
        auditRepository,
    )
    val backupRepository = BackupRepository(database, context.applicationContext)
}
