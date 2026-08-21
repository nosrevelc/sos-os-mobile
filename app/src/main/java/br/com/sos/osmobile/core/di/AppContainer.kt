package br.com.sos.osmobile.core.di

import android.content.Context
import androidx.room.Room
import br.com.sos.osmobile.core.database.ALL_MIGRATIONS
import br.com.sos.osmobile.core.database.AppDatabase
import br.com.sos.osmobile.data.backup.BackupRepository
import br.com.sos.osmobile.data.drive.DriveSyncRepository
import br.com.sos.osmobile.data.repository.AuditRepository
import br.com.sos.osmobile.data.repository.AppointmentRepository
import br.com.sos.osmobile.data.repository.CalendarRepository
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

    val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "os_mobile.db",
    )
        .addMigrations(*ALL_MIGRATIONS)
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
    val calendarRepository = CalendarRepository(context.applicationContext, settingsRepository)
    val appointmentRepository = AppointmentRepository(
        database.appointmentDao(),
        workOrderRepository,
        calendarRepository,
        auditRepository,
    )
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
    val driveSyncRepository = DriveSyncRepository(
        context.applicationContext,
        database.workOrderDao(),
        database.workOrderPhotoDao(),
        database.workOrderSignatureDao(),
        settingsRepository,
        auditRepository,
    )
    val backupRepository = BackupRepository(database, context.applicationContext, settingsRepository)

}
