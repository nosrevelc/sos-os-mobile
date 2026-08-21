package br.com.sos.osmobile.feature.workorders

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pix
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.text.KeyboardOptions
import br.com.sos.osmobile.data.message.MessageTemplateRenderer
import br.com.sos.osmobile.data.message.PixPayloadGenerator
import br.com.sos.osmobile.data.message.WorkOrderMessageRenderer
import br.com.sos.osmobile.data.model.DeliveryStatus
import br.com.sos.osmobile.data.model.DeliveryType
import br.com.sos.osmobile.data.local.AttachmentNames
import br.com.sos.osmobile.data.local.entity.CustomerEntity
import br.com.sos.osmobile.data.local.entity.ServiceProductEntity
import br.com.sos.osmobile.data.local.model.WorkOrderSummary
import br.com.sos.osmobile.data.model.WorkOrderStatus
import br.com.sos.osmobile.data.print.BluetoothThermalPrinter
import br.com.sos.osmobile.data.print.ThermalPrintContent
import br.com.sos.osmobile.data.print.ThermalTextBlock
import br.com.sos.osmobile.ui.components.CustomerSearchSelector
import br.com.sos.osmobile.ui.components.DriveSyncIndicator
import br.com.sos.osmobile.ui.components.DriveSyncStatusIcon
import br.com.sos.osmobile.ui.components.DriveSyncStatusText
import br.com.sos.osmobile.ui.components.MessageActionButtons
import br.com.sos.osmobile.ui.components.PixQrCode
import br.com.sos.osmobile.ui.components.ShareFileButton
import br.com.sos.osmobile.ui.components.SharePdfButton
import br.com.sos.osmobile.ui.components.ShareTextButton
import br.com.sos.osmobile.ui.components.ServiceProductSearchSelector
import br.com.sos.osmobile.ui.components.openEmail
import br.com.sos.osmobile.ui.components.openSms
import br.com.sos.osmobile.ui.components.openWhatsApp
import br.com.sos.osmobile.ui.components.sharePixQrJpeg
import br.com.sos.osmobile.ui.input.InputMasks
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import br.com.sos.osmobile.core.format.Formatters

@Composable
fun WorkOrderListScreen(
    viewModel: WorkOrderViewModel,
    onEdit: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var visibleCount by remember { mutableStateOf(20) }
    val visibleItems = uiState.workOrders.take(visibleCount)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Lista de OS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Mais recentes primeiro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (uiState.workOrders.isEmpty()) {
            item {
                Text("Nenhuma OS cadastrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(visibleItems, key = { it.id }) { workOrder ->
                WorkOrderListRow(workOrder = workOrder, onClick = { onEdit(workOrder.id) })
            }
            if (visibleCount < uiState.workOrders.size) {
                item {
                    OutlinedButton(
                        onClick = { visibleCount += 20 },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ver mais")
                    }
                }
            }
        }
    }
}

@Composable
fun WorkOrderPickupScreen(viewModel: WorkOrderViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val now = System.currentTimeMillis()
    val items = uiState.workOrders
        .filter { it.status == WorkOrderStatus.Completed.label }
        .sortedByDescending { pickupDays(it, now) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("OS prontas para retirada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (items.isEmpty()) {
            item { Text("Nenhuma OS concluida aguardando retirada.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(items, key = { it.id }) { workOrder ->
                val days = pickupDays(workOrder, now)
                PickupRow(
                    workOrder = workOrder,
                    days = days,
                    onSend = {
                        val text = renderPickupMessage(
                            template = uiState.pickupReminderTemplate,
                            workOrder = workOrder,
                            days = days,
                            companyName = uiState.companyName,
                        )
                        openWhatsApp(context, workOrder.customerPhone, text)
                    },
                )
            }
        }
    }
}

@Composable
internal fun PickupRow(
    workOrder: WorkOrderSummary,
    days: Long,
    onSend: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(workOrder.customerName, fontWeight = FontWeight.SemiBold)
                Text("${workOrder.number} - $days dia(s) aguardando retirada", style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onSend) {
                Text("Enviar MSG")
            }
        }
    }
}

internal fun pickupDays(workOrder: WorkOrderSummary, now: Long): Long {
    val base = workOrder.concludedAt ?: workOrder.openedAt
    return TimeUnit.MILLISECONDS.toDays((now - base).coerceAtLeast(0))
}

internal fun renderPickupMessage(
    template: String,
    workOrder: WorkOrderSummary,
    days: Long,
    companyName: String,
): String =
    MessageTemplateRenderer.render(
        template = template,
        tokens = mapOf(
            "nome" to workOrder.customerName,
            "telefone" to workOrder.customerPhone,
            "cpf" to "",
            "os" to workOrder.number,
            "orcamento" to "",
            "valor" to Formatters.currency(workOrder.totalValue),
            "status" to workOrder.status,
            "empresa" to companyName,
            "data" to Formatters.dateTimeShort(System.currentTimeMillis()),
            "dias" to days.toString(),
        ),
    )

@Composable
internal fun WorkOrderListRow(
    workOrder: WorkOrderSummary,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(workOrder.customerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                text = "${workOrder.number} - ${workOrder.status}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${Formatters.currency(workOrder.totalValue)} - ${Formatters.dateTimeShort(workOrder.openedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WorkOrderRow(
    workOrder: WorkOrderSummary,
    onStatusSelected: (WorkOrderStatus) -> Unit,
    onShowDocument: () -> Unit,
    onShowMessage: () -> Unit,
    onShowHistory: () -> Unit,
    onEdit: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(workOrder.number, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(workOrder.customerName, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "${workOrder.status} - ${workOrder.itemCount} item(ns) - ${Formatters.currency(workOrder.totalValue)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = Formatters.dateTimeShort(workOrder.openedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                if (workOrder.status != WorkOrderStatus.Completed.label && workOrder.status != WorkOrderStatus.Canceled.label) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Text("Editar")
                    }
                }
                TextButton(onClick = onShowDocument) {
                    Icon(Icons.Filled.Description, contentDescription = null)
                    Text("Documento")
                }
                TextButton(onClick = onShowMessage) {
                    Icon(Icons.Filled.Message, contentDescription = null)
                    Text("Mensagem")
                }
                TextButton(onClick = onShowHistory) {
                    Icon(Icons.Filled.History, contentDescription = null)
                    Text("Historico")
                }
                WorkOrderStatus.entries.forEach { status ->
                    TextButton(onClick = { onStatusSelected(status) }) {
                        Text(status.label)
                    }
                }
            }
        }
    }
}
