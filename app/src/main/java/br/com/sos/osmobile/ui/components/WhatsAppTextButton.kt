package br.com.sos.osmobile.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun WhatsAppTextButton(
    phone: String,
    text: String,
) {
    val context = LocalContext.current
    Button(
        onClick = {
            val digits = phone.filter { it.isDigit() }
            val uri = Uri.parse("https://wa.me/55$digits?text=${Uri.encode(text)}")
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        },
    ) {
        Text("Abrir WhatsApp")
    }
}
