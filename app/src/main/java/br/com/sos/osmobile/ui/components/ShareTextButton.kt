package br.com.sos.osmobile.ui.components

import android.content.Intent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ShareTextButton(
    label: String,
    text: String,
) {
    val context = LocalContext.current
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, label))
        },
    ) {
        Text(label)
    }
}
