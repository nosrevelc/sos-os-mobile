package br.com.sos.osmobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

fun driveStatusText(status: String, error: String): String =
    when {
        status.isBlank() -> "Nao iniciado"
        error.isNotBlank() && status != "Sincronizado" -> "$status - $error"
        else -> status
    }

fun driveStatusIcon(status: String): ImageVector =
    when (status) {
        "Sincronizado" -> Icons.Filled.CheckCircle
        "Erro" -> Icons.Filled.Warning
        else -> Icons.Filled.CloudUpload
    }

@Composable
fun driveStatusColor(status: String): Color =
    when (status) {
        "Sincronizado" -> MaterialTheme.colorScheme.primary
        "Erro" -> MaterialTheme.colorScheme.error
        "Pendente", "Sem configuracao" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
fun DriveSyncIndicator(
    status: String,
    error: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DriveSyncStatusIcon(status = status, error = error)
        DriveSyncStatusText(status = status, error = error)
    }
}

@Composable
fun DriveSyncStatusIcon(
    status: String,
    error: String,
) {
    Icon(
        imageVector = driveStatusIcon(status),
        contentDescription = driveStatusText(status, error),
        tint = driveStatusColor(status),
    )
}

@Composable
fun DriveSyncStatusText(
    status: String,
    error: String,
) {
    Text(
        text = "Drive: ${driveStatusText(status, error)}",
        style = MaterialTheme.typography.bodySmall,
        color = driveStatusColor(status),
    )
}
