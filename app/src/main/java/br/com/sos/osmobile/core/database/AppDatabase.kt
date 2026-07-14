package br.com.sos.osmobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.sos.osmobile.data.local.dao.AuditLogDao
import br.com.sos.osmobile.data.local.dao.CustomerDao
import br.com.sos.osmobile.data.local.dao.QuoteDao
import br.com.sos.osmobile.data.local.dao.SaleDao
import br.com.sos.osmobile.data.local.dao.ServiceProductDao
import br.com.sos.osmobile.data.local.dao.SettingsDao
import br.com.sos.osmobile.data.local.dao.StockMovementDao
import br.com.sos.osmobile.data.local.dao.WorkOrderDao
import br.com.sos.osmobile.data.local.dao.WorkOrderChecklistDao
import br.com.sos.osmobile.data.local.dao.WorkOrderPaymentDao
import br.com.sos.osmobile.data.local.dao.WorkOrderPhotoDao
import br.com.sos.osmobile.data.local.dao.WorkOrderSignatureDao
import br.com.sos.osmobile.data.local.dao.WorkOrderWarrantyDao
import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import br.com.sos.osmobile.data.local.entity.AuditLogEntity
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import br.com.sos.osmobile.data.local.entity.QuoteItemEntity
import br.com.sos.osmobile.data.local.entity.SaleEntity
import br.com.sos.osmobile.data.local.entity.SaleItemEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.StockMovementEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderChecklistItemEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderWarrantyEntity

@Database(
    entities = [
        CustomerEntity::class,
        ServiceProductEntity::class,
        StockMovementEntity::class,
        WorkOrderEntity::class,
        WorkOrderItemEntity::class,
        QuoteEntity::class,
        QuoteItemEntity::class,
        SaleEntity::class,
        SaleItemEntity::class,
        AuditLogEntity::class,
        AppSettingEntity::class,
        WorkOrderPhotoEntity::class,
        WorkOrderSignatureEntity::class,
        WorkOrderChecklistItemEntity::class,
        WorkOrderWarrantyEntity::class,
        WorkOrderPaymentEntity::class,
    ],
    version = 12,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun serviceProductDao(): ServiceProductDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun quoteDao(): QuoteDao
    abstract fun saleDao(): SaleDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun workOrderPhotoDao(): WorkOrderPhotoDao
    abstract fun workOrderSignatureDao(): WorkOrderSignatureDao
    abstract fun workOrderChecklistDao(): WorkOrderChecklistDao
    abstract fun workOrderWarrantyDao(): WorkOrderWarrantyDao
    abstract fun workOrderPaymentDao(): WorkOrderPaymentDao
}
