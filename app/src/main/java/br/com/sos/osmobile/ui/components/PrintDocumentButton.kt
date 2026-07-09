package br.com.sos.osmobile.ui.components

import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.FileOutputStream

@Composable
fun PrintDocumentButton(
    label: String,
    jobName: String,
    text: String,
) {
    val context = LocalContext.current
    Button(
        onClick = {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print(
                jobName,
                TextPdfPrintAdapter(jobName = "$jobName.pdf", text = text),
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                    .build(),
            )
        },
    ) {
        Text(label)
    }
}

private class TextPdfPrintAdapter(
    private val jobName: String,
    private val text: String,
) : PrintDocumentAdapter() {
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes?,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: android.os.Bundle?,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }
        callback.onLayoutFinished(
            PrintDocumentInfo.Builder(jobName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build(),
            true,
        )
    }

    override fun onWrite(
        pages: Array<out PageRange>?,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback,
    ) {
        if (cancellationSignal?.isCanceled == true) {
            callback.onWriteCancelled()
            return
        }
        runCatching {
            FileOutputStream(destination.fileDescriptor).use { output ->
                writePdf(output, text)
            }
        }.fold(
            onSuccess = { callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES)) },
            onFailure = { callback.onWriteFailed(it.message) },
        )
    }
}
