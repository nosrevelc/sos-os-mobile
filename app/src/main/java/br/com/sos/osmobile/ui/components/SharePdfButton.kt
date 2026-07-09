package br.com.sos.osmobile.ui.components

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import java.io.OutputStream

@Composable
fun SharePdfButton(
    label: String,
    fileName: String,
    text: String,
) {
    val context = LocalContext.current
    Button(
        onClick = {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, fileName)
            file.outputStream().use { writePdf(it, text) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, label))
        },
    ) {
        Text(label)
    }
}

internal fun writePdf(outputStream: OutputStream, text: String) {
    val document = PdfDocument()
    val paint = Paint().apply {
        textSize = 12f
        isAntiAlias = true
    }
    val pageWidth = 595
    val pageHeight = 842
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    var y = 40f

    text.lineSequence().forEach { line ->
        if (y > pageHeight - 40) {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            y = 40f
        }
        page.canvas.drawText(line.take(95), 40f, y, paint)
        y += 18f
    }

    document.finishPage(page)
    document.writeTo(outputStream)
    document.close()
}
