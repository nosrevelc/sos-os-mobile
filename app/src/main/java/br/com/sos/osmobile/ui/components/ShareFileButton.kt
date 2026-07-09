package br.com.sos.osmobile.ui.components

import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ShareFileButton(
    label: String,
    fileName: String,
    text: String,
    mimeType: String = "text/plain",
) {
    val context = LocalContext.current
    Button(
        onClick = {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(dir, fileName).apply { writeText(text) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, label))
        },
    ) {
        Text(label)
    }
}
