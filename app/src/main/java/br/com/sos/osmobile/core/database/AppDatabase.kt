package br.com.sos.osmobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.sos.osmobile.data.local.dao.AuditLogDao
import br.com.sos.osmobile.data.local.dao.CustomerDao
import br.com.sos.osmobile.data.local.dao.QuoteDao
import br.com.sos.osmobile.data.local.dao.ServiceProductDao
import br.com.sos.osmobile.data.local.dao.SettingsDao
import br.com.sos.osmobile.data.local.dao.WorkOrderDao
import br.com.sos.osmobile.data.local.dao.WorkOrderPhotoDao
import br.com.sos.osmobile.data.local.dao.WorkOrderSignatureDao
import br.com.sos.osmobile.data.local.entity.AppSettingEntity
import br.com.sos.osmobile.data.local.entity.AuditLogEntity
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.QuoteEntity
import br.com.sos.osmobile.data.local.entity.QuoteItemEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderItemEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity

@Database(
    entities = [
        CustomerEntity::class,
        ServiceProductEntity::class,
        WorkOrderEntity::class,
        WorkOrderItemEntity::class,
        QuoteEntity::class,
        QuoteItemEntity::class,
        AuditLogEntity::class,
        AppSettingEntity::class,
        WorkOrderPhotoEntity::class,
        WorkOrderSignatureEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customerDao(): CustomerDao
    abstract fun serviceProductDao(): ServiceProductDao
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun quoteDao(): QuoteDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun settingsDao(): SettingsDao
    abstract fun workOrderPhotoDao(): WorkOrderPhotoDao
    abstract fun workOrderSignatureDao(): WorkOrderSignatureDao
}
