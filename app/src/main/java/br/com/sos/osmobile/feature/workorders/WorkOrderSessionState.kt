package br.com.sos.osmobile.feature.workorders

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.sos.osmobile.data.drive.DriveDesignImportCandidate
import br.com.sos.osmobile.data.local.entity.WorkOrderChecklistItemEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPaymentEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderPhotoEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderSignatureEntity
import br.com.sos.osmobile.data.local.entity.WorkOrderWarrantyEntity
import kotlinx.coroutines.CoroutineScope

class WorkOrderSessionState {
    var scope: CoroutineScope? = null
    var formState by mutableStateOf(WorkOrderFormState())
    var listMessage by mutableStateOf<String?>(null)
    var documentText by mutableStateOf<String?>(null)
    var messageText by mutableStateOf<String?>(null)
    var messagePhone by mutableStateOf("")
    var driveDebugReport by mutableStateOf("")
    var pendingDesignImportCandidates by mutableStateOf<List<DriveDesignImportCandidate>>(emptyList())
    var historyText by mutableStateOf<String?>(null)
    var photos by mutableStateOf<List<WorkOrderPhotoEntity>>(emptyList())
    var signature by mutableStateOf<WorkOrderSignatureEntity?>(null)
    var checklist by mutableStateOf<List<WorkOrderChecklistItemEntity>>(emptyList())
    var warranty by mutableStateOf<WorkOrderWarrantyEntity?>(null)
    var payments by mutableStateOf<List<WorkOrderPaymentEntity>>(emptyList())
}
