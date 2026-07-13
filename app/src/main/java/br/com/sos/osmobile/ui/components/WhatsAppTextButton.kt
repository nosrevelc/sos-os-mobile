package br.com.sos.osmobile.ui.components

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
            openWhatsApp(context, phone, text)
        },
    ) {
        Text("Abrir WhatsApp")
    }
}
