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
internal fun SavedSignaturePreview(signatureUri: Uri) {
    val context = LocalContext.current
    val signatureImage = remember(signatureUri) {
        runCatching {
            context.contentResolver.openInputStream(signatureUri)?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (signatureImage != null) {
            Image(
                bitmap = signatureImage,
                contentDescription = "Assinatura coletada",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = "Assinatura salva nao encontrada.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun SignatureCapture(
    signerName: String,
    onSignerNameChanged: (String) -> Unit,
    onSave: (String, Bitmap) -> Unit,
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var padSize by remember { mutableStateOf(IntSize.Zero) }
    var expanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = signerName,
        onValueChange = onSignerNameChanged,
        label = { Text("Nome do assinante") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
    ) {
        SignaturePad(
            strokes = strokes,
            currentStroke = currentStroke,
            onCurrentStrokeChanged = { currentStroke = it },
            onStrokeFinished = { strokes += it },
            onSizeChanged = { padSize = it },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(Icons.Filled.OpenInFull, contentDescription = "Expandir assinatura")
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = {
                strokes.clear()
                currentStroke = emptyList()
            },
            modifier = Modifier.weight(1f),
        ) {
            Text("Limpar")
        }
        Button(
            onClick = {
                val allStrokes = strokes.toList() + listOf(currentStroke).filter { it.size > 1 }
                if (padSize.width > 0 && padSize.height > 0 && allStrokes.isNotEmpty()) {
                    onSave(signerName, createSignatureBitmap(allStrokes, padSize.width, padSize.height))
                }
            },
            modifier = Modifier.weight(1f),
            enabled = strokes.isNotEmpty() || currentStroke.isNotEmpty(),
        ) {
            Text("Salvar assinatura")
        }
    }

    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.98f)
                    .fillMaxHeight(0.92f),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                ) {
                    Text("Assinatura", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = signerName,
                        onValueChange = onSignerNameChanged,
                        label = { Text("Nome do assinante") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SignaturePad(
                        strokes = strokes,
                        currentStroke = currentStroke,
                        onCurrentStrokeChanged = { currentStroke = it },
                        onStrokeFinished = { strokes += it },
                        onSizeChanged = { padSize = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                strokes.clear()
                                currentStroke = emptyList()
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Limpar")
                        }
                        OutlinedButton(onClick = { expanded = false }, modifier = Modifier.weight(1f)) {
                            Text("Fechar")
                        }
                        Button(
                            onClick = {
                                val allStrokes = strokes.toList() + listOf(currentStroke).filter { it.size > 1 }
                                if (padSize.width > 0 && padSize.height > 0 && allStrokes.isNotEmpty()) {
                                    onSave(signerName, createSignatureBitmap(allStrokes, padSize.width, padSize.height))
                                    expanded = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = strokes.isNotEmpty() || currentStroke.isNotEmpty(),
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SignaturePad(
    strokes: List<List<Offset>>,
    currentStroke: List<Offset>,
    onCurrentStrokeChanged: (List<Offset>) -> Unit,
    onStrokeFinished: (List<Offset>) -> Unit,
    onSizeChanged: (IntSize) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .background(Color.White)
            .onSizeChanged(onSizeChanged)
            .pointerInput(Unit) {
                val inProgressStroke = mutableListOf<Offset>()
                detectDragGestures(
                    onDragStart = {
                        inProgressStroke.clear()
                        inProgressStroke += it
                        onCurrentStrokeChanged(inProgressStroke.toList())
                    },
                    onDrag = { change, _ ->
                        inProgressStroke += change.position
                        onCurrentStrokeChanged(inProgressStroke.toList())
                    },
                    onDragEnd = {
                        if (inProgressStroke.size > 1) onStrokeFinished(inProgressStroke.toList())
                        inProgressStroke.clear()
                        onCurrentStrokeChanged(emptyList())
                    },
                    onDragCancel = {
                        inProgressStroke.clear()
                        onCurrentStrokeChanged(emptyList())
                    },
                )
            },
    ) {
        (strokes + listOf(currentStroke)).forEach { stroke ->
            stroke.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = Color.Black,
                    start = start,
                    end = end,
                    strokeWidth = 4f,
                    cap = StrokeCap.Round,
                )
            }
        }
        drawRect(color = Color.LightGray, style = Stroke(width = 1f))
    }
}

internal fun createSignatureBitmap(strokes: List<List<Offset>>, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(AndroidColor.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.BLACK
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.STROKE
    }
    strokes.forEach { stroke ->
        stroke.zipWithNext().forEach { (start, end) ->
            canvas.drawLine(start.x, start.y, end.x, end.y, paint)
        }
    }
    return bitmap
}

internal data class ClientMessage(
    val phone: String,
    val email: String?,
    val subject: String,
    val text: String,
)

