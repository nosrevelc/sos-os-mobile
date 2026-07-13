package br.com.sos.osmobile.ui.components

import android.content.Context
import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

fun openWhatsApp(context: Context, phone: String, text: String) {
    val digits = phone.filter { it.isDigit() }
    val uri = Uri.parse("https://wa.me/55$digits?text=${Uri.encode(text)}")
    startActivityOrWarn(context, Intent(Intent.ACTION_VIEW, uri))
}

fun openSms(context: Context, phone: String, text: String) {
    val uri = Uri.parse("smsto:${phone.filter { it.isDigit() }}")
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra("sms_body", text)
    }
    startActivityOrWarn(context, intent)
}

fun openEmail(context: Context, email: String?, subject: String, text: String) {
    val uri = if (email.isNullOrBlank()) {
        Uri.parse("mailto:")
    } else {
        Uri.parse("mailto:${Uri.encode(email)}")
    }
    val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivityOrWarn(context, intent)
}

private fun startActivityOrWarn(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "Nenhum aplicativo disponivel para esta acao.", Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MessageActionButtons(
    phone: String,
    text: String,
    email: String? = null,
    subject: String = "OS Mobile",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = { openWhatsApp(context, phone, text) }) {
            Text("WhatsApp")
        }
        OutlinedButton(onClick = { openSms(context, phone, text) }) {
            Text("SMS")
        }
        OutlinedButton(onClick = { openEmail(context, email, subject, text) }) {
            Text("Email")
        }
    }
}
