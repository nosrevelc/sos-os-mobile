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
internal fun PaymentMethodSelector(
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val methods = listOf("PIX", "Dinheiro", "Debito", "Credito", "Transferencia", "Outro")
    OutlinedButton(onClick = { expanded = true }, modifier = modifier.fillMaxWidth()) {
        Icon(paymentMethodIcon(selected), contentDescription = null)
        Spacer(Modifier.width(6.dp))
        Text("Forma: $selected")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        methods.forEach { method ->
            DropdownMenuItem(
                leadingIcon = { Icon(paymentMethodIcon(method), contentDescription = null) },
                text = { Text(if (method == selected) "${paymentMethodLabel(method)} *" else paymentMethodLabel(method)) },
                onClick = {
                    onSelected(method)
                    expanded = false
                },
            )
        }
    }
}

@Composable
internal fun DeliveryOptionSelector(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }, modifier = modifier.fillMaxWidth()) {
        Text("$label: $selected")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(if (option == selected) "$option *" else option) },
                onClick = {
                    onSelected(option)
                    expanded = false
                },
            )
        }
    }
}

internal fun paymentMethodLabel(method: String): String =
    if (method == "PIX") "PIX" else method

internal fun paymentMethodIcon(method: String): ImageVector =
    when (method) {
        "PIX" -> Icons.Filled.Payment
        "Dinheiro" -> Icons.Filled.AttachMoney
        "Debito", "Credito" -> Icons.Filled.CreditCard
        "Transferencia" -> Icons.Filled.AccountBalance
        else -> Icons.Filled.MoreHoriz
    }
