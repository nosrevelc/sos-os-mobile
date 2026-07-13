package br.com.sos.osmobile.ui.components

import android.graphics.Bitmap
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

import java.io.File

@Composable
fun PixQrCode(payload: String, modifier: Modifier = Modifier) {
    if (payload.isBlank()) return
    val bitmap = remember(payload) { createPixQrBitmap(payload, 256) }
    Box(modifier = modifier.background(Color.White)) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "QR Code PIX",
            modifier = Modifier.size(220.dp),
        )
    }
}

fun createPixQrBitmap(payload: String, size: Int): Bitmap =
    QrCodeBitmap.generate(payload, size)

fun sharePixQrJpeg(context: Context, payload: String, text: String, whatsappOnly: Boolean = false) {
    if (payload.isBlank()) return
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(dir, "pix-qr.jpg")
    file.outputStream().use { output ->
        createPixQrBitmap(payload, 640).compress(Bitmap.CompressFormat.JPEG, 100, output)
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (whatsappOnly) setPackage("com.whatsapp")
    }
    runCatching { context.startActivity(intent) }
        .onFailure { context.startActivity(Intent.createChooser(intent.apply { setPackage(null) }, "Enviar QR PIX")) }
}

private object QrCodeBitmap {
    fun generate(content: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 1),
        )
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
        }
    }
}
