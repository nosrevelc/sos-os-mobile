package br.com.sos.osmobile.core.di

import android.content.Context
import androidx.room.Room
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.data.backup.BackupRepository
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.CustomerRepository
import br.com.sos.osmobile.data.repository.QuoteConversionRepository
import br.com.sos.osmobile.data.repository.QuoteRepository
import br.com.sos.osmobile.data.repository.ServiceProductRepository
import br.com.sos.osmobile.data.repository.SettingsRepository
import br.com.sos.osmobile.data.repository.WorkOrderRepository

class AppContainer(context: Context) {
    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "os_mobile.db",
    ).build()

    val auditRepository = AuditRepository(database.auditLogDao())
    val customerRepository = CustomerRepository(database.customerDao(), auditRepository)
    val serviceProductRepository = ServiceProductRepository(database.serviceProductDao(), auditRepository)
    val workOrderRepository = WorkOrderRepository(database.workOrderDao(), auditRepository)
    val quoteRepository = QuoteRepository(database.quoteDao(), auditRepository)
    val quoteConversionRepository = QuoteConversionRepository(database, auditRepository)
    val settingsRepository = SettingsRepository(database.settingsDao(), auditRepository)
    val backupRepository = BackupRepository(database)
}
