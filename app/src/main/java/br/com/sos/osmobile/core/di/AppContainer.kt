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
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.WorkOrderChecklistRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository
import br.com.sos.osmobile.data.repository.WorkOrderPhotoRepository
import br.com.sos.osmobile.data.repository.WorkOrderSignatureRepository

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

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "os_mobile.db",
    )
        .addMigrations(migration1To2, migration2To3, migration3To4)
        .build()

    val auditRepository = AuditRepository(database.auditLogDao())
    val contactsRepository = ContactsRepository(context.applicationContext)
    val customerRepository = CustomerRepository(database.customerDao(), auditRepository)
    val serviceProductRepository = ServiceProductRepository(database.serviceProductDao(), auditRepository)
    val workOrderRepository = WorkOrderRepository(database.workOrderDao(), auditRepository)
    val quoteRepository = QuoteRepository(database.quoteDao(), auditRepository)
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
    val backupRepository = BackupRepository(database)
}
